package io.quarkiverse.systemd.notify.test;

import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.systemd.notify.runtime.SystemdNotifier;
import io.quarkus.test.QuarkusExtensionTest;

public class SystemdNotifyTest {

    private static final Set<String> STARTUP_MESSAGES = Set.of("Quarkus was not started as systemd service",
            "Notified systemd that this service started successfully");

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .setLogRecordPredicate(record -> SystemdNotifier.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> Assertions.assertTrue(
                    records.stream().anyMatch(record -> STARTUP_MESSAGES.contains(record.getMessage())),
                    "The service notifying systemd did not run when the application started"));

    @Test
    public void writeYourOwnUnitTest() {
        // Write your unit tests here - see the testing extension guide https://quarkus.io/guides/writing-extensions#testing-extensions for more information
        Assertions.assertTrue(true, "Add some assertions to " + getClass().getName());
    }
}
