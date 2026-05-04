package io.carizmi.domain.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.platform.data.dto.response.ReferenceDescDto;
import io.carizmi.domain.platform.data.dto.ReferenceDto;
import io.carizmi.domain.platform.data.dto.request.ReferenceSearchRequestDto;
import io.carizmi.domain.platform.data.transformer.ReferenceDtoTransformer;
import io.carizmi.domain.platform.model.ReferenceVO;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.data.response.PaginationMeta;
import io.carizmi.framework.exception.RecordNotFoundException;
import io.carizmi.domain.platform.repository.ReferenceRepository;
import io.carizmi.domain.platform.repository.spec.ReferenceSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.carizmi.shared.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public final class ReferenceImpl extends ReferenceAbstractBL implements Reference {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceImpl.class);

    private final ReferenceDtoTransformer dtoTransformer;

    @Autowired
    public ReferenceImpl(ReferenceRepository referenceRepo,
                         ReferenceDtoTransformer dtoTransformer) {
        super(referenceRepo);
        this.dtoTransformer = dtoTransformer;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(ReferenceVO vo, boolean isUpdate) {
        // No domain validation implementation for reference data currently
    }

    @Override
    public ReferenceDto getReference(Integer referenceID) {
        ReferenceVO existing = getRepo().findById(referenceID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        return dtoTransformer.transform(existing);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "referenceData", key = "#referenceName")
    public List<ReferenceDescDto> getReferencesByName(String referenceName) {
        Specification<ReferenceVO> spec = Specification.allOf(
                ReferenceSpecifications.hasReferenceName(referenceName),
                ReferenceSpecifications.isActive(true));
        // We typically want all of them, no paging, maybe sorted by Code
        List<ReferenceVO> list = getRepo().findAll(spec, Sort.by(FieldConstants.REFERENCE_CODE));
        return dtoTransformer.transformDataList(list);
    }

    @Override
    public Optional<ReferenceVO> findByNameAndCode(String referenceName, String referenceCode) {
        Specification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(referenceName)
                .and(ReferenceSpecifications.hasReferenceCode(referenceCode));
        return getRepo().findOne(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ReferenceDto> searchReferences(ReferenceSearchRequestDto request) {

        List<Specification<ReferenceVO>> specs = new ArrayList<>();
        if (request.getReferenceName() != null)
            specs.add(ReferenceSpecifications.hasReferenceName(request.getReferenceName()));

        Specification<ReferenceVO> spec = Specification.allOf(specs);
        Page<ReferenceVO> result = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return PagedResult.of(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    public boolean isValidReference(String referenceName, String referenceCode) {
        Specification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(referenceName)
                .and(ReferenceSpecifications.hasReferenceCode(referenceCode))
                .and(ReferenceSpecifications.isActive(true));
        return getRepo().exists(spec);
    }
}