package io.carizmi.domain.identity.data.transformer;

import io.carizmi.domain.identity.data.dto.UserDto;
import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.data.transformer.Transformer;
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
