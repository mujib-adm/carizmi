package org.sofumar.portal.core.businesslogic;

import org.sofumar.portal.data.dto.response.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto;
import org.sofumar.portal.core.vo.ReferenceVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface Reference extends BusinessLogic<ReferenceVO> {

    ResponseEntity<GlobalResponse<ReferenceDto>> getReference(Integer referenceID);

    ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(ReferenceSearchRequestDto request);

    ResponseEntity<GlobalResponse<List<ReferenceDataDto>>> getReferencesByName(String referenceName);

    boolean isValidReference(String referenceName, String referenceCode);

    Optional<ReferenceVO> findByNameAndCode(String referenceName, String referenceCode);
}