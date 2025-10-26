package com.selimhorri.app.business.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.business.user.model.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void testFindAllIntegration() throws Exception {
        System.out.println("[INTEGRATION TEST] testFindAllIntegration - start");

        MvcResult result = mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("[INTEGRATION TEST] testFindAllIntegration - response: " + content);

        System.out.println("[INTEGRATION TEST] testFindAllIntegration - end");
    }


    @Test
    void testFindByIdIntegration() throws Exception {
        System.out.println("[INTEGRATION TEST] testFindByIdIntegration - start");

        MvcResult result = mockMvc.perform(get("/api/users/{id}", 1))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("[INTEGRATION TEST] testFindByIdIntegration - response: " + content);

        System.out.println("[INTEGRATION TEST] testFindByIdIntegration - end");
    }

    @Test
    void testFindByUsernameIntegration() throws Exception {
        System.out.println("[INTEGRATION TEST] testFindByUsernameIntegration - start");

        MvcResult result = mockMvc.perform(get("/api/users/username/{username}", "juan"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("[INTEGRATION TEST] testFindByUsernameIntegration - response: " + content);

        System.out.println("[INTEGRATION TEST] testFindByUsernameIntegration - end");
    }

  
    @Test
    void testSaveUserIntegration() throws Exception {
        System.out.println("[INTEGRATION TEST] testSaveUserIntegration - start");

        UserDto newUser = UserDto.builder()
                .firstName("Maria")
                .lastName("Lopez")
                .email("maria@mail.com")
                .build();

        String requestJson = objectMapper.writeValueAsString(newUser);
        System.out.println("[INTEGRATION TEST] testSaveUserIntegration - request JSON: " + requestJson);

        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("[INTEGRATION TEST] testSaveUserIntegration - response: " + content);

        System.out.println("[INTEGRATION TEST] testSaveUserIntegration - end");
    }

  
    @Test
    void testDeleteUserIntegration() throws Exception {
        System.out.println("[INTEGRATION TEST] testDeleteUserIntegration - start");

        MvcResult result = mockMvc.perform(delete("/api/users/{id}", 5))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("[INTEGRATION TEST] testDeleteUserIntegration - response: " + content);

        System.out.println("[INTEGRATION TEST] testDeleteUserIntegration - end");
    }
}
