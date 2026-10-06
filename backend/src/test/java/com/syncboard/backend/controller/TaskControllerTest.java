package com.syncboard.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncboard.backend.dto.CreateTaskRequest;
import com.syncboard.backend.dto.RegisterRequest;
import com.syncboard.backend.dto.UpdateTaskRequest;
import com.syncboard.backend.dto.WorkspaceCreateRequest;
import com.syncboard.backend.entity.TaskPriority;
import com.syncboard.backend.entity.TaskStatus;
import com.syncboard.backend.repository.ProcessedOperationRepository;
import com.syncboard.backend.repository.TaskRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private ProcessedOperationRepository processedOperationRepository;

    private String authToken;
    private Long sampleWorkspaceId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        processedOperationRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register test user
        RegisterRequest reg = new RegisterRequest("Test User", "test@syncboard.dev", "password123");
        String res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        authToken = "Bearer " + objectMapper.readTree(res).get("token").asText();

        // 2. Create workspace owned by test user
        WorkspaceCreateRequest wsReq = new WorkspaceCreateRequest("Engineering Team");
        String wsRes = mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        sampleWorkspaceId = objectMapper.readTree(wsRes).get("id").asLong();
    }

    @Test
    void createTask_returns201Created() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("Setup CI/CD", "Configure GitHub Actions", TaskPriority.HIGH);

        mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Setup CI/CD"))
                .andExpect(jsonPath("$.description").value("Configure GitHub Actions"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.workspaceId").value(sampleWorkspaceId));
    }

    @Test
    void getWorkspaceTasks_returnsTaskList() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("Task 1", "Description 1", TaskPriority.LOW);

        mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Task 1"));
    }

    @Test
    void updateTask_returnsUpdatedTask() throws Exception {
        CreateTaskRequest createReq = new CreateTaskRequest("Initial Title", "Initial Desc", TaskPriority.LOW);
        String responseString = mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdTaskId = objectMapper.readTree(responseString).get("id").asLong();

        UpdateTaskRequest updateReq = new UpdateTaskRequest("Updated Title", "Updated Desc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH);

        mockMvc.perform(put("/api/tasks/" + createdTaskId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTaskId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void updateTask_versionMismatch_returns409Conflict() throws Exception {
        CreateTaskRequest createReq = new CreateTaskRequest("Initial Title", "Initial Desc", TaskPriority.LOW);
        String responseString = mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdTaskId = objectMapper.readTree(responseString).get("id").asLong();

        // Send update with stale version 99
        UpdateTaskRequest updateReq = new UpdateTaskRequest("Updated Title", "Updated Desc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "op-1", 99L);

        mockMvc.perform(put("/api/tasks/" + createdTaskId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.currentTask.id").value(createdTaskId))
                .andExpect(jsonPath("$.currentTask.version").value(0));
    }

    @Test
    void duplicateOperationId_returns200WithDuplicateFlag() throws Exception {
        CreateTaskRequest createReq = new CreateTaskRequest("Initial Title", "Initial Desc", TaskPriority.LOW, "op-unique-123");

        // First request succeeds
        mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // Second request with same operationId returns 200 OK idempotent
        mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @Test
    void updateTask_invalidStateTransition_returns400BadRequest() throws Exception {
        CreateTaskRequest createReq = new CreateTaskRequest("Initial Title", "Initial Desc", TaskPriority.LOW);
        String responseString = mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdTaskId = objectMapper.readTree(responseString).get("id").asLong();

        // Attempt illegal transition: TODO -> DONE
        UpdateTaskRequest updateReq = new UpdateTaskRequest("Title", "Desc", TaskStatus.DONE, TaskPriority.HIGH);

        mockMvc.perform(put("/api/tasks/" + createdTaskId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid task state transition: TODO → DONE"));
    }

    @Test
    void deleteTask_returns204NoContent() throws Exception {
        CreateTaskRequest createReq = new CreateTaskRequest("Task To Delete", "Desc", TaskPriority.MEDIUM);
        String responseString = mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long createdTaskId = objectMapper.readTree(responseString).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/" + createdTaskId)
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        // Verify task is deleted
        mockMvc.perform(get("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createTask_invalidInput_returns400BadRequest() throws Exception {
        CreateTaskRequest invalidReq = new CreateTaskRequest("", "Desc", null);

        mockMvc.perform(post("/api/workspaces/" + sampleWorkspaceId + "/tasks")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
