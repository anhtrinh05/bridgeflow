package com.bridgeflow.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bridgeflow.api.auth.domain.AppUser;
import com.bridgeflow.api.auth.persistence.AppUserRepository;
import com.bridgeflow.api.project.membership.ProjectMember;
import com.bridgeflow.api.project.membership.ProjectMemberRepository;
import com.bridgeflow.api.project.membership.ProjectRole;
import com.bridgeflow.api.project.persistence.ProjectRepository;

@Component
@Profile("dev")
public class DevAuthDataInitializer implements ApplicationRunner {

    private static final String DEMO_EMAIL = "brse@bridgeflow.local";
    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DevAuthDataInitializer(
        AppUserRepository userRepository,
        ProjectRepository projectRepository,
        ProjectMemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        @Value("${BRIDGEFLOW_DEMO_PASSWORD:bridgeflow-demo}") String demoPassword
    ) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        var user = userRepository.findByEmailIgnoreCase(DEMO_EMAIL)
            .orElseGet(() -> userRepository.save(new AppUser(
                DEMO_EMAIL, "Trần Minh – BrSE", passwordEncoder.encode(demoPassword)
            )));
        projectRepository.findByCode("EC-RENEWAL").ifPresent(project -> {
            if (!memberRepository.existsByProjectIdAndUserId(project.getId(), user.getId())) {
                memberRepository.save(new ProjectMember(project, user, ProjectRole.BRSE));
            }
        });
    }
}
