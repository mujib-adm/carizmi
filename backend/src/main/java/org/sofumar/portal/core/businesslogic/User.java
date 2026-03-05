package org.sofumar.portal.core.businesslogic;

import java.util.List;
import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.data.dto.response.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

public interface User extends BusinessLogic<UserVO> {

    ResponseEntity<GlobalResponse<Void>> register(UserDto requestDto);

    void logout(String accessToken, String refreshToken);

    ResponseEntity<?> refreshToken(String refreshToken);

    ResponseEntity<GlobalResponse<UserProfileDto>> getProfile(String username);

    ResponseEntity<GlobalResponse<Void>> updatePassword(String username, String token, PasswordUpdateRequestDto requestDto);

    ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers();

    ResponseEntity<GlobalResponse<Void>> updateUserRole(@NonNull Integer userId, String newRole);

    ResponseEntity<GlobalResponse<Void>> toggleUserStatus(@NonNull Integer userId, boolean active);

    boolean adminUserExists();

    /**
     * Finds a user by username for authentication purposes.
     * Used by UserDetailsService.
     */
    UserVO findUserForAuthentication(String username);

    /**
     * Handles logic after a successful login (e.g., resetting failed attempts).
     */
    void onLoginSuccess(String username);

    /**
     * Handles logic after a failed login (e.g., incrementing failed attempts, locking account).
     */
    void onLoginFailure(String username);

    boolean existsByUsername(String username, Integer userId);

    boolean existsByEmail(String email, Integer userId);
}