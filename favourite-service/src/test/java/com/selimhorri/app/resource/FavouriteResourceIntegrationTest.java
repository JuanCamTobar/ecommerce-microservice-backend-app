package com.selimhorri.app.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.FavouriteRepository;

import org.springframework.test.web.client.ExpectedCount;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FavouriteResourceIntegrationTest {

    @TestConfiguration
    static class TestRestConfig {
        // Provide a plain RestTemplate bean for tests so MockRestServiceServer can intercept calls
        @Bean
        @Primary
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RestTemplate clientRestTemplate;

    @Autowired
    private FavouriteRepository favouriteRepository;

    private MockRestServiceServer mockServer;

    private final ObjectMapper mapper = new ObjectMapper();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);

    private String encode(final String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        this.favouriteRepository.deleteAll();
        this.mockServer = MockRestServiceServer.createServer(this.clientRestTemplate);
    }

    private FavouriteDto buildDto(int userId, int productId) {
        return FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .build();
    }

    private String userJson(int id) throws Exception {
    return mapper.writeValueAsString(
        UserDto.builder()
            .userId(id)
            .firstName("User" + id)
            .lastName("Test")
            .email("user" + id + "@example.com")
            .build());
    }

    private String productJson(int id) throws Exception {
    return mapper.writeValueAsString(
        ProductDto.builder()
            .productId(id)
            .productTitle("Product" + id)
            .priceUnit(9.99)
            .build());
    }

    @Test
    void saveAndFindById_with_request_body_and_external_calls() throws Exception {
        FavouriteDto dto = buildDto(1, 10);

        ResponseEntity<FavouriteDto> postRes = restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);
        assertThat(postRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        this.mockServer.expect(ExpectedCount.once(), requestTo(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + dto.getUserId()))
                .andRespond(withSuccess(userJson(dto.getUserId()), MediaType.APPLICATION_JSON));

        this.mockServer.expect(ExpectedCount.once(), requestTo(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + dto.getProductId()))
                .andRespond(withSuccess(productJson(dto.getProductId()), MediaType.APPLICATION_JSON));

        FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
        ResponseEntity<FavouriteDto> getRes = restTemplate.postForEntity("/api/favourites/find", id, FavouriteDto.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        FavouriteDto found = getRes.getBody();
        assertThat(found).isNotNull();
        assertThat(found.getUserDto()).isNotNull();
        assertThat(found.getProductDto()).isNotNull();

        this.mockServer.verify();
    }

    @Test
    void findAll_returns_collection_and_resolves_external_apis() throws Exception {
        FavouriteDto a = buildDto(2, 20);
        FavouriteDto b = buildDto(3, 30);
        restTemplate.postForEntity("/api/favourites", a, FavouriteDto.class);
        restTemplate.postForEntity("/api/favourites", b, FavouriteDto.class);

        this.mockServer.expect(ExpectedCount.manyTimes(), requestTo(org.hamcrest.Matchers.startsWith(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL)))
                .andRespond(withSuccess(userJson(2), MediaType.APPLICATION_JSON));
        this.mockServer.expect(ExpectedCount.manyTimes(), requestTo(org.hamcrest.Matchers.startsWith(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL)))
                .andRespond(withSuccess(productJson(20), MediaType.APPLICATION_JSON));

        ResponseEntity<String> res = restTemplate.getForEntity("/api/favourites", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("user", "product");

        this.mockServer.verify();
    }

    @Test
    void update_existing_favourite() {
        FavouriteDto dto = buildDto(4, 40);
        restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);

        HttpEntity<FavouriteDto> request = new HttpEntity<>(dto);
        ResponseEntity<FavouriteDto> putRes = restTemplate.exchange("/api/favourites", HttpMethod.PUT, request, FavouriteDto.class);
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        
        FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
        ResponseEntity<FavouriteDto> getRes = restTemplate.postForEntity("/api/favourites/find", id, FavouriteDto.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteById_and_then_not_found() {
        FavouriteDto dto = buildDto(5, 50);
        restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);

    String likeDate = encode(dto.getLikeDate().format(fmt));
    ResponseEntity<Boolean> delRes = restTemplate.exchange(
        "/api/favourites/" + dto.getUserId() + "/" + dto.getProductId() + "/" + likeDate,
        HttpMethod.DELETE, null, Boolean.class);
        assertThat(delRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
        ResponseEntity<String> getRes = restTemplate.postForEntity("/api/favourites/find", id, String.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void findById_path_variables() throws Exception {
        FavouriteDto dto = buildDto(6, 60);
        restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);

        this.mockServer.expect(ExpectedCount.once(), requestTo(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + dto.getUserId()))
                .andRespond(withSuccess(userJson(dto.getUserId()), MediaType.APPLICATION_JSON));
        this.mockServer.expect(ExpectedCount.once(), requestTo(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + dto.getProductId()))
                .andRespond(withSuccess(productJson(dto.getProductId()), MediaType.APPLICATION_JSON));

    String likeDate = encode(dto.getLikeDate().format(fmt));
    ResponseEntity<FavouriteDto> getRes = restTemplate.getForEntity(
        "/api/favourites/" + dto.getUserId() + "/" + dto.getProductId() + "/" + likeDate,
        FavouriteDto.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        FavouriteDto found = getRes.getBody();
        assertThat(found).isNotNull();
        assertThat(found.getUserDto()).isNotNull();
        assertThat(found.getProductDto()).isNotNull();

        this.mockServer.verify();
    }

}
