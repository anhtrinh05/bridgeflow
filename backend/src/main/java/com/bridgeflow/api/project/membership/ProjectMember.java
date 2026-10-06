package com.bridgeflow.api.project.membership;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.project.domain.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_project_members_project_user", columnNames = {"project_id", "user_id"}
    )
)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, AppUser user, ProjectRole role) {
        this.project = Objects.requireNonNull(project, "project is required");
        this.user = Objects.requireNonNull(user, "user is required");
        this.role = Objects.requireNonNull(role, "role is required");
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Project getProject() { return project; }
    public AppUser getUser() { return user; }
    public ProjectRole getRole() { return role; }
}
