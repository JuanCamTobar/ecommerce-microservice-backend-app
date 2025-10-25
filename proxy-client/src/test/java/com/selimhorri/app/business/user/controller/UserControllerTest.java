package com.selimhorri.app.business.user.controller;

import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;
import com.selimhorri.app.business.user.service.UserClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


class UserControllerTest {

    @Mock
    private UserClientService userClientService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        com.selimhorri.app.business.user.model.UserDto user = com.selimhorri.app.business.user.model.UserDto.builder()
                .userId(1)
                .firstName("juan")
                .lastName("perez")
                .email("juan@mail.com")
                .build();
        UserUserServiceCollectionDtoResponse mockResponse = new UserUserServiceCollectionDtoResponse(List.of(user));

        when(userClientService.findAll()).thenReturn(ResponseEntity.ok(mockResponse));

        var result = userController.findAll();
        assertThat(result.getBody().getCollection()).hasSize(1);
    }

    @Test
    void testFindById() {
        com.selimhorri.app.business.user.model.UserDto mockUser = com.selimhorri.app.business.user.model.UserDto.builder()
                .userId(1)
                .firstName("juan")
                .email("juan@mail.com")
                .build();
        when(userClientService.findById("1")).thenReturn(ResponseEntity.ok(mockUser));

        var result = userController.findById("1");
        assertThat(result.getBody().getFirstName()).isEqualTo("juan");
    }

    @Test
    void testFindByUsername() {
        com.selimhorri.app.business.user.model.UserDto mockUser = com.selimhorri.app.business.user.model.UserDto.builder()
                .userId(2)
                .firstName("carlos")
                .email("carlos@mail.com")
                .build();
        when(userClientService.findByUsername("carlos")).thenReturn(ResponseEntity.ok(mockUser));

        var result = userController.findByUsername("carlos");
        assertThat(result.getBody().getEmail()).isEqualTo("carlos@mail.com");
    }

    @Test
    void testSaveUser() {
        com.selimhorri.app.business.user.model.UserDto input = com.selimhorri.app.business.user.model.UserDto.builder()
                .firstName("maria")
                .email("maria@mail.com")
                .build();
        com.selimhorri.app.business.user.model.UserDto saved = com.selimhorri.app.business.user.model.UserDto.builder()
                .userId(10)
                .firstName("maria")
                .email("maria@mail.com")
                .build();
        when(userClientService.save(input)).thenReturn(ResponseEntity.ok(saved));

        var result = userController.save(input);
        assertThat(result.getBody().getUserId()).isEqualTo(10);
    }

    @Test
    void testDeleteUser() {
        when(userClientService.deleteById("99")).thenReturn(ResponseEntity.ok(true));

        var result = userController.deleteById("99");
        assertThat(result.getBody()).isTrue();
    }
}
