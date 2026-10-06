package com.syncboard.backend.config;

import com.syncboard.backend.entity.MemberRole;
import com.syncboard.backend.entity.Task;
import com.syncboard.backend.entity.TaskPriority;
import com.syncboard.backend.entity.User;
import com.syncboard.backend.entity.Workspace;
import com.syncboard.backend.entity.WorkspaceMember;
import com.syncboard.backend.repository.TaskRepository;
import com.syncboard.backend.repository.UserRepository;
import com.syncboard.backend.repository.WorkspaceMemberRepository;
import com.syncboard.backend.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           WorkspaceRepository workspaceRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           TaskRepository taskRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing demo data...");

            User owner = userRepository.save(new User("Alex Owner", "owner@syncboard.dev", passwordEncoder.encode("password123")));
            User editor = userRepository.save(new User("Sam Editor", "editor@syncboard.dev", passwordEncoder.encode("password123")));
            User viewer = userRepository.save(new User("Val Viewer", "viewer@syncboard.dev", passwordEncoder.encode("password123")));

            Workspace defaultWorkspace = workspaceRepository.save(new Workspace("Engineering Team"));

            workspaceMemberRepository.save(new WorkspaceMember(defaultWorkspace, owner, MemberRole.OWNER));
            workspaceMemberRepository.save(new WorkspaceMember(defaultWorkspace, editor, MemberRole.EDITOR));
            workspaceMemberRepository.save(new WorkspaceMember(defaultWorkspace, viewer, MemberRole.VIEWER));

            taskRepository.save(new Task(defaultWorkspace, "Welcome to SyncBoard", "Sample task initialized on startup.", TaskPriority.HIGH));
            taskRepository.save(new Task(defaultWorkspace, "Try moving or editing tasks", "Changes sync in real-time.", TaskPriority.MEDIUM));

            log.info("Demo users created: owner@syncboard.dev, editor@syncboard.dev, viewer@syncboard.dev (password: password123)");
        }
    }
}
