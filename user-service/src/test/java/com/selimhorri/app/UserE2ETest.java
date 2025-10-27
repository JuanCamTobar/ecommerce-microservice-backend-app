package com.selimhorri.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:user_e2e_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.config.import=optional:file:./",
        "SPRING_CONFIG_IMPORT=optional:file:./",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "server.servlet.context-path="
})
class UserE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        this.userRepository.deleteAll();
    }

    private UserDto buildUser(String username, String firstName, String lastName) {
        CredentialDto cred = CredentialDto.builder()
                .username(username)
                .password("secret")
                .build();
        return UserDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .credentialDto(cred)
                .build();
    }

    @Test
    void createUser_thenGetById() {
        UserDto payload = buildUser("alice1", "Alice", "Smith");
        ResponseEntity<UserDto> postResp = this.restTemplate.postForEntity("/api/users", payload, UserDto.class);
        assertThat(postResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto created = postResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getUserId()).isNotNull();

        ResponseEntity<UserDto> getResp = this.restTemplate.getForEntity("/api/users/{id}", UserDto.class, created.getUserId());
        assertThat(getResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto fetched = getResp.getBody();
        assertThat(fetched.getFirstName()).isEqualTo("Alice");
    }

    @Test
    void createUser_thenFindByUsername() {
        UserDto payload = buildUser("bob1", "Bob", "Jones");
        UserDto created = this.restTemplate.postForEntity("/api/users", payload, UserDto.class).getBody();
        assertThat(created).isNotNull();

        ResponseEntity<UserDto> resp = this.restTemplate.getForEntity("/api/users/username/{username}", UserDto.class, "bob1");
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto found = resp.getBody();
        assertThat(found.getUserId()).isEqualTo(created.getUserId());
    }

    @Test
    void updateUser_viaPut_bodyAndById() {
        UserDto payload = buildUser("carol1", "Carol", "White");
        UserDto created = this.restTemplate.postForEntity("/api/users", payload, UserDto.class).getBody();
        assertThat(created).isNotNull();

        created.setFirstName("Caroline");
        ResponseEntity<UserDto> putResp = this.restTemplate.exchange("/api/users", HttpMethod.PUT, new HttpEntity<>(created), UserDto.class);
        assertThat(putResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto updated = putResp.getBody();
        assertThat(updated.getFirstName()).isEqualTo("Caroline");

        updated.setLastName("Black");
        ResponseEntity<UserDto> putById = this.restTemplate.exchange("/api/users/{id}", HttpMethod.PUT, new HttpEntity<>(updated), UserDto.class, updated.getUserId());
        assertThat(putById.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(putById.getBody().getLastName()).isEqualTo("Black");
    }

    @Test
    void deleteUser_shouldRemoveAndNotFind() {
        UserDto payload = buildUser("dan1", "Dan", "Brown");
        UserDto created = this.restTemplate.postForEntity("/api/users", payload, UserDto.class).getBody();
        Integer id = created.getUserId();

        ResponseEntity<Boolean> del = this.restTemplate.exchange("/api/users/{id}", HttpMethod.DELETE, HttpEntity.EMPTY, Boolean.class, id);
        assertThat(del.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(del.getBody()).isTrue();

        Optional<com.selimhorri.app.domain.User> maybe = this.userRepository.findById(id);
        assertThat(maybe).isNotPresent();
    }

    @Test
    void createMultiple_thenFindAll() {
        this.restTemplate.postForEntity("/api/users", buildUser("u1", "U", "One"), UserDto.class);
        this.restTemplate.postForEntity("/api/users", buildUser("u2", "V", "Two"), UserDto.class);

    ResponseEntity<com.selimhorri.app.dto.response.collection.DtoCollectionResponse<com.selimhorri.app.dto.UserDto>> resp = this.restTemplate.getForEntity("/api/users", (Class) com.selimhorri.app.dto.response.collection.DtoCollectionResponse.class);
    assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    com.selimhorri.app.dto.response.collection.DtoCollectionResponse<com.selimhorri.app.dto.UserDto> body = resp.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getCollection()).asList().hasSizeGreaterThanOrEqualTo(2);
    }

}
