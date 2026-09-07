package com.motolink.api.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SessionService sessionService;

    public AuthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/dev-login")
    LoginResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        SessionService.Session session = sessionService.create(request.nickname());
        return new LoginResponse(
                session.token(),
                session.userId(),
                session.nickname(),
                session.expiresAt());
    }

    public record DevLoginRequest(
            @NotBlank(message = "昵称不能为空")
            @Size(min = 2, max = 20, message = "昵称长度应为 2-20 个字符")
            String nickname) {
    }

    public record LoginResponse(
            String token,
            String userId,
            String nickname,
            Instant expiresAt) {
    }
}
