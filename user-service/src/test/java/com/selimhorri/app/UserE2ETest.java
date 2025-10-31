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
    void createUserWithCompleteProfile_thenUpdate_thenVerifyAllChanges() {
        UserDto payload = UserDto.builder()
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@example.com")
                .phone("+1234567890")
                .imageUrl("https://example.com/avatar.jpg")
                .credentialDto(CredentialDto.builder()
                        .username("juanperez")
                        .password("securePass123")
                        .build())
                .build();

        ResponseEntity<UserDto> createResp = this.restTemplate.postForEntity("/api/users", payload, UserDto.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getUserId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("juan.perez@example.com");
        assertThat(created.getPhone()).isEqualTo("+1234567890");

        created.setFirstName("Juan Carlos");
        created.setLastName("Pérez García");
        created.setEmail("juancarlos.perez@example.com");
        created.setPhone("+0987654321");

        ResponseEntity<UserDto> updateResp = this.restTemplate.exchange(
                "/api/users/{userId}",
                HttpMethod.PUT,
                new HttpEntity<>(created),
                UserDto.class,
                created.getUserId());
        assertThat(updateResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto updated = updateResp.getBody();
        
        assertThat(updated.getFirstName()).isEqualTo("Juan Carlos");
        assertThat(updated.getLastName()).isEqualTo("Pérez García");
        assertThat(updated.getEmail()).isEqualTo("juancarlos.perez@example.com");
        assertThat(updated.getPhone()).isEqualTo("+0987654321");
    }

    @Test
    void createUser_thenGetById_thenDelete_thenVerifyNotFound() {
        UserDto payload = buildUser("testuser", "Test", "User");
        ResponseEntity<UserDto> createResp = this.restTemplate.postForEntity("/api/users", payload, UserDto.class);
        assertThat(createResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto created = createResp.getBody();
        assertThat(created).isNotNull();
        Integer userId = created.getUserId();

        ResponseEntity<UserDto> getResp = this.restTemplate.getForEntity("/api/users/{id}", UserDto.class, userId);
        assertThat(getResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getResp.getBody().getUserId()).isEqualTo(userId);

        ResponseEntity<Boolean> delResp = this.restTemplate.exchange(
                "/api/users/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Boolean.class,
                userId);
        assertThat(delResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(delResp.getBody()).isTrue();

        Optional<com.selimhorri.app.domain.User> deleted = this.userRepository.findById(userId);
        assertThat(deleted).isNotPresent();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createMultipleUsers_thenFindAll_thenFindByUsername_thenUpdateOne() {
        UserDto user1 = this.restTemplate.postForEntity("/api/users", 
                buildUser("alice_2024", "Alice", "Wonderland"), UserDto.class).getBody();
        UserDto user2 = this.restTemplate.postForEntity("/api/users", 
                buildUser("bob_2024", "Bob", "Builder"), UserDto.class).getBody();
        UserDto user3 = this.restTemplate.postForEntity("/api/users", 
                buildUser("charlie_2024", "Charlie", "Brown"), UserDto.class).getBody();

        assertThat(user1).isNotNull();
        assertThat(user2).isNotNull();
        assertThat(user3).isNotNull();

        ResponseEntity<com.selimhorri.app.dto.response.collection.DtoCollectionResponse<UserDto>> getAllResp = 
                this.restTemplate.getForEntity("/api/users",
                (Class<com.selimhorri.app.dto.response.collection.DtoCollectionResponse<UserDto>>) 
                (Class<?>) com.selimhorri.app.dto.response.collection.DtoCollectionResponse.class);
        assertThat(getAllResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getAllResp.getBody().getCollection()).asList().hasSizeGreaterThanOrEqualTo(3);

        ResponseEntity<UserDto> findByUsernameResp = this.restTemplate.getForEntity(
                "/api/users/username/{username}",
                UserDto.class,
                "bob_2024");
        assertThat(findByUsernameResp.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto found = findByUsernameResp.getBody();
        assertThat(found.getFirstName()).isEqualTo("Bob");
        assertThat(found.getLastName()).isEqualTo("Builder");

        user2.setFirstName("Robert");
        user2.setLastName("The Builder");

        ResponseEntity<UserDto> updateResp = this.restTemplate.exchange(
                "/api/users/{userId}",
                HttpMethod.PUT,
                new HttpEntity<>(user2),
                UserDto.class,
                user2.getUserId());
        assertThat(updateResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(updateResp.getBody().getFirstName()).isEqualTo("Robert");
    }

    @Test
    void createUser_thenUpdatePartialFields_thenVerifyDatabaseState() {
        UserDto initialUser = UserDto.builder()
                .firstName("María")
                .lastName("González")
                .email("maria.gonzalez@example.com")
                .phone("555-0001")
                .credentialDto(CredentialDto.builder()
                        .username("mariagonz")
                        .password("password123")
                        .build())
                .build();

        ResponseEntity<UserDto> createResp = this.restTemplate.postForEntity("/api/users", initialUser, UserDto.class);
        UserDto created = createResp.getBody();
        assertThat(created).isNotNull();
        Integer userId = created.getUserId();

        created.setLastName("González Rodríguez");
        created.setImageUrl("https://example.com/new-avatar.jpg");

        ResponseEntity<UserDto> updateResp = this.restTemplate.exchange(
                "/api/users/{userId}",
                HttpMethod.PUT,
                new HttpEntity<>(created),
                UserDto.class,
                userId);
        assertThat(updateResp.getStatusCode().is2xxSuccessful()).isTrue();

        UserDto updated = updateResp.getBody();
        assertThat(updated.getFirstName()).isEqualTo("María");
        assertThat(updated.getLastName()).isEqualTo("González Rodríguez");
        assertThat(updated.getEmail()).isEqualTo("maria.gonzalez@example.com");
        assertThat(updated.getPhone()).isEqualTo("555-0001");
        assertThat(updated.getImageUrl()).isEqualTo("https://example.com/new-avatar.jpg");
    }

    @Test
    void createUser_thenRetrieve_thenUpdateByPath_thenRetrieveAgain() {
        UserDto newUser = UserDto.builder()
                .firstName("Pedro")
                .lastName("Martínez")
                .email("pedro.martinez@example.com")
                .phone("+555-1234")
                .credentialDto(CredentialDto.builder()
                        .username("pedromart")
                        .password("secret123")
                        .build())
                .build();

        ResponseEntity<UserDto> create = this.restTemplate.postForEntity("/api/users", newUser, UserDto.class);
        assertThat(create.getStatusCode().is2xxSuccessful()).isTrue();
        UserDto created = create.getBody();
        assertThat(created.getUserId()).isNotNull();

        ResponseEntity<UserDto> getById = this.restTemplate.getForEntity(
                "/api/users/{id}",
                UserDto.class,
                created.getUserId());   
        assertThat(getById.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getById.getBody().getFirstName()).isEqualTo("Pedro");

        created.setFirstName("Pedro Luis");
        created.setEmail("pedroluis.martinez@example.com");

        ResponseEntity<UserDto> updateById = this.restTemplate.exchange(
                "/api/users/{userId}",
                HttpMethod.PUT,
                new HttpEntity<>(created),
                UserDto.class,
                created.getUserId());
        assertThat(updateById.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<UserDto> getAgain = this.restTemplate.getForEntity(
                "/api/users/{id}",
                UserDto.class,
                created.getUserId());
        assertThat(getAgain.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getAgain.getBody().getFirstName()).isEqualTo("Pedro Luis");
        assertThat(getAgain.getBody().getEmail()).isEqualTo("pedroluis.martinez@example.com");
    }

}
