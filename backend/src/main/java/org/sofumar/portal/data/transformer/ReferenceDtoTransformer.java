package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.response.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.core.vo.ReferenceVO;
import org.sofumar.portal.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferenceDtoTransformer implements Transformer<ReferenceVO, ReferenceDto> {

    @Override
    public ReferenceDto transform(ReferenceVO vo) {
        if (vo == null) return null;
        return ReferenceDto.builder()
                .referenceID(vo.getReferenceID())
                .referenceName(vo.getReferenceName())
                .referenceCode(vo.getReferenceCode())
                .referenceDisplay(vo.getReferenceDisplay())
                .active(vo.isActive())
                .build();
    }

    // For display purposes with limited fields
    public ReferenceDataDto transformData(ReferenceVO vo) {
        if (vo == null) return null;
        return ReferenceDataDto.builder()
                .referenceCode(vo.getReferenceCode())
                .referenceDisplay(vo.getReferenceDisplay())
                .build();
    }

    public List<ReferenceDataDto> transformDataList(List<ReferenceVO> list) {
        return list == null ? List.of() : list.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::transformData)
                .collect(Collectors.toList());
    }
}