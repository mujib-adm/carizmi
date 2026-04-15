package io.carizmi.domain.membership.data.transformer;

import io.carizmi.domain.membership.data.dto.MemberDto;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class MemberVOTransformer implements Transformer<MemberDto, MemberVO> {

    @Override
    public MemberVO transform(MemberDto dto) {
        if (dto == null) return null;
        MemberVO memberVO = new MemberVO();
        memberVO.setFirstName(dto.getFirstName());
        memberVO.setLastName(dto.getLastName());
        memberVO.setPhone(dto.getPhone());
        memberVO.setEmail(dto.getEmail());
        memberVO.setStatus(dto.getStatus());
        memberVO.setJoinDate(dto.getJoinDate());
        memberVO.setAddress1(dto.getAddress1());
        memberVO.setAddress2(dto.getAddress2());
        memberVO.setCity(dto.getCity());
        memberVO.setState(dto.getState());
        memberVO.setZip(dto.getZip());
        return memberVO;
    }

    public MemberVO transformForUpdate(MemberDto dto, MemberVO existingMember) {
        if (dto == null || existingMember == null) return existingMember;
        existingMember.setFirstName(dto.getFirstName());
        existingMember.setLastName(dto.getLastName());
        existingMember.setPhone(dto.getPhone());
        existingMember.setEmail(dto.getEmail());
        existingMember.setStatus(dto.getStatus());
        existingMember.setJoinDate(dto.getJoinDate());
        existingMember.setAddress1(dto.getAddress1());
        existingMember.setAddress2(dto.getAddress2());
        existingMember.setCity(dto.getCity());
        existingMember.setState(dto.getState());
        existingMember.setZip(dto.getZip());
        return existingMember;
    }
}