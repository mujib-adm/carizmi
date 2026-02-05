package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class UserVOTransformer implements Transformer<UserDto, UserVO> {

    @Override
    public UserVO transform(UserDto dto) {
        if (dto == null) return null;
        UserVO userVO = new UserVO();
        userVO.setUsername(dto.getUsername());
        userVO.setFirstName(dto.getFirstName());
        userVO.setLastName(dto.getLastName());
        userVO.setEmail(dto.getEmail());
        userVO.setPassword(dto.getPassword());
        return userVO;
    }

}
