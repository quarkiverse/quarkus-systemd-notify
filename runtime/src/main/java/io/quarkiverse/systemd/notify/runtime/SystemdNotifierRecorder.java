package io.quarkiverse.systemd.notify.runtime;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SystemdNotifierRecorder {
    public boolean checkSystemdAvailability() {
        return System.getenv("NOTIFY_SOCKET") != null;
    }

    public void sdNotify(String state) {
        try {
            var process = new ProcessBuilder("systemd-notify", state).redirectErrorStream(true).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Log.warn(String.format("systemd-notify returned non-zero exit code: %d", exitCode));
                try (var r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    r.lines().forEach(l -> Log.warn(String.format("systemd-notify output: %s", l)));
                }
            }
        } catch (Exception e) {
            Log.error("Failed to call systemd-notify", e);
        }
    }
}