package com.syncboard.backend.config;

import com.syncboard.backend.repository.WorkspaceMemberRepository;
import com.syncboard.backend.security.JwtTokenProvider;
import com.syncboard.backend.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final Pattern WORKSPACE_TOPIC_PATTERN = Pattern.compile("^/topic/workspaces/(\\d+)$");

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public WebSocketConfig(JwtTokenProvider tokenProvider,
                           UserDetailsServiceImpl userDetailsService,
                           WorkspaceMemberRepository workspaceMemberRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null) {
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        authenticateAccessor(accessor);
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        authenticateAccessor(accessor);
                        authorizeSubscription(accessor);
                    }
                }
                return message;
            }
        });
    }

    private void authenticateAccessor(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return; // Already authenticated
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                accessor.setUser(auth);
                log.info("WebSocket authenticated user '{}'", email);
            }
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Matcher matcher = WORKSPACE_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Long workspaceId = Long.parseLong(matcher.group(1));

            if (accessor.getUser() == null) {
                log.warn("Anonymous WebSocket subscription attempt to workspace #{}", workspaceId);
                // Allow unauthenticated connection fallback in dev if needed, or enforce:
                // throw new AccessDeniedException("Authentication required to subscribe to workspace events");
                return;
            }

            String userEmail = accessor.getUser().getName();
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (userDetails instanceof com.syncboard.backend.security.UserPrincipal principal) {
                Long userId = principal.getId();
                if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
                    log.warn("User '{}' (#{}) denied WebSocket subscription to unauthorized workspace #{}", userEmail, userId, workspaceId);
                    throw new AccessDeniedException("User is not a member of workspace #" + workspaceId);
                }
                log.info("Authorized WebSocket subscription for user '{}' (#{}) to workspace #{}", userEmail, userId, workspaceId);
            }
        }
    }
}
