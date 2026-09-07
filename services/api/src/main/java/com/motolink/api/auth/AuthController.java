package com.motolink.api.auth;

import com.motolink.api.user.AppUser;
import com.motolink.api.user.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;

    public AuthController(AppUserRepository users) {
        this.users = users;
    }

    @PostMapping("/dev-login")
    @Transactional
    public DevLoginResponse devLogin(@Valid @RequestBody DevLoginRequest request) {
        AppUser user = users.findByPhone(request.phone())
                .orElseGet(() -> new AppUser(request.phone(), request.nickname()));
        user.updateProfile(request.nickname(), null);
        user = users.save(user);
        return new DevLoginResponse(user.getId(), user.getPhone(), user.getNickname());
    }

    public record DevLoginRequest(
            @NotBlank @Pattern(regexp = "^[0-9+ -]{6,20}$") String phone,
            @NotBlank String nickname) {}

    public record DevLoginResponse(UUID userId, String phone, String nickname) {}
}
