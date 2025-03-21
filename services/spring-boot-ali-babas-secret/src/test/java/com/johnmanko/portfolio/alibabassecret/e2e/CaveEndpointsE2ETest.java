package com.johnmanko.portfolio.alibabassecret.e2e;

import com.johnmanko.portfolio.alibabassecret.models.TreasureModel;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/",
        "app.config.client.auth.auth0.domain=test.us.auth0.com",
        "app.config.client.auth.auth0.client-id=ASDF1234",
        "app.config.server.auth.auth0.custom-jwt-namespace=custom.jwt.namespace/roles"
})
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E Testing '/api/cave/*' API")
public class CaveEndpointsE2ETest {
    static final String THIEVES_TREASURE = "thieves-treasure";
    static final String ALIBABA_TREASURE = "alibaba-treasure";
    static final String TAKE_TREASURE = "take-treasure";
    static final String AUTHORITIES = "authorities";
    static final int TAKE_AMOUNT = 20;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt = jwt().jwt(jwt -> {
        jwt.subject("test-user");
    }).authorities(
            new SimpleGrantedAuthority("SCOPE_see:thieves-treasure"),
            new SimpleGrantedAuthority("SCOPE_see:alibaba-treasure"),
            new SimpleGrantedAuthority("SCOPE_take:thieves-treasure"),
            new SimpleGrantedAuthority("ROLE_treasure-hunter")
    );

    @Container
    @ServiceConnection(type = RedisConnectionDetails.class)
    static RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:6.2.6"));

    /**
     * There is a problem with using WebTestClient and mocking JWT
     * https://github.com/spring-projects/spring-security/issues/9304#issuecomment-841495717
     */
    @Autowired
    private WebApplicationContext webContext;
    private WebTestClient webClientWithCredentials;

    @BeforeAll
    void setUpAll()  {
        assertThat(redisContainer.isCreated()).isTrue();
        assertThat(redisContainer.isRunning()).isTrue();

        /**
         * There is a problem with using WebTestClient and mocking JWT
         * https://github.com/spring-projects/spring-security/issues/9304#issuecomment-841495717
         */
        webClientWithCredentials = MockMvcWebTestClient.bindToApplicationContext(webContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(MockMvcRequestBuilders.get("/").with(jwt)).configureClient().build();
    }

    @BeforeEach
    void setUpEach() {


    }

    /**
     * Test for getting authorities.  Only for testing purposes.
     */
    @Test
    @Order(1)
    @DisplayName("GET /api/cave/" + AUTHORITIES + " (Authorized)")
    void getAuthorities() throws Exception {

        ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};

        webClientWithCredentials.get()
                .uri("/api/cave/" + AUTHORITIES)
                .exchange()
                .expectStatus().isOk()
                .expectBody(typeRef)
                .consumeWith(response -> {

                    Map<String, Object> map = response.getResponseBody();
                    assertNotNull(map);
                    assertEquals(2, map.size());
                    assertTrue(map.containsKey("name"));
                    assertTrue(map.containsKey("authorities"));
                    assertEquals("test-user", map.get("name"));
                    assertInstanceOf(List.class, map.get("authorities"));
                    List<?> authorities = (List<?>) map.get("authorities");
                    assertEquals(4, authorities.size());
                    assertTrue(authorities.contains("SCOPE_see:thieves-treasure"));
                    assertTrue(authorities.contains("SCOPE_see:alibaba-treasure"));
                    assertTrue(authorities.contains("SCOPE_take:thieves-treasure"));
                    assertTrue(authorities.contains("ROLE_treasure-hunter"));
                });
    }


    @Test
    @Order(2)
    @DisplayName("GET /api/cave/" + THIEVES_TREASURE + " (Unauthorized)")
    void getThievesTreasureUnauthorized(@Autowired WebTestClient webClient) throws Exception {

        webClient.get()
                .uri("/api/cave/" + THIEVES_TREASURE)
                .exchange()
                .expectStatus().isUnauthorized();

    }

    @Test
    @Order(3)
    @DisplayName("GET /api/cave/" + THIEVES_TREASURE + " (Authorized)")
    void getThievesTreasureAuthorized() throws Exception {

        webClientWithCredentials.get()
                .uri("/api/cave/" + THIEVES_TREASURE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(TreasureModel.class)
                .consumeWith(response -> {
                    TreasureModel config = Objects.requireNonNull(response.getResponseBody());
                    assertNotNull(config);
                    assertEquals(THIEVES_TREASURE, config.owner());
                    assertEquals(1000, config.amount());
                });

    }
    @Test
    @Order(4)
    @DisplayName("GET /api/cave/" + ALIBABA_TREASURE + " (Unauthorized)")
    void getAliBabaTreasureUnauthorized(@Autowired WebTestClient webClient) throws Exception {

        webClient.get()
                .uri("/api/cave/" + ALIBABA_TREASURE)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/cave/" + ALIBABA_TREASURE + " (Authorized)")
    void getAliBabaTreasureAuthorized() throws Exception {

        webClientWithCredentials.get()
                .uri("/api/cave/" + ALIBABA_TREASURE)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(TreasureModel.class)
                .consumeWith(response -> {
                    TreasureModel config = Objects.requireNonNull(response.getResponseBody());
                    assertNotNull(config);
                    assertEquals(ALIBABA_TREASURE, config.owner());
                    assertEquals(0, config.amount());
                });

    }

    @Test
    @Order(6)
    @DisplayName("POST /api/cave/" + TAKE_TREASURE + " (Unauthorized)")
    void getTakeTreasureUnauthorized(@Autowired WebTestClient webClient) throws Exception {

        webClient.get()
                .uri("/api/cave/" + TAKE_TREASURE)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/cave/" + TAKE_TREASURE + " (Authorized)")
    void getTakeTreasureAuthorized() throws Exception {

        postTakeTreasure().consumeWith((response) -> {
            Map<String, Integer> treasures = response.getResponseBody();
            assertEquals(TAKE_AMOUNT, treasures.get(ALIBABA_TREASURE));
            assertEquals(1000 - TAKE_AMOUNT, treasures.get(THIEVES_TREASURE));
        });
        postTakeTreasure().consumeWith((response) -> {
            Map<String, Integer> treasures = response.getResponseBody();
            assertEquals(TAKE_AMOUNT * 2, treasures.get(ALIBABA_TREASURE));
            assertEquals(1000 - (TAKE_AMOUNT * 2), treasures.get(THIEVES_TREASURE));
        });

    }

    private WebTestClient.BodySpec<Map<String, Integer>, ?> postTakeTreasure() throws Exception {

        ParameterizedTypeReference<Map<String, Integer>> typeRef = new ParameterizedTypeReference<>() {};

        return webClientWithCredentials.post()
                .uri("/api/cave/" + TAKE_TREASURE)
                .bodyValue(new TreasureModel(ALIBABA_TREASURE, TAKE_AMOUNT))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(typeRef)
                .consumeWith((response) -> {
                    Map<String, Integer> treasures = response.getResponseBody();
                    assertNotNull(treasures);
                    assertEquals(2, treasures.size());
                    assertTrue(treasures.containsKey(ALIBABA_TREASURE));
                    assertTrue(treasures.containsKey(THIEVES_TREASURE));
                });


    }
}
