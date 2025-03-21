package com.johnmanko.portfolio.alibabassecret.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnmanko.portfolio.alibabassecret.models.AppConfigModel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/",
        "app.config.client.auth.auth0.domain=test.us.auth0.com",
        "app.config.client.auth.auth0.client-id=ASDF1234",
        "app.config.server.auth.auth0.custom-jwt-namespace=custom.jwt.namespace/roles"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Testing '/public/*' API")
public class PublicEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    @Order(1)
    @DisplayName("GET /public/config.json")
    void getConfigJSON() throws Exception {
        String jsonResponse = mockMvc.perform(get("/public/config.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AppConfigModel config = objectMapper.readValue(jsonResponse, AppConfigModel.class);
        assertNotNull(config);
        assertEquals("ASDF1234", config.authAuth0ClientId());
        assertEquals("test.us.auth0.com", config.authAuth0Domain());
    }

}
