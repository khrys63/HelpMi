package com.helpmi;

import com.helpmi.domain.*;
import com.helpmi.domain.enums.UserRole;


import java.util.UUID;

public final class Fixtures {

    private Fixtures() {}

    public static Organization organization() {
        return Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organisation")
                .active(true)
                .build();
    }


    public static User adminUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .active(true)
                .build();
    }

    public static User agentUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("User")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

    public static User agentUserWithOrg(Organization org) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("User")
                .role(UserRole.USER)
                .active(true)
                .organization(org)
                .build();
    }

    public static User clientUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("client@test.com")
                .firstName("Client")
                .lastName("User")
                .role(UserRole.USER)
                .active(true)
                .build();
    }

    public static Project project() {
        return Project.builder()
                .id(UUID.randomUUID())
                .name("Test Project")
                .key("TEST")
                .active(true)
                .ticketSequence(0)
                .build();
    }

    public static Ticket ticket(Project project, User reporter) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .reference("TEST-1")
                .title("Test Ticket")
                .description("Some description")
                .status("OPEN")
                .priority("MEDIUM")
                .type("TASK")
                .project(project)
                .reporter(reporter)
                .build();
    }

    public static ConfigValue configValue(String category, String code) {
        return ConfigValue.builder()
                .id(UUID.randomUUID())
                .category(category)
                .code(code)
                .label(code.charAt(0) + code.substring(1).toLowerCase())
                .color("blue")
                .active(true)
                .position(1)
                .build();
    }

    public static Client client(String name) {
        return Client.builder()
                .id(UUID.randomUUID())
                .name(name)
                .contactEmail(name.toLowerCase().replace(" ", "") + "@test.com")
                .active(true)
                .build();
    }

    public static Label label(String name) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name(name)
                .color("blue")
                .build();
    }
}
