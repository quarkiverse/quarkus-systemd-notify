package io.quarkiverse.systemd.notify.deployment;

import io.quarkiverse.systemd.notify.runtime.SystemdNotifyRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

class SystemdNotifyProcessor {
    private static final String FEATURE = "systemd-notify";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Notifies systemd once the application started : after the {@code StartupEvent} observers (which run once the
     * services started). The shutdown tasks run in the reverse order : it notifies systemd that the application stops
     * before the {@code ShutdownEvent} observers.
     */
    @BuildStep
    @Consume(ApplicationStartBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void onQuarkusStarted(SystemdNotifyRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem) {
        recorder.onQuarkusStarted(shutdownContextBuildItem);
    }
}