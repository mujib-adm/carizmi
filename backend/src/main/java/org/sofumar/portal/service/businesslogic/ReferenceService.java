package org.sofumar.portal.service.businesslogic;

import org.sofumar.portal.data.dto.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.vo.ReferenceVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ReferenceService extends BusinessLogic<ReferenceVO> {

    ResponseEntity<GlobalResponse<ReferenceDto>> getReference(Integer referenceID);

    ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(
            String referenceName, String referenceCode, Boolean active,
            int page, int size, String sortField, String sortOrder);

    ResponseEntity<GlobalResponse<List<ReferenceDataDto>>> getReferencesByName(String referenceName);
}