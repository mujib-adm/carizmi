package org.sofumar.portal.service.businesslogic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.dto.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.transformer.ReferenceDtoTransformer;
import org.sofumar.portal.data.vo.ReferenceVO;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.ReferenceRepository;
import org.sofumar.portal.repo.jpaspec.ReferenceSpecifications;
import org.sofumar.portal.service.businesslogic.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReferenceServiceImpl extends AbstractBusinessLogic<ReferenceVO, ReferenceRepository>
        implements ReferenceService {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceServiceImpl.class);

    private final ReferenceRepository referenceRepo;
    private final ReferenceDtoTransformer dtoTransformer;

    @Autowired
    public ReferenceServiceImpl(ReferenceRepository referenceRepo,
            ReferenceDtoTransformer dtoTransformer) {
        this.referenceRepo = referenceRepo;
        this.dtoTransformer = dtoTransformer;
    }

    @Override
    protected ReferenceRepository getRepository() {
        return referenceRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public ResponseEntity<GlobalResponse<ReferenceDto>> getReference(Integer referenceID) {
        ReferenceVO existing = referenceRepo.findById(referenceID)
                .orElseThrow(() -> new RecordNotFoundException("Reference not found: " + referenceID));
        return ResponseUtils.okWithData(dtoTransformer.transform(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(
            String referenceName, String referenceCode, Boolean active,
            int page, int size, String sortField, String sortOrder) {

        List<Specification<ReferenceVO>> specs = new ArrayList<>();
        if (referenceName != null)
            specs.add(ReferenceSpecifications.hasReferenceName(referenceName));
        if (referenceCode != null)
            specs.add(ReferenceSpecifications.hasReferenceCode(referenceCode));
        if (active != null)
            specs.add(ReferenceSpecifications.isActive(active));

        Specification<ReferenceVO> spec = Specification.allOf(specs);
        Sort sort = Sort.unsorted();
        if (sortField != null && sortOrder != null) {
            sort = Sort.by(Sort.Direction.fromString(sortOrder), sortField);
        }
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<ReferenceVO> result = referenceRepo.findAll(spec, pageRequest);
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<GlobalResponse<List<ReferenceDataDto>>> getReferencesByName(String referenceName) {
        Specification<ReferenceVO> spec = Specification.allOf(
                ReferenceSpecifications.hasReferenceName(referenceName),
                ReferenceSpecifications.isActive(true));
        // We typically want all of them, no paging, maybe sorted by Code
        List<ReferenceVO> list = referenceRepo.findAll(spec, Sort.by(FieldConstants.REFERENCE_CODE));
        return ResponseUtils.okWithData(dtoTransformer.transformDataList(list));
    }
}