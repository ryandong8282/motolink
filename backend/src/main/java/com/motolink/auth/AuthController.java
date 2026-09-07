package com.motolink.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/dev-login")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse devLogin(@Valid @RequestBody LoginRequest request) {
        UUID userId = UUID.nameUUIDFromBytes(
                request.nickname().trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        return new LoginResponse(
                userId,
                request.nickname().trim(),
                "demo." + userId,
                Instant.now().plusSeconds(24 * 60 * 60));
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64) String nickname) {
    }

    public record LoginResponse(
            UUID userId,
            String nickname,
            String accessToken,
            Instant expiresAt) {
    }
}
