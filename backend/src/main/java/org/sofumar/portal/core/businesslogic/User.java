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

public interface User extends BusinessLogic<UserVO> {
    ResponseEntity<GlobalResponse<Void>> register(UserDto requestDto);

    ResponseEntity<?> login(String username, String password);

    void logout(String accessToken, String refreshToken);

    ResponseEntity<?> refreshToken(String refreshToken);

    ResponseEntity<GlobalResponse<UserProfileDto>> getProfile(String username);

    ResponseEntity<GlobalResponse<Void>> updatePassword(String username, String token, PasswordUpdateRequestDto requestDto);

    ResponseEntity<GlobalResponse<List<UserResponseDto>>> getAllUsers();

    ResponseEntity<GlobalResponse<Void>> updateUserRole(Integer userId, String newRole);

    ResponseEntity<GlobalResponse<Void>> toggleUserStatus(Integer userId, boolean active);

    boolean adminUserExists();

}