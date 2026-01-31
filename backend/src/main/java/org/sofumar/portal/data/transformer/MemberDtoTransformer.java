package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.core.vo.MemberVO;
import org.springframework.stereotype.Service;

@Service
public class MemberDtoTransformer implements Transformer<MemberVO, MemberDto> {

    @Override
    public MemberDto transform(MemberVO vo) {
        MemberDto requestDto = new MemberDto();
        requestDto.setMemberID(vo.getMemberID());
        requestDto.setFirstName(vo.getFirstName());
        requestDto.setLastName(vo.getLastName());
        requestDto.setPhone(vo.getPhone());
        requestDto.setEmail(vo.getEmail());
        requestDto.setStatus(vo.getStatus());
        requestDto.setJoinDate(vo.getJoinDate());
        requestDto.setAddress1(vo.getAddress1());
        requestDto.setAddress2(vo.getAddress2());
        requestDto.setCity(vo.getCity());
        requestDto.setState(vo.getState());
        requestDto.setZip(vo.getZip());
        return requestDto;
    }
}