package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.core.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class UserDtoTransformer implements Transformer<UserVO, UserDto> {

    @Override
    public UserDto transform(UserVO vo) {
        if (vo == null)
            return null;
        return UserDto.builder()
                .userID(vo.getUserID())
                .username(vo.getUsername())
                .email(vo.getEmail())
                .firstName(vo.getFirstName())
                .lastName(vo.getLastName())
                .role(vo.getRole())
                .active(vo.isActive())
                .build();
    }
}