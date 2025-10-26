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
 

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
 
import com.selimhorri.app.repository.FavouriteRepository;

 

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FavouriteResourceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FavouriteRepository favouriteRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);


    @BeforeEach
    void setUp() {
        this.favouriteRepository.deleteAll();
    }

    private FavouriteDto buildDto(int userId, int productId) {
        return FavouriteDto.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(LocalDateTime.now())
                .build();
    }


    @Test
    void saveAndFindById_with_request_body_and_external_calls() throws Exception {
        FavouriteDto dto = buildDto(1, 10);

    ResponseEntity<String> postRes = restTemplate.postForEntity("/api/favourites", dto, String.class);
    assertThat(postRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    FavouriteDto saved = mapper.readValue(postRes.getBody(), FavouriteDto.class);
    assertThat(saved).isNotNull();

    FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
    ResponseEntity<String> getRes = restTemplate.postForEntity("/api/favourites/find", id, String.class);
    assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    FavouriteDto found = mapper.readValue(getRes.getBody(), FavouriteDto.class);
    assertThat(found).isNotNull();
    assertThat(found.getUserDto()).isNotNull();
    assertThat(found.getProductDto()).isNotNull();

    }

    @Test
    void findAll_returns_collection_and_resolves_external_apis() throws Exception {
        FavouriteDto a = buildDto(2, 20);
        FavouriteDto b = buildDto(3, 30);
    restTemplate.postForEntity("/api/favourites", a, String.class);
    restTemplate.postForEntity("/api/favourites", b, String.class);

        ResponseEntity<String> res = restTemplate.getForEntity("/api/favourites", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("user", "product");

    }

    @Test
    void update_existing_favourite() throws Exception {
        FavouriteDto dto = buildDto(4, 40);
    restTemplate.postForEntity("/api/favourites", dto, String.class);

    HttpEntity<FavouriteDto> request = new HttpEntity<>(dto);
    ResponseEntity<String> putRes = restTemplate.exchange("/api/favourites", HttpMethod.PUT, request, String.class);
    assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
    ResponseEntity<String> getRes = restTemplate.postForEntity("/api/favourites/find", id, String.class);
    assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteById_and_then_not_found() throws Exception {
        FavouriteDto dto = buildDto(5, 50);
        restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);

    String likeDate = dto.getLikeDate().format(fmt);
    ResponseEntity<String> delRes = restTemplate.exchange(
        "/api/favourites/{userId}/{productId}/{likeDate}", HttpMethod.DELETE, null, String.class,
        dto.getUserId(), dto.getProductId(), likeDate);
        assertThat(delRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        Boolean deleted = Boolean.valueOf(delRes.getBody());
        assertThat(deleted).isTrue();

        FavouriteId id = new FavouriteId(dto.getUserId(), dto.getProductId(), dto.getLikeDate());
        ResponseEntity<String> getRes = restTemplate.postForEntity("/api/favourites/find", id, String.class);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void findById_path_variables() throws Exception {
        FavouriteDto dto = buildDto(6, 60);
        restTemplate.postForEntity("/api/favourites", dto, FavouriteDto.class);

    String likeDate = dto.getLikeDate().format(fmt);
    ResponseEntity<String> getRes = restTemplate.getForEntity(
        "/api/favourites/{userId}/{productId}/{likeDate}", String.class,
        dto.getUserId(), dto.getProductId(), likeDate);
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        FavouriteDto found = mapper.readValue(getRes.getBody(), FavouriteDto.class);
        assertThat(found).isNotNull();
        assertThat(found.getUserDto()).isNotNull();
        assertThat(found.getProductDto()).isNotNull();
    }

}
