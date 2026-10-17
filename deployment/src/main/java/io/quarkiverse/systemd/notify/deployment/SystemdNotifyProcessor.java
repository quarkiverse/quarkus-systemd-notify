package io.quarkiverse.systemd.notify.deployment;

import io.quarkiverse.systemd.notify.runtime.SystemdNotifier;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class SystemdNotifyProcessor {
    private static final String FEATURE = "systemd-notify";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Notifies systemd once the application started : after the {@code StartupEvent} observers, which run after the
     * services started by the other extensions. It then stops before the {@code ShutdownEvent} observers.
     */
    @BuildStep
    void onQuarkusStarted(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar
                .forService("io.quarkiverse.systemd-notify.notify")
                .atPhase(Phase.SERVING)
                .after("io.quarkus.arc.startup-event")
                .onStart(ctx -> SystemdNotifier.notifyStarted(ctx));
    }
}
