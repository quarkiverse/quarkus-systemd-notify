package io.quarkiverse.systemd.notify.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Runs the application as a systemd service of type notify and checks what systemd sees : the service is active once
 * the application started (READY=1, after the {@code StartupEvent} observers), and deactivating as soon as the
 * application begins to stop (STOPPING=1, before the {@code ShutdownEvent} observers). In JVM mode, and as a native
 * executable with -Dnative. Linux booted with systemd only, as root or with passwordless sudo.
 */
@EnabledOnOs(OS.LINUX)
class SystemdNotifyIT {

    /**
     * The unit settings that let the notifications through : the extension sends them with systemd-notify, a child
     * process of the application, which the default NotifyAccess=main accepts only when systemd-notify can send them
     * on behalf of the application.
     */
    enum Setup {
        /**
         * A service run as root.
         */
        ROOT,
        /**
         * An unprivileged user, with CAP_SYS_ADMIN, as in the documentation.
         */
        CAP_SYS_ADMIN("DynamicUser=yes", "AmbientCapabilities=CAP_SYS_ADMIN"),
        /**
         * An unprivileged user, and the notifications of every process of the service accepted.
         */
        NOTIFY_ACCESS_ALL("DynamicUser=yes", "NotifyAccess=all");

        final List<String> properties;

        Setup(String... properties) {
            this.properties = List.of(properties);
        }
    }

    @BeforeAll
    static void systemd() {
        assumeTrue(TransientService.available(), "needs Linux booted with systemd, and root or passwordless sudo");
    }

    @ParameterizedTest
    @EnumSource(Setup.class)
    void notifiesStartupAndShutdown(Setup setup) {
        try (TransientService service = TransientService.start(Duration.ofSeconds(60), setup.properties)) {
            // systemd-run returns once the service started : once systemd received READY=1, which comes after the
            // StartupEvent observers (LifecycleDelays)
            assertEquals(0, service.start().exitCode(), () -> service.start().output() + service.journal());
            assertTrue(service.startDuration().compareTo(TransientService.STARTUP_DELAY) >= 0,
                    () -> "READY=1 before the StartupEvent observers ended, after " + service.startDuration() + "\n"
                            + service.journal());
            assertEquals("active/running", service.state(), service::journal);

            // The application stops on its own (SIGTERM, as systemctl stop sends, but no stop job) : once systemd
            // received STOPPING=1, which comes before the ShutdownEvent observers (LifecycleDelays), the service is
            // deactivating while its main process still runs
            service.kill("SIGTERM");
            assertTrue(service.await(state -> "deactivating".equals(state.get("ActiveState"))
                    && !"0".equals(state.get("MainPID")), TransientService.SHUTDOWN_DELAY.dividedBy(2)),
                    () -> service.state() + "\n" + service.journal());
            assertTrue(service.await(state -> "inactive".equals(state.get("ActiveState")), Duration.ofSeconds(30)),
                    service::state);

            String journal = service.awaitJournal("Deactivated successfully", Duration.ofSeconds(10));
            assertTrue(journal.contains("Deactivated successfully"), journal);
            assertTrue(journal.contains("Notified systemd that this service started successfully"), journal);
            assertTrue(journal.contains("Notified systemd that this service is about to shutdown"), journal);
            assertFalse(journal.contains("reception only permitted"), journal);
            // systemd logs the invalid messages without the service that sent them
            String manager = service.managerJournal();
            assertFalse(manager.contains("BARRIER=1"), manager);
        }
    }

    /**
     * An unprivileged user without CAP_SYS_ADMIN nor NotifyAccess=all : systemd-notify sends the notifications under
     * its own PID, the default NotifyAccess=main rejects them, and the service fails to start. systemd logs the
     * rejection ("but reception only permitted for main PID") at the debug level since systemd 256 : not in the journal
     * by default, so the test checks that the application sent READY=1 and that systemd still timed out.
     */
    @Test
    void notificationsRejectedWithoutCapSysAdminNorNotifyAccessAll() {
        try (TransientService service = TransientService.start(Duration.ofSeconds(10), List.of("DynamicUser=yes"))) {
            assertNotEquals(0, service.start().exitCode(), service::journal);
            assertEquals("timeout", service.show("Result").get("Result"), service::journal);
            String journal = service.journal();
            assertTrue(journal.contains("Notified systemd that this service started successfully"), journal);
        }
    }
}
