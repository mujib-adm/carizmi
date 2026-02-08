package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.response.UserResponseDto;
import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class UserResponseDtoTransformer implements Transformer<UserVO, UserResponseDto> {

    @Override
    public UserResponseDto transform(UserVO vo) {
        if (vo == null)
            return null;
        return UserResponseDto.builder()
                .userID(vo.getUserID())
                .username(vo.getUsername())
                .email(vo.getEmail())
                .firstName(vo.getFirstName())
                .lastName(vo.getLastName())
                .role(vo.getRole() != null ? vo.getRole().name() : null)
                .active(vo.isActive())
                .build();
    }
}