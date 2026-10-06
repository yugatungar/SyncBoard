package com.syncboard.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncboard.backend.dto.AddMemberRequest;
import com.syncboard.backend.dto.CreateTaskRequest;
import com.syncboard.backend.dto.RegisterRequest;
import com.syncboard.backend.dto.WorkspaceCreateRequest;
import com.syncboard.backend.entity.MemberRole;
import com.syncboard.backend.entity.TaskPriority;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceMemberControllerTest {

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

    private String ownerToken;
    private String editorToken;
    private String viewerToken;
    private Long workspaceId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Owner User
        ownerToken = registerAndGetToken("Owner User", "owner@syncboard.dev");
        // 2. Create Editor User
        editorToken = registerAndGetToken("Editor User", "editor@syncboard.dev");
        // 3. Create Viewer User
        viewerToken = registerAndGetToken("Viewer User", "viewer@syncboard.dev");

        // 4. Owner creates workspace
        WorkspaceCreateRequest wsReq = new WorkspaceCreateRequest("Engineering Workspace");
        String wsRes = mockMvc.perform(post("/api/workspaces")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        workspaceId = objectMapper.readTree(wsRes).get("id").asLong();

        // 5. Owner adds Editor and Viewer members
        AddMemberRequest addEditor = new AddMemberRequest("editor@syncboard.dev", MemberRole.EDITOR);
        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/members")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addEditor)))
                .andExpect(status().isCreated());

        AddMemberRequest addViewer = new AddMemberRequest("viewer@syncboard.dev", MemberRole.VIEWER);
        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/members")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addViewer)))
                .andExpect(status().isCreated());
    }

    private String registerAndGetToken(String name, String email) throws Exception {
        RegisterRequest reg = new RegisterRequest(name, email, "password123");
        String res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(res).get("token").asText();
    }

    @Test
    void editor_canCreateTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("Editor Task", "Desc", TaskPriority.HIGH);
        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/tasks")
                        .header("Authorization", editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Editor Task"));
    }

    @Test
    void viewer_cannotCreateTask_returns403Forbidden() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("Viewer Task Attempt", "Desc", TaskPriority.HIGH);
        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/tasks")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void editor_cannotAddMembers_returns403Forbidden() throws Exception {
        AddMemberRequest req = new AddMemberRequest("viewer@syncboard.dev", MemberRole.VIEWER);
        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/members")
                        .header("Authorization", editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewer_canViewTasks() throws Exception {
        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/tasks")
                        .header("Authorization", viewerToken))
                .andExpect(status().isOk());
    }
}
