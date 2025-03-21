package com.johnmanko.portfolio.alibabassecret.e2e;

import com.johnmanko.portfolio.alibabassecret.models.AppConfigModel;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/",
        "app.config.client.auth.auth0.domain=test.us.auth0.com",
        "app.config.client.auth.auth0.client-id=ASDF1234",
        "app.config.server.auth.auth0.custom-jwt-namespace=custom.jwt.namespace/roles"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Testing '/public/*' API")
public class PublicEndpointsE2ETest {


    @Container
    @ServiceConnection(type = RedisConnectionDetails.class)
    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:6.2.6"));

    @BeforeAll
    void setUp()  {
        assertThat(redisContainer.isCreated()).isTrue();
        assertThat(redisContainer.isRunning()).isTrue();
    }

    @Test
    @Order(1)
    @DisplayName("GET /public/config.json")
    void getConfigJSON(@Autowired WebTestClient webClient) throws Exception {

        webClient.get()
                .uri("/public/config.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AppConfigModel.class)
                .consumeWith(response -> {
                    AppConfigModel config = Objects.requireNonNull(response.getResponseBody());
                    assertNotNull(config);
                    assertEquals("ASDF1234", config.authAuth0ClientId());
                    assertEquals("test.us.auth0.com", config.authAuth0Domain());
                });


    }
}
