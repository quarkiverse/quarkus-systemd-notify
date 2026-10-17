package io.quarkiverse.systemd.notify.it;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * The application of this module run as a transient systemd service of type notify (systemd-run) : its jar in JVM mode,
 * its native executable with -Dnative. It is a system service : the systemd commands run as root, directly or through
 * {@code sudo -n} (passwordless sudo, as on the GitHub runners).
 */
final class TransientService implements AutoCloseable {

    /**
     * How long the {@code StartupEvent} observers of the application take (see {@code LifecycleDelays}).
     */
    static final Duration STARTUP_DELAY = Duration.ofSeconds(2);

    /**
     * How long the {@code ShutdownEvent} observers of the application take (see {@code LifecycleDelays}).
     */
    static final Duration SHUTDOWN_DELAY = Duration.ofSeconds(3);

    /**
     * Where the application is copied for its run : any user can read it (DynamicUser=yes), unlike a build directory in
     * a home directory, and it is not in /tmp, which a service with a dynamic user does not share.
     */
    private static final Path RUN_DIRECTORY = Path.of("/run/quarkus-systemd-notify-it");

    private static final boolean ROOT = "root".equals(System.getProperty("user.name"));

    record Command(int exitCode, String output) {
    }

    private final String unit;
    private final Path directory;
    private final String cursor;
    private final Command start;
    private final Duration startDuration;

    private TransientService(String unit, Path directory, String cursor, Command start, Duration startDuration) {
        this.unit = unit;
        this.directory = directory;
        this.cursor = cursor;
        this.start = start;
        this.startDuration = startDuration;
    }

    /**
     * Whether the services can run here : on Linux booted with systemd, as root or with passwordless sudo.
     */
    static boolean available() {
        if (!Files.isDirectory(Path.of("/run/systemd/system"))) {
            return false;
        }
        try {
            return ROOT || exec(List.of("sudo", "-n", "true"), Duration.ofSeconds(10)).exitCode() == 0;
        } catch (UncheckedIOException e) {
            // no sudo
            return false;
        }
    }

    /**
     * Starts the application as a service of type notify with these unit properties, and waits until it started (until
     * systemd received READY=1) or failed to start.
     */
    static TransientService start(Duration startTimeout, List<String> properties) {
        String unit = "quarkus-systemd-notify-it-" + UUID.randomUUID().toString().substring(0, 8);
        Path directory = RUN_DIRECTORY.resolve(unit);
        List<String> command = stage(directory);
        String cursor = cursor();
        List<String> systemdRun = new ArrayList<>(List.of("systemd-run", "--unit=" + unit, "--quiet",
                "--property=Type=notify",
                "--property=TimeoutStartSec=" + startTimeout.toSeconds(),
                // the exit status of the JVM stopped by SIGTERM, as in the unit of the documentation
                "--property=SuccessExitStatus=143",
                "--setenv=QUARKUS_HTTP_PORT=0",
                "--setenv=IT_STARTUP_DELAY=" + STARTUP_DELAY.toSeconds() + "s",
                "--setenv=IT_SHUTDOWN_DELAY=" + SHUTDOWN_DELAY.toSeconds() + "s"));
        properties.forEach(property -> systemdRun.add("--property=" + property));
        systemdRun.add("--");
        systemdRun.addAll(command);
        Instant before = Instant.now();
        Command start = asRoot(systemdRun, startTimeout.plusSeconds(30));
        return new TransientService(unit, directory, cursor, start, Duration.between(before, Instant.now()));
    }

    /**
     * Copies the application to the directory, and returns the command line that runs it.
     */
    private static List<String> stage(Path directory) {
        checked(asRoot(List.of("install", "-d", "-m", "0755", directory.toString())));
        if (Boolean.getBoolean("quarkus.native.enabled")) {
            Path runner = Path.of(System.getProperty("native.image.path").strip());
            Path staged = directory.resolve(runner.getFileName());
            checked(asRoot(List.of("install", "-m", "0755", runner.toString(), staged.toString())));
            return List.of(staged.toString());
        }
        Path app = Path.of(System.getProperty("quarkus-app.path").strip());
        checked(asRoot(List.of("cp", "-r", app.toString(), directory.toString())));
        checked(asRoot(List.of("chmod", "-R", "a+rX", directory.toString())));
        return List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-jar",
                directory.resolve(app.getFileName()).resolve("quarkus-run.jar").toString());
    }

    /**
     * The result of systemd-run : 0 once the service started.
     */
    Command start() {
        return start;
    }

    /**
     * How long systemd-run took to return : until systemd received READY=1, or the start failed.
     */
    Duration startDuration() {
        return startDuration;
    }

    /**
     * The ActiveState and SubState of the service, such as active/running.
     */
    String state() {
        Map<String, String> state = show("ActiveState", "SubState");
        return state.get("ActiveState") + "/" + state.get("SubState");
    }

    /**
     * These properties of the service (systemctl show).
     */
    Map<String, String> show(String... properties) {
        List<String> command = new ArrayList<>(List.of("systemctl", "show", unit));
        for (String property : properties) {
            command.add("--property=" + property);
        }
        Map<String, String> values = new HashMap<>();
        checked(asRoot(command)).output().lines().forEach(line -> {
            int equals = line.indexOf('=');
            if (equals > 0) {
                values.put(line.substring(0, equals), line.substring(equals + 1));
            }
        });
        return values;
    }

    /**
     * Waits until the ActiveState, SubState and MainPID of the service match the condition, and tells whether they did.
     */
    boolean await(Predicate<Map<String, String>> condition, Duration timeout) {
        Instant end = Instant.now().plus(timeout);
        do {
            if (condition.test(show("ActiveState", "SubState", "MainPID"))) {
                return true;
            }
            sleep(Duration.ofMillis(20));
        } while (Instant.now().isBefore(end));
        return false;
    }

    /**
     * Sends the signal to the processes of the service, as systemctl stop does, but without a stop job : the service
     * stops on its own.
     */
    void kill(String signal) {
        checked(asRoot(List.of("systemctl", "kill", "--signal=" + signal, unit)));
    }

    /**
     * The journal of the service since it started : its output, and what systemd logged about it.
     */
    String journal() {
        return checked(asRoot(List.of("journalctl", "--unit=" + unit, "--after-cursor=" + cursor, "--no-pager",
                "--output=cat"))).output();
    }

    /**
     * Waits until the journal of the service contains the text, and returns it.
     */
    String awaitJournal(String text, Duration timeout) {
        Instant end = Instant.now().plus(timeout);
        String journal = journal();
        while (!journal.contains(text) && Instant.now().isBefore(end)) {
            sleep(Duration.ofMillis(100));
            journal = journal();
        }
        return journal;
    }

    /**
     * What systemd itself (PID 1) logged since the service started, including the messages that it rejected without
     * knowing the service that sent them.
     */
    String managerJournal() {
        return checked(asRoot(List.of("journalctl", "_PID=1", "--after-cursor=" + cursor, "--no-pager",
                "--output=cat"))).output();
    }

    @Override
    public void close() {
        asRoot(List.of("systemctl", "stop", unit));
        asRoot(List.of("systemctl", "reset-failed", unit));
        asRoot(List.of("rm", "-rf", directory.toString()));
    }

    /**
     * The cursor of the last entry of the journal.
     */
    private static String cursor() {
        String output = checked(asRoot(List.of("journalctl", "--lines=1", "--show-cursor", "--no-pager",
                "--output=cat"))).output();
        return output.lines().filter(line -> line.startsWith("-- cursor: ")).map(line -> line.substring(11))
                .reduce((first, last) -> last)
                .orElseThrow(() -> new IllegalStateException("no journal cursor in: " + output));
    }

    private static Command asRoot(List<String> command) {
        return asRoot(command, Duration.ofSeconds(60));
    }

    private static Command asRoot(List<String> command, Duration timeout) {
        if (ROOT) {
            return exec(command, timeout);
        }
        List<String> sudo = new ArrayList<>(List.of("sudo", "-n"));
        sudo.addAll(command);
        return exec(sudo, timeout);
    }

    private static Command checked(Command command) {
        if (command.exitCode() != 0) {
            throw new IllegalStateException("exit code " + command.exitCode() + ": " + command.output());
        }
        return command;
    }

    private static Command exec(List<String> command, Duration timeout) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            // read while the command runs : systemd-run waits until the service started
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(process.getInputStream().readAllBytes(), UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("timed out after " + timeout + ": " + command);
            }
            return new Command(process.exitValue(), output.join());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
