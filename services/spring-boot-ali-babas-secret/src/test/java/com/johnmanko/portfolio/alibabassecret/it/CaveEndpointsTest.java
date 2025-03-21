package com.johnmanko.portfolio.alibabassecret.it;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnmanko.portfolio.alibabassecret.models.TreasureModel;
import com.johnmanko.portfolio.alibabassecret.services.RedisService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisKeyValueAdapterAutoConfiguration",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost/",
        "app.config.client.auth.auth0.domain=test.us.auth0.com",
        "app.config.client.auth.auth0.client-id=ASDF1234",
        "app.config.server.auth.auth0.custom-jwt-namespace=custom.jwt.namespace/roles"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Testing '/api/cave/*' API")
public class CaveEndpointsTest {

    static final String THIEVES_TREASURE = "thieves-treasure";
    static final String ALIBABA_TREASURE = "alibaba-treasure";
    static final String TAKE_TREASURE = "take-treasure";
    static final String AUTHORITIES = "authorities";
    static final int TAKE_AMOUNT = 20;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    ValueOperations<String, String> valueOperations;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private RedisService redisService;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private HashMap<String, String> treasures = new HashMap<>();

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwt = jwt().jwt(jwt -> {
        jwt.subject("test-user");
    }).authorities(
            new SimpleGrantedAuthority("SCOPE_see:thieves-treasure"),
            new SimpleGrantedAuthority("SCOPE_see:alibaba-treasure"),
            new SimpleGrantedAuthority("SCOPE_take:thieves-treasure"),
            new SimpleGrantedAuthority("ROLE_treasure-hunter")
    );

    @BeforeEach
    void setUp() {

        treasures.clear();

        // Step 2: When redisTemplate.opsForValue() is called, return this mock
        doReturn(valueOperations).when(redisTemplate).opsForValue();

        // Step 3: Modify the behavior of set() on the mock ValueOperations instance
        doAnswer(invocation -> {
            // Retrieves method arguments
            String key = invocation.getArgument(0);  // The Redis key
            String value = invocation.getArgument(1); // The value to store

            // Simulates storing the value in a local HashMap (mocking Redis behavior)
            treasures.put(key, value);

            return null; // Since opsForValue().set() returns void, return null
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        doAnswer(invocation -> {
            // Retrieves method arguments
            String key = invocation.getArgument(0);  // The Redis key
            return treasures.get(key);
        }).when(valueOperations).get(anyString());

    }

    /**
     * Test for getting authorities.  Only for testing purposes.
     */
    @Test
    @Order(1)
    @DisplayName("GET /api/cave/" + AUTHORITIES + " (Authorized)")
    void getPrincipalInfo() throws Exception {

        String jsonResponse = mvc.perform(
                        get("/api/cave/" + AUTHORITIES).with(jwt))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

        Map<String, Object> map = objectMapper.readValue(jsonResponse, typeRef);

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

    }

    @Test
    @Order(2)
    @DisplayName("GET /api/cave/" + THIEVES_TREASURE + " (Unauthorized)")
    void getThievesTreasureUnauthorized() throws Exception {
        mvc.perform(get("/api/cave/" + THIEVES_TREASURE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/cave/" + THIEVES_TREASURE + " (Authorized)")
    void getThievesTreasureAuthorized() throws Exception {

        MvcResult mvcResult = mvc.perform(
                    get("/api/cave/" + THIEVES_TREASURE)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwt))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        String jsonResponse = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TreasureModel config = objectMapper.readValue(jsonResponse, TreasureModel.class);
        assertNotNull(config);
        assertEquals(THIEVES_TREASURE, config.owner());
        assertEquals(1000, config.amount());

    }

    @Test
    @Order(4)
    @DisplayName("GET /api/cave/" + ALIBABA_TREASURE + " (Unauthorized)")
    void getAliBabaTreasureUnauthorized() throws Exception {
        mvc.perform(get("/api/cave/" + ALIBABA_TREASURE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/cave/" + ALIBABA_TREASURE + " (Authorized)")
    void getAliBabaTreasureAuthorized() throws Exception {

        MvcResult mvcResult = mvc.perform(
                        get("/api/cave/" + ALIBABA_TREASURE)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(jwt))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        String jsonResponse = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TreasureModel config = objectMapper.readValue(jsonResponse, TreasureModel.class);
        assertNotNull(config);
        assertEquals(ALIBABA_TREASURE, config.owner());
        assertEquals(0, config.amount());

    }

    @Test
    @Order(6)
    @DisplayName("POST /api/cave/" + TAKE_TREASURE + " (Unauthorized)")
    void getTakeTreasureUnauthorized() throws Exception {
        mvc.perform(get("/api/cave/" + TAKE_TREASURE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/cave/" + TAKE_TREASURE + " (Authorized)")
    void getTakeTreasureAuthorized() throws Exception {

        Map<String, Integer> treasures = postTakeTreasure();
        assertNotNull(treasures);
        assertEquals(2, treasures.size());
        assertTrue(treasures.containsKey(ALIBABA_TREASURE));
        assertTrue(treasures.containsKey(THIEVES_TREASURE));
        assertEquals(TAKE_AMOUNT, treasures.get(ALIBABA_TREASURE));
        assertEquals(1000 - TAKE_AMOUNT, treasures.get(THIEVES_TREASURE));

        treasures = postTakeTreasure();
        assertNotNull(treasures);
        assertEquals(2, treasures.size());
        assertTrue(treasures.containsKey(ALIBABA_TREASURE));
        assertTrue(treasures.containsKey(THIEVES_TREASURE));
        assertEquals(TAKE_AMOUNT * 2, treasures.get(ALIBABA_TREASURE));
        assertEquals(1000 - (TAKE_AMOUNT * 2), treasures.get(THIEVES_TREASURE));

    }

    private Map<String, Integer> postTakeTreasure() throws Exception {

        TreasureModel mt = new TreasureModel(ALIBABA_TREASURE, TAKE_AMOUNT);

        MvcResult mvcResult = mvc.perform(
                        post("/api/cave/" + TAKE_TREASURE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mt))
                                .accept(MediaType.APPLICATION_JSON)
                                .with(jwt))
                .andDo(print())
                .andExpect(request().asyncStarted())
                .andReturn();

        String jsonResponse = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TypeReference<Map<String, Integer>> typeRef = new TypeReference<>() {};

        return objectMapper.readValue(jsonResponse, typeRef);

    }

}
