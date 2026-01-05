package org.sofumar.portal.service.businesslogic;

import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.data.vo.UserVO;
import org.springframework.http.ResponseEntity;

public interface UserService extends BusinessLogic<UserVO> {
    ResponseEntity<GlobalResponse<Void>> register(UserRequestDto requestDto);

    ResponseEntity<?> login(String username, String password);

    void logout(String token);

    ResponseEntity<?> getProfile(String username);
}