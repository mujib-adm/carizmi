package org.sofumar.portal.core.businesslogic;

import java.util.List;
import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.data.dto.response.TokenDto;
import org.sofumar.portal.data.dto.response.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.springframework.lang.NonNull;

public interface User extends BusinessLogic<UserVO> {

    void register(UserDto requestDto);

    void logout(String accessToken, String refreshToken);

    TokenDto refreshToken(String refreshToken);

    UserProfileDto getProfile(String username);

    void updatePassword(String username, String token, PasswordUpdateRequestDto requestDto);

    List<UserResponseDto> getAllUsers();

    void updateUserRole(@NonNull Integer userId, String newRole);

    void toggleUserStatus(@NonNull Integer userId, boolean active);

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