package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.core.vo.ReferenceVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferenceDtoTransformer implements Transformer<ReferenceVO, ReferenceDto> {

    @Override
    public ReferenceDto transform(ReferenceVO vo) {
        return ReferenceDto.builder()
                .referenceID(vo.getReferenceID())
                .referenceName(vo.getReferenceName())
                .referenceCode(vo.getReferenceCode())
                .referenceDisplay(vo.getReferenceDisplay())
                .active(vo.isActive())
                .build();
    }

    public List<ReferenceDto> transformList(List<ReferenceVO> list) {
        return list.stream().map(this::transform).collect(Collectors.toList());
    }

    // For display purposes with limited fields
    public ReferenceDataDto transformData(ReferenceVO vo) {
        return ReferenceDataDto.builder()
                .referenceCode(vo.getReferenceCode())
                .referenceDisplay(vo.getReferenceDisplay())
                .build();
    }

    public List<ReferenceDataDto> transformDataList(List<ReferenceVO> list) {
        return list.stream().map(this::transformData).collect(Collectors.toList());
    }
}