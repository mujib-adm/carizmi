package io.carizmi.domain.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.identity.service.User;
import io.carizmi.domain.identity.data.dto.response.UserResponseDto;
import io.carizmi.domain.identity.data.dto.request.UserRoleUpdateRequestDto;
import io.carizmi.domain.identity.data.dto.request.UserStatusUpdateRequestDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.infrastructure.security.annotation.IsAdmin;
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

import static io.carizmi.shared.message.ValidationMessages.*;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "User administration APIs")
@RequiredArgsConstructor
@IsAdmin
public class UserController {

    private final User user;

    @GetMapping("/")
    @Operation(summary = "Get all users")
    public ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers() {
        return ResponseUtils.okWithData(user.getAllUsers());
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<GlobalResponse<Void>> updateRole(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody UserRoleUpdateRequestDto request) {
        user.updateUserRole(id, request.getRole());
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User role").getMessageString());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(
            @PathVariable @NonNull Integer id,
            @Valid @RequestBody UserStatusUpdateRequestDto request) {
        user.toggleUserStatus(id, request.getActive());
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User status").getMessageString());
    }
}