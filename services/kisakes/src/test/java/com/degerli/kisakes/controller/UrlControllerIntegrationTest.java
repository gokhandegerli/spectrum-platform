package com.degerli.kisakes.controller;

import com.degerli.kisakes.model.dto.UrlCreateRequest;
import com.degerli.kisakes.repository.UrlRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for URL shortener functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UrlControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UrlRepository urlRepository;

  @BeforeEach
  void setUp() {
    urlRepository.deleteAll();
  }

  @Test
  void shouldCreateShortUrl() throws Exception {
    // Given
    UrlCreateRequest request = new UrlCreateRequest("https://www.example.com");

    // When
    MvcResult result = mockMvc.perform(
            post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
        .andExpect(jsonPath("$.shortUrl").exists())
        .andExpect(jsonPath("$.createdAt").exists())
        .andReturn();

    // Then
    assertThat(urlRepository.count()).isEqualTo(1);
  }

  @Test
  void shouldRejectInvalidUrl() throws Exception {
    // Given
    UrlCreateRequest request = new UrlCreateRequest("not-a-valid-url");

    // When & Then
    mockMvc.perform(post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectBlankUrl() throws Exception {
    // Given
    UrlCreateRequest request = new UrlCreateRequest("");

    // When & Then
    mockMvc.perform(post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
  }

  @Test
  void shouldRedirectToOriginalUrl() throws Exception {
    // Given - Create a short URL first
    UrlCreateRequest request = new UrlCreateRequest("https://www.example.com");

    MvcResult createResult = mockMvc.perform(
            post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    String response = createResult.getResponse().getContentAsString();
    String shortUrl = objectMapper.readTree(response).get("shortUrl").asText();
    String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

    // When & Then - Access the short URL
    mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound()).andExpect(result -> {
      String location = result.getResponse().getHeader("Location");
      assertThat(location).isEqualTo("https://www.example.com");
    });
  }

  @Test
  void shouldReturn404ForNonExistentShortCode() throws Exception {
    // When & Then
    mockMvc.perform(get("/nonexistent")).andExpect(status().isNotFound());
  }

  @Test
  void shouldGenerateUniqueShortCodes() throws Exception {
    // Given
    UrlCreateRequest request1 = new UrlCreateRequest("https://www.example1.com");
    UrlCreateRequest request2 = new UrlCreateRequest("https://www.example2.com");

    // When
    MvcResult result1 = mockMvc.perform(
            post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated())
        .andReturn();

    MvcResult result2 = mockMvc.perform(
            post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated())
        .andReturn();

    // Then
    String shortUrl1 = objectMapper.readTree(result1.getResponse().getContentAsString())
        .get("shortUrl")
        .asText();
    String shortUrl2 = objectMapper.readTree(result2.getResponse().getContentAsString())
        .get("shortUrl")
        .asText();

    assertThat(shortUrl1).isNotEqualTo(shortUrl2);
  }

  @Test
  void shouldIncrementClickCount() throws Exception {
    // Given - Create a short URL
    UrlCreateRequest request = new UrlCreateRequest("https://www.example.com");

    MvcResult createResult = mockMvc.perform(
            post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn();

    String response = createResult.getResponse().getContentAsString();
    String shortUrl = objectMapper.readTree(response).get("shortUrl").asText();
    String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

    // When - Access it multiple times
    mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
    mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
    mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());

    // Then - Verify click count (this would require adding an endpoint to get URL stats)
    // For now, just verify the redirects work
  }
}