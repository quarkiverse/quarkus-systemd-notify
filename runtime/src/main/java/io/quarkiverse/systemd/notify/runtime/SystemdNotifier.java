package io.quarkiverse.systemd.notify.runtime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.quarkus.core.StartContext;

/**
 * Notifies systemd of the lifecycle of the application, when it runs as a systemd service.
 */
public final class SystemdNotifier {

    private static final Logger LOGGER = Logger.getLogger(SystemdNotifier.class.getName());

    private SystemdNotifier() {
    }

    /**
     * Notifies systemd that the application started, and registers the notification that it stops.
     *
     * @param context the start context of the service
     */
    public static void notifyStarted(StartContext context) {
        boolean startedAsSystemdService = System.getenv("NOTIFY_SOCKET") != null;
        if (startedAsSystemdService) {
            context.onStop(() -> {
                sdNotify("STOPPING=1");
                LOGGER.info("Notified systemd that this service is about to shutdown");
            });
            sdNotify("READY=1");
            LOGGER.info("Notified systemd that this service started successfully");
        } else {
            LOGGER.info("Quarkus was not started as systemd service");
        }
    }

    private static void sdNotify(String state) {
        try {
            // systemd-notify waits until systemd processed the notification (systemd 246 and later)
            var process = new ProcessBuilder("systemd-notify", state).redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning(
                        String.format("systemd-notify returned non-zero exit code (%d) for state (%s)", exitCode, state));
                try (var r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    r.lines().forEach(l -> LOGGER.warning(String.format("systemd-notify output: %s", l)));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to call systemd-notify", e);
        }
    }
}
