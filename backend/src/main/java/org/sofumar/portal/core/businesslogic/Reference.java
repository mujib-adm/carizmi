package org.sofumar.portal.core.businesslogic;

import org.sofumar.portal.data.dto.response.ReferenceDescDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto;
import org.sofumar.portal.core.vo.ReferenceVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.PagedResult;

import java.util.List;
import java.util.Optional;

public interface Reference extends BusinessLogic<ReferenceVO> {

    ReferenceDto getReference(Integer referenceID);

    PagedResult<ReferenceDto> searchReferences(ReferenceSearchRequestDto request);

    List<ReferenceDescDto> getReferencesByName(String referenceName);

    boolean isValidReference(String referenceName, String referenceCode);

    Optional<ReferenceVO> findByNameAndCode(String referenceName, String referenceCode);
}