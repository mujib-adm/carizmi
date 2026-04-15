package io.carizmi.domain.identity.data.transformer;

import io.carizmi.domain.identity.data.dto.response.UserResponseDto;
import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.data.transformer.Transformer;
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