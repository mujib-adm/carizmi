package org.sofumar.portal.controller;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.core.businesslogic.User;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final User user;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<UserDto>>> getAllUsers() {
        return user.getAllUsers();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<GlobalResponse<Void>> updateRole(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        return user.updateUserRole(id, newRole);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        return user.toggleUserStatus(id, active);
    }
}