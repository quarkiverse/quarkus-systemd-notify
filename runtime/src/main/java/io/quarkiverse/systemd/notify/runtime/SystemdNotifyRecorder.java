package io.quarkiverse.systemd.notify.runtime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SystemdNotifyRecorder {

    private static final Logger LOGGER = Logger.getLogger(SystemdNotifyRecorder.class.getName());

    public void onQuarkusStarted(ShutdownContext shutdownContext) {
        boolean systemdAvailable = System.getenv("NOTIFY_SOCKET") != null;
        if (systemdAvailable) {
            shutdownContext.addShutdownTask(() -> sdNotify("STOPPING=1"));
            sdNotify("READY=1");
        } else {
            LOGGER.warning("systemd is not available");
        }
    }

    private static void sdNotify(String state) {
        try {
            var process = new ProcessBuilder("systemd-notify", state).redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.warning(
                        String.format("systemd-notify returned non-zero exit code (%d) for state (%s)", exitCode, state));
                try (var r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    r.lines().forEach(l -> LOGGER.warning(String.format("systemd-notify output: %s", l)));
                }
            } else {
                LOGGER.info(String.format("Called systemd-notify with state (%s)", state));
            }
        } catch (Exception e) {
            Log.error("Failed to call systemd-notify", e);
        }
    }
}