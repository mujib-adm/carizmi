package org.sofumar.portal.core.businesslogic.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.data.dto.request.SystemSettingsSearchRequestDto;
import org.sofumar.portal.data.transformer.SystemSettingsDtoTransformer;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.core.businesslogic.SystemSetting;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.repo.SystemSettingRepository;
import org.sofumar.portal.core.repo.jpaspec.SystemSettingsSpecifications;
import org.sofumar.portal.service.validation.SystemSettingsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;

@Service
public non-sealed class SystemSettingImpl extends SystemSettingAbstractBL
        implements SystemSetting {
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
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateSystemSetting(SystemSettingsDto dto) {
        SystemSettingsVO existing = getRepo().findById(dto.getSystemSettingsID())
                .orElseThrow(() -> new RecordNotFoundException(
                        "System Setting not found with ID: " + dto.getSystemSettingsID()));

        // UI only allows updating settingValue. 
        // We preserve settingName and settingKey to ensure integrity with sync mechanism.
        existing.setSettingValue(dto.getSettingValue());

        validator.validateForUpdate(existing);
        update(existing);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("System Setting").getMessageString());
    }


    @Override
    public ResponseEntity<GlobalResponse<SystemSettingsDto>> getSystemSetting(Integer id) {
        SystemSettingsVO vo = getRepo().findById(id)
                .orElseThrow(() -> new RecordNotFoundException("System Setting not found with ID: " + id));
        return ResponseUtils.okWithData(dtoTransformer.transform(vo));
    }

    @Override
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> searchSystemSettings(SystemSettingsSearchRequestDto request) {
        List<Specification<SystemSettingsVO>> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(request.getSettingName()))
            specList.add(SystemSettingsSpecifications.hasSettingName(request.getSettingName()));

        Specification<SystemSettingsVO> spec = Specification.allOf(specList);

        Page<SystemSettingsVO> pageResult = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(pageResult.toList()), meta);
    }

    @Override
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> getSettingsByKey(String key) {
        List<SystemSettingsVO> list = getRepo().findAll(SystemSettingsSpecifications.withSettingKey(key));
        return ResponseUtils.okWithData(dtoTransformer.transformList(list));
    }

    @Override
    public Optional<SystemSettingsVO> findBySettingKey(String key) {
        return getRepo().findOne(SystemSettingsSpecifications.withSettingKey(key));
    }

    @Override
    public Optional<SystemSettingsVO> findByNameAndKey(String settingName, String key) {
        Specification<SystemSettingsVO> spec = SystemSettingsSpecifications.withSettingName(settingName)
                .and(SystemSettingsSpecifications.withSettingKey(key));
        return getRepo().findOne(spec);
    }
}