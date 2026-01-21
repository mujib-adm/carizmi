package org.sofumar.portal.controller;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.service.businesslogic.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<UserDto>>> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<GlobalResponse<Void>> updateRole(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        return userService.updateUserRole(id, newRole);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<GlobalResponse<Void>> toggleStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        return userService.toggleUserStatus(id, active);
    }
}