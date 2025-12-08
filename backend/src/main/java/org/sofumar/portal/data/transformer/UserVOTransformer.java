package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.data.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserVOTransformer implements Transformer<UserRequestDto, UserVO> {

    @Override
    public UserVO transform(UserRequestDto dto) {
        UserVO userVO = new UserVO();
        userVO.setUsername(dto.getUsername());
        userVO.setFirstName(dto.getFirstName());
        userVO.setLastName(dto.getLastName());
        userVO.setEmail(dto.getEmail());
        userVO.setPassword(dto.getPassword());
        return userVO;
    }

}
