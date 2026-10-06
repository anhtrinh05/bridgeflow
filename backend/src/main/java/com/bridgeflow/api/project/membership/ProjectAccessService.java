package com.bridgeflow.api.project.membership;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessService {

    private final ProjectMemberRepository memberRepository;

    public ProjectAccessService(ProjectMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public ProjectMember requireMember(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new AccessDeniedException("Bạn không phải thành viên của project này."));
    }

    public ProjectMember requireRole(UUID projectId, UUID userId, ProjectRole... roles) {
        var member = requireMember(projectId, userId);
        if (!Set.of(roles).contains(member.getRole())) {
            throw new AccessDeniedException("Role hiện tại không được phép thực hiện thao tác này.");
        }
        return member;
    }
}
