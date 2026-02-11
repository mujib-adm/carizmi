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
     *
     * @param username The username to search for.
     * @return The UserVO if found, or null/empty.
     */
    UserVO findUserForAuthentication(String username);

    /**
     * Handles logic after a successful login (e.g., resetting failed attempts).
     *
     * @param username The username of the logged-in user.
     */
    void onLoginSuccess(String username);

    /**
     * Handles logic after a failed login (e.g., incrementing failed attempts, locking account).
     *
     * @param username The username of the user who failed to login.
     */
    void onLoginFailure(String username);
}