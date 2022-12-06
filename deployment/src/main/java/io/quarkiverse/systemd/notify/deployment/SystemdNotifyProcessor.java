package io.quarkiverse.systemd.notify.deployment;

import io.quarkiverse.systemd.notify.runtime.SystemdNotifierRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.logging.Log;
import io.quarkus.runtime.shutdown.ShutdownListener;

class SystemdNotifyProcessor {
    private static final String FEATURE = "systemd-notify";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Consume(ServiceStartBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void serviceStarted(SystemdNotifierRecorder recorder) {
        boolean available = recorder.checkSystemdAvailability();
        if (available) {
            recorder.sdNotify("READY=1");
        } else {
            Log.warn("systemd is not available");
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ShutdownListenerBuildItem shutdownListener(SystemdNotifierRecorder recorder) {
        boolean available = recorder.checkSystemdAvailability();
        if (!available)
            return null;

        return new ShutdownListenerBuildItem(new ShutdownListener() {
            @Override
            public void preShutdown(ShutdownNotification notification) {
                recorder.sdNotify("STOPPING=1");
                notification.done();
            }

            @Override
            public void shutdown(ShutdownNotification notification) {
                notification.done();
            }
        });
    }
}