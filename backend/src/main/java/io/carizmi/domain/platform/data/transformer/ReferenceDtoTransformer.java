package io.carizmi.domain.platform.data.transformer;

import io.carizmi.domain.platform.data.dto.response.ReferenceDescDto;
import io.carizmi.domain.platform.data.dto.ReferenceDto;
import io.carizmi.domain.platform.model.ReferenceVO;
import io.carizmi.framework.data.transformer.Transformer;
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
    public ReferenceDescDto transformData(ReferenceVO vo) {
        if (vo == null) return null;
        return ReferenceDescDto.builder()
                .referenceCode(vo.getReferenceCode())
                .referenceDisplay(vo.getReferenceDisplay())
                .build();
    }

    public List<ReferenceDescDto> transformDataList(List<ReferenceVO> list) {
        return list == null ? List.of() : list.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::transformData)
                .collect(Collectors.toList());
    }
}