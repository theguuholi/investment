package com.github.theguuholi.investment.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.theguuholi.investment.config.GlobalExceptionHandler;
import com.github.theguuholi.investment.user.api.UserController;
import com.github.theguuholi.investment.user.api.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUsers_returnsOkWithJsonArray() throws Exception {
        // given
        var user1 = buildUser("Alice Smith", "alice@example.com");
        var user2 = buildUser("Bob Jones", "bob@example.com");
        given(userService.findAll()).willReturn(List.of(user1, user2));

        // when / then
        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice Smith"))
                .andExpect(jsonPath("$[1].name").value("Bob Jones"));
    }

    @Test
    void getUserById_whenExists_returnsOkWithUser() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        var user = buildUser("Alice Smith", "alice@example.com");
        given(userService.findById(id)).willReturn(user);

        // when / then
        mockMvc.perform(get("/users/{id}", id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void getUserById_whenNotFound_returns500() throws Exception {
        // given
        UUID unknownId = UUID.randomUUID();
        given(userService.findById(unknownId)).willThrow(new RuntimeException("User not found: " + unknownId));

        // when / then
        mockMvc.perform(get("/users/{id}", unknownId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createUser_withValidBody_returnsCreatedUser() throws Exception {
        // given
        var request = new UserRequest("Carol White", "carol@example.com", "password123");
        var created = buildUser("Carol White", "carol@example.com");
        given(userService.create(any(User.class))).willReturn(created);

        // when / then
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carol White"))
                .andExpect(jsonPath("$.email").value("carol@example.com"));
    }

    @Test
    void deleteUser_returnsNoContent() throws Exception {
        // given
        UUID id = UUID.randomUUID();
        willDoNothing().given(userService).delete(id);

        // when / then
        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    private User buildUser(String name, String email) {
        var user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }
}
