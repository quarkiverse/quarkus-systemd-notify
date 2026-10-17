package io.quarkiverse.systemd.notify.it;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * Slows down the startup and the shutdown of the application, so that the tests see when systemd is notified : the
 * service must still be activating while the {@code StartupEvent} observers run, and already deactivating while the
 * {@code ShutdownEvent} observers run.
 */
@ApplicationScoped
public class LifecycleDelays {

    @ConfigProperty(name = "it.startup-delay", defaultValue = "0s")
    Duration startupDelay;

    @ConfigProperty(name = "it.shutdown-delay", defaultValue = "0s")
    Duration shutdownDelay;

    void onStart(@Observes StartupEvent event) throws InterruptedException {
        Thread.sleep(startupDelay.toMillis());
    }

    void onStop(@Observes ShutdownEvent event) throws InterruptedException {
        Thread.sleep(shutdownDelay.toMillis());
    }
}
