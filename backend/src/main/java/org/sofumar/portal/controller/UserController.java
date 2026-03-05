package org.sofumar.portal.controller;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.data.dto.request.UserRoleUpdateRequestDto;
import org.sofumar.portal.data.dto.request.UserStatusUpdateRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.security.annotation.IsAdmin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.lang.NonNull;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@IsAdmin
public class UserController {

    private final User user;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers() {
        return user.getAllUsers();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<GlobalResponse<Void>> updateRole(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody UserRoleUpdateRequestDto request) {
        return user.updateUserRole(id, request.getRole());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody UserStatusUpdateRequestDto request) {
        return user.toggleUserStatus(id, request.getActive());
    }
}