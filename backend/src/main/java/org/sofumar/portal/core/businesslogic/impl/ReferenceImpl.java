package org.sofumar.portal.core.businesslogic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.dto.response.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto;
import org.sofumar.portal.data.transformer.ReferenceDtoTransformer;
import org.sofumar.portal.core.vo.ReferenceVO;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.repo.ReferenceRepository;
import org.sofumar.portal.core.repo.jpaspec.ReferenceSpecifications;
import org.sofumar.portal.core.businesslogic.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public non-sealed class ReferenceImpl extends ReferenceAbstractBL implements Reference {

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
    public ResponseEntity<GlobalResponse<ReferenceDto>> getReference(Integer referenceID) {
        ReferenceVO existing = getRepo().findById(referenceID)
                .orElseThrow(() -> new RecordNotFoundException("Reference not found: " + referenceID));
        return ResponseUtils.okWithData(dtoTransformer.transform(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<ReferenceDataDto>>> getReferencesByName(String referenceName) {
        Specification<ReferenceVO> spec = Specification.allOf(
                ReferenceSpecifications.hasReferenceName(referenceName),
                ReferenceSpecifications.isActive(true));
        // We typically want all of them, no paging, maybe sorted by Code
        List<ReferenceVO> list = getRepo().findAll(spec, Sort.by(FieldConstants.REFERENCE_CODE));
        return ResponseUtils.okWithData(dtoTransformer.transformDataList(list));
    }

    @Override
    public Optional<ReferenceVO> findByNameAndCode(String referenceName, String referenceCode) {
        Specification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(referenceName)
                .and(ReferenceSpecifications.hasReferenceCode(referenceCode));
        return getRepo().findOne(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(ReferenceSearchRequestDto request) {

        List<Specification<ReferenceVO>> specs = new ArrayList<>();
        if (request.getReferenceName() != null)
            specs.add(ReferenceSpecifications.hasReferenceName(request.getReferenceName()));

        Specification<ReferenceVO> spec = Specification.allOf(specs);
        Page<ReferenceVO> result = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    public boolean isValidReference(String referenceName, String referenceCode) {
        Specification<ReferenceVO> spec = ReferenceSpecifications.hasReferenceName(referenceName)
                .and(ReferenceSpecifications.hasReferenceCode(referenceCode))
                .and(ReferenceSpecifications.isActive(true));
        return getRepo().exists(spec);
    }
}