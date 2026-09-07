package com.motolink.api.user;

import com.motolink.api.common.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final AppUserRepository repository;

    public UserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AppUser require(UUID userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }
}
