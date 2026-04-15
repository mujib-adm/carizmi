package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.data.dto.response.ReferenceDescDto;
import io.carizmi.domain.platform.data.dto.ReferenceDto;
import io.carizmi.domain.platform.data.dto.request.ReferenceSearchRequestDto;
import io.carizmi.domain.platform.model.ReferenceVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.PagedResult;

import java.util.List;
import java.util.Optional;

public interface Reference extends BusinessLogic<ReferenceVO> {

    ReferenceDto getReference(Integer referenceID);

    PagedResult<ReferenceDto> searchReferences(ReferenceSearchRequestDto request);

    List<ReferenceDescDto> getReferencesByName(String referenceName);

    boolean isValidReference(String referenceName, String referenceCode);

    Optional<ReferenceVO> findByNameAndCode(String referenceName, String referenceCode);
}