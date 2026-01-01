package org.sofumar.portal.service.businesslogic.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.dto.MemberDto;
import org.sofumar.portal.data.dto.MemberLookupDto;
import org.sofumar.portal.data.transformer.MemberDtoTransformer;
import org.sofumar.portal.data.transformer.MemberVOTransformer;
import org.sofumar.portal.data.vo.MemberVO;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.MemberRepository;
import org.sofumar.portal.repo.jpaspec.MemberSpecifications;
import org.sofumar.portal.service.businesslogic.MemberService;
import org.sofumar.portal.service.validation.MemberValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.sofumar.portal.constants.MessagesConstants.RECORD_ADDED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_DELETED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;
import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
public class MemberServiceImpl extends AbstractBusinessLogic<MemberVO, MemberRepository> implements MemberService {
    private static final Logger logger = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final MemberRepository memberRepo;
    private final MemberVOTransformer voTransformer;
    private final MemberDtoTransformer dtoTransformer;
    private final MemberValidator validator;

    @Autowired
    public MemberServiceImpl(final MemberRepository memberRepo, final MemberVOTransformer voTransformer, MemberDtoTransformer dtoTransformer, final MemberValidator validator) {
        this.memberRepo = memberRepo;
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected MemberRepository getRepository() {
        return memberRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Integer>> addMember(MemberDto requestDto) {
        MemberVO memberVO = voTransformer.transform(requestDto);
        validator.validate(memberVO);
        MemberVO savedMember = add(memberVO);
        logger.info("Member added successfully with ID: {}", savedMember.getMemberID());
        return ResponseUtils.okWithData(savedMember.getMemberID(), RECORD_ADDED.addMessageArgs("Member").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateMember(MemberDto requestDto) {
        if (requestDto.getMemberID() == null) {
            return ResponseUtils.badRequest(REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)).getMessageString());
        }

        MemberVO existingMember = memberRepo.findById(requestDto.getMemberID())
                .orElseThrow(() -> new RecordNotFoundException("Member not found with ID: " + requestDto.getMemberID()));

        MemberVO updatedMember = voTransformer.transformForUpdate(requestDto, existingMember);
        validator.validateForUpdate(updatedMember);
        MemberVO savedMember = update(updatedMember);
        logger.info("Member updated successfully, memberID: {}", savedMember.getMemberID());
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Member").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> deleteMember(Integer memberID) {
        if (memberID == null) {
            return ResponseUtils.badRequest(REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)).getMessageString());
        }

        MemberVO existingMember = memberRepo.findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException("Member not found with ID: " + memberID));

        delete(existingMember);
        logger.info("Member deleted successfully, memberID: {}", memberID);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Member").getMessageString());
    }

    @Override
    public ResponseEntity<GlobalResponse<List<MemberDto>>> searchMembers(String firstName, String lastName, String phone, String email,
                                                                         String status, String city, String state, String zip,
                                                                         LocalDate joinDateFrom, LocalDate joinDateTo, int page, int size, String sortField, String sortOrder) {
        logger.info("Searching members with criteria - firstName: {}, lastName: {}, status: {}, city: {}, state: {}",
                firstName, lastName, status, city, state);

        List<Specification<MemberVO>> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(firstName))
            specList.add(MemberSpecifications.hasFirstName(firstName));
        if (StringUtils.isNotBlank(lastName))
            specList.add(MemberSpecifications.hasLastName(lastName));
        if (StringUtils.isNotBlank(phone))
            specList.add(MemberSpecifications.hasPhone(phone));
        if (StringUtils.isNotBlank(email))
            specList.add(MemberSpecifications.hasEmail(email));
        if (StringUtils.isNotBlank(status))
            specList.add(MemberSpecifications.hasState(status));

        if (joinDateFrom != null && joinDateTo != null)
            specList.add(MemberSpecifications.joinDateBetween(joinDateFrom, joinDateTo));

        Specification<MemberVO> spec = Specification.allOf(specList);

        Sort sort = Sort.unsorted();
        if (sortField != null && sortOrder != null) {
            sort = Sort.by(Sort.Direction.fromString(sortOrder), sortField);
        }
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<MemberVO> members = memberRepo.findAll(spec, pageRequest);
        PaginationMeta meta = PaginationMeta.of(members.getNumber(), members.getSize(), members.getTotalElements(), members.getTotalPages());
        logger.info("Found {} members matching search criteria", members.getTotalElements());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(members.toList()), meta);
    }

    @Override
    public ResponseEntity<GlobalResponse<MemberDto>> getMember(Integer memberID) {
        logger.info("Fetching member with ID: {}", memberID);

        if (memberID == null) {
            return ResponseUtils.badRequestWithData(REQUIRED_FIELD.addMessageArgs(LabelUtils.toLabel(FieldConstants.MEMBER_ID)).getMessageString());
        }

        MemberVO member = memberRepo.findById(memberID)
                .orElseThrow(() -> new RecordNotFoundException("Member not found with ID: " + memberID));

        return ResponseUtils.okWithData(dtoTransformer.transform(member));
    }

    @Override
    public ResponseEntity<GlobalResponse<List<MemberLookupDto>>> lookupMembers(String query) {
        Specification<MemberVO> spec = MemberSpecifications.lookup(query);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME));
        Page<MemberVO> members = memberRepo.findAll(spec, pageRequest);

        List<MemberLookupDto> dtos = members.getContent().stream()
                .map(m -> MemberLookupDto.builder()
                        .memberID(m.getMemberID())
                        .firstName(m.getFirstName())
                        .lastName(m.getLastName())
                        .phone(m.getPhone())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseUtils.okWithData(dtos);
    }
}