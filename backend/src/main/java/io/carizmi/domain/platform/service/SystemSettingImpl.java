package io.carizmi.domain.platform.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.domain.platform.constants.SystemSettingConstants;
import io.carizmi.domain.platform.data.dto.SystemSettingsDto;
import io.carizmi.domain.platform.data.dto.request.SystemSettingsSearchRequestDto;
import io.carizmi.domain.platform.data.transformer.SystemSettingsDtoTransformer;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.data.response.PaginationMeta;
import io.carizmi.framework.exception.RecordNotFoundException;
import io.carizmi.domain.platform.repository.SystemSettingRepository;
import io.carizmi.domain.platform.repository.spec.SystemSettingsSpecifications;
import io.carizmi.domain.platform.validation.SystemSettingsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.carizmi.shared.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public non-sealed class SystemSettingImpl extends SystemSettingAbstractBL implements SystemSetting {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingImpl.class);

    private final SystemSettingsDtoTransformer dtoTransformer;
    private final SystemSettingsValidator validator;

    @Autowired
    public SystemSettingImpl(SystemSettingRepository settingsRepo,
                             SystemSettingsDtoTransformer dtoTransformer,
                             SystemSettingsValidator validator) {
        super(settingsRepo);
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(SystemSettingsVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
    }

    @Override
    @Transactional
    public void updateSystemSetting(SystemSettingsDto dto) {
        SystemSettingsVO existing = getRepo().findById(dto.getSystemSettingsID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        // UI only allows updating settingValue. 
        // We preserve settingName and settingKey to ensure integrity with sync mechanism.
        existing.setSettingValue(dto.getSettingValue());

        update(existing);
    }


    @Override
    @Transactional(readOnly = true)
    public SystemSettingsDto getSystemSetting(Integer id) {
        SystemSettingsVO vo = getRepo().findById(id)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        return dtoTransformer.transform(vo);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<SystemSettingsDto> searchSystemSettings(SystemSettingsSearchRequestDto request) {
        List<Specification<SystemSettingsVO>> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getSettingName()))
            specList.add(SystemSettingsSpecifications.hasSettingName(request.getSettingName()));

        Specification<SystemSettingsVO> spec = Specification.allOf(specList);

        Page<SystemSettingsVO> pageResult = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages());

        return PagedResult.of(dtoTransformer.transformList(pageResult.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingsDto> getSettingsByKey(String key) {
        List<SystemSettingsVO> list = getRepo().findAll(SystemSettingsSpecifications.withSettingKey(key));
        return dtoTransformer.transformList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemSettingsVO> findBySettingKey(String key) {
        return getRepo().findOne(SystemSettingsSpecifications.withSettingKey(key));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SystemSettingsVO> findByNameAndKey(String settingName, String key) {
        Specification<SystemSettingsVO> spec = SystemSettingsSpecifications.withSettingName(settingName)
                .and(SystemSettingsSpecifications.withSettingKey(key));
        return getRepo().findOne(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getQuarterlyFeeAmount() {
        BigDecimal feeAmount = new BigDecimal(
                findBySettingKey(SystemSettingConstants.FEE.MEMBERSHIP_FEE)
                        .orElseThrow(() -> new IllegalStateException("Required system setting not found: " + SystemSettingConstants.FEE.MEMBERSHIP_FEE))
                        .getSettingValue()
        );

        if (feeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invalid MEMBERSHIP_FEE value: " + feeAmount + ". Must be greater than 0.");
        }
        return feeAmount;
    }
}