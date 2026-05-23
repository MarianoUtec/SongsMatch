package com.musicmatch.controller;

import com.musicmatch.service.interfaces.IUserService;

import com.musicmatch.dto.request.UpdateUserRequest;
import com.musicmatch.dto.response.PageResponse;
import com.musicmatch.dto.response.UserResponse;
import com.musicmatch.service.impl.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    // ── User endpoints ────────────────────────────────────────────────────────

    @GetMapping("/api/v1/users/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PutMapping("/api/v1/users/me")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/users")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> result = userService.getAllUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(new PageResponse<>(
            result.getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages(), result.isLast()
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/v1/admin/users/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
