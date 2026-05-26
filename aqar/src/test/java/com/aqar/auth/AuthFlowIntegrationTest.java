package com.aqar.auth;

import com.aqar.auth.config.JwtProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

	@Autowired
	WebApplicationContext applicationContext;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	JwtProperties jwtProperties;

	MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
	}

	@Test
	void registerLoginRefreshLogoutFlowWorksAndReplayIsRejected() throws Exception {
		String email = "integration@example.com";
		String password = "password123";

		JsonNode register = performAuthPost("/api/v1/auth/register", Map.of("email", email, "password", password));
		String accessToken = register.get("accessToken").asText();
		String refreshToken = register.get("refreshToken").asText();

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk());

		JsonNode login = performAuthPost("/api/v1/auth/login", Map.of("email", email, "password", password));
		String loginRefreshToken = login.get("refreshToken").asText();

		JsonNode refreshed = performAuthPost("/api/v1/auth/refresh", Map.of("refreshToken", loginRefreshToken));
		String rotatedRefreshToken = refreshed.get("refreshToken").asText();

		mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("refreshToken", loginRefreshToken))))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("refreshToken", rotatedRefreshToken))))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
				.andExpect(status().isNoContent());
	}

	@Test
	void duplicateEmailReturnsConflict() throws Exception {
		Map<String, String> body = Map.of("email", "duplicate@example.com", "password", "password123");
		performAuthPost("/api/v1/auth/register", body);

		mockMvc.perform(post("/api/v1/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isConflict());
	}

	@Test
	void tamperedAccessTokenReturnsUnauthorized() throws Exception {
		JsonNode register = performAuthPost(
				"/api/v1/auth/register",
				Map.of("email", "tampered@example.com", "password", "password123"));
		String token = register.get("accessToken").asText();
		String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tamperedToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredAccessTokenReturnsUnauthorized() throws Exception {
		String expiredToken = Jwts.builder()
				.setSubject("999")
				.claim("role", "USER")
				.setIssuer(jwtProperties.issuer())
				.setIssuedAt(java.util.Date.from(Instant.now().minusSeconds(3600)))
				.setExpiration(java.util.Date.from(Instant.now().minusSeconds(1)))
				.signWith(Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
				.compact();

		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());
	}

	private JsonNode performAuthPost(String path, Map<String, String> body) throws Exception {
		String content = objectMapper.writeValueAsString(new LinkedHashMap<>(body));
		String response = mockMvc.perform(post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content(content))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(response);
	}
}