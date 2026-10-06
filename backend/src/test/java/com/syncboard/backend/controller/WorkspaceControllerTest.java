package com.syncboard.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncboard.backend.dto.LoginRequest;
import com.syncboard.backend.dto.RegisterRequest;
import com.syncboard.backend.dto.WorkspaceCreateRequest;
import com.syncboard.backend.repository.UserRepository;
import com.syncboard.backend.repository.WorkspaceMemberRepository;
import com.syncboard.backend.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private com.syncboard.backend.repository.TaskRepository taskRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        RegisterRequest reg = new RegisterRequest("Test User", "test@syncboard.dev", "password123");
        String res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        authToken = "Bearer " + objectMapper.readTree(res).get("token").asText();
    }

    @Test
    void createWorkspace_and_getAllWorkspaces() throws Exception {
        WorkspaceCreateRequest request = new WorkspaceCreateRequest("Engineering Workspace");

        // 1. POST /api/workspaces
        mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Engineering Workspace"))
                .andExpect(jsonPath("$.createdAt").exists());

        // 2. GET /api/workspaces
        mockMvc.perform(get("/api/workspaces")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Engineering Workspace"));
    }
}
