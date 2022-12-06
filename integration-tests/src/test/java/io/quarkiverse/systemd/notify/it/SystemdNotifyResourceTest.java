package io.quarkiverse.systemd.notify.it;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class SystemdNotifyResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/systemd-notify")
                .then()
                .statusCode(200)
                .body(is("Hello systemd-notify"));
    }
}
