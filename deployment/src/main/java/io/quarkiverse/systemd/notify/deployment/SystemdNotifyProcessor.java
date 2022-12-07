package io.quarkiverse.systemd.notify.deployment;

import io.quarkiverse.systemd.notify.runtime.SystemdNotifyRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;

class SystemdNotifyProcessor {
    private static final String FEATURE = "systemd-notify";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Consume(ServiceStartBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void onQuarkusStarted(SystemdNotifyRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem) {
        recorder.onQuarkusStarted(shutdownContextBuildItem);
    }
}