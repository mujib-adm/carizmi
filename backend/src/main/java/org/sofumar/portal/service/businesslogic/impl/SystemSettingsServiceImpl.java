package org.sofumar.portal.service.businesslogic.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.data.transformer.SystemSettingsDtoTransformer;
import org.sofumar.portal.data.transformer.SystemSettingsVOTransformer;
import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.util.LabelUtils;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.SystemSettingsRepository;
import org.sofumar.portal.repo.jpaspec.SystemSettingsSpecifications;
import org.sofumar.portal.service.businesslogic.SystemSettingsService;
import org.sofumar.portal.service.validation.SystemSettingsValidator;
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

import static org.sofumar.portal.constants.MessagesConstants.RECORD_ADDED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_DELETED;
import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;
import static org.sofumar.portal.constants.MessagesConstants.REQUIRED_FIELD;

@Service
public class SystemSettingsServiceImpl extends AbstractBusinessLogic<SystemSettingsVO, SystemSettingsRepository>
        implements SystemSettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsServiceImpl.class);

    private final SystemSettingsRepository settingsRepo;
    private final SystemSettingsVOTransformer voTransformer;
    private final SystemSettingsDtoTransformer dtoTransformer;
    private final SystemSettingsValidator validator;

    @Autowired
    public SystemSettingsServiceImpl(SystemSettingsRepository settingsRepo,
            SystemSettingsVOTransformer voTransformer,
            SystemSettingsDtoTransformer dtoTransformer,
            SystemSettingsValidator validator) {
        this.settingsRepo = settingsRepo;
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected SystemSettingsRepository getRepository() {
        return settingsRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> addSystemSetting(SystemSettingsDto dto) {
        SystemSettingsVO vo = voTransformer.transform(dto);
        validator.validate(vo);
        add(vo);
        return ResponseUtils.ok(RECORD_ADDED.addMessageArgs("System Setting").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateSystemSetting(SystemSettingsDto dto) {
        if (dto.getSystemSettingsID() == null) {
            return ResponseUtils.badRequest(REQUIRED_FIELD
                    .addMessageArgs(LabelUtils.toLabel(FieldConstants.SYSTEM_SETTINGS_ID)).getMessageString());
        }

        SystemSettingsVO existing = settingsRepo.findById(dto.getSystemSettingsID())
                .orElseThrow(() -> new RecordNotFoundException(
                        "System Setting not found with ID: " + dto.getSystemSettingsID()));

        SystemSettingsVO updated = voTransformer.transformForUpdate(dto, existing);
        validator.validateForUpdate(updated);
        update(updated);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("System Setting").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> deleteSystemSetting(Integer id) {
        if (id == null) {
            return ResponseUtils.badRequest(REQUIRED_FIELD
                    .addMessageArgs(LabelUtils.toLabel(FieldConstants.SYSTEM_SETTINGS_ID)).getMessageString());
        }
        SystemSettingsVO existing = settingsRepo.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("System Setting not found with ID: " + id));
        delete(existing);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("System Setting").getMessageString());
    }

    @Override
    public ResponseEntity<GlobalResponse<SystemSettingsDto>> getSystemSetting(Integer id) {
        if (id == null) {
            return ResponseUtils.badRequestWithData(REQUIRED_FIELD
                    .addMessageArgs(LabelUtils.toLabel(FieldConstants.SYSTEM_SETTINGS_ID)).getMessageString());
        }
        SystemSettingsVO vo = settingsRepo.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("System Setting not found with ID: " + id));
        return ResponseUtils.okWithData(dtoTransformer.transform(vo));
    }

    @Override
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> searchSystemSettings(String settingType,
            String settingKey, String settingValue, int page, int size, String sortField, String sortOrder) {
        List<Specification<SystemSettingsVO>> specList = new ArrayList<>();
        if (StringUtils.isNotBlank(settingType))
            specList.add(SystemSettingsSpecifications.hasSettingType(settingType));
        if (StringUtils.isNotBlank(settingKey))
            specList.add(SystemSettingsSpecifications.hasSettingKey(settingKey));
        if (StringUtils.isNotBlank(settingValue))
            specList.add(SystemSettingsSpecifications.hasSettingValue(settingValue));

        Specification<SystemSettingsVO> spec = Specification.allOf(specList);

        Sort sort = Sort.unsorted();
        if (sortField != null && sortOrder != null) {
            sort = Sort.by(Sort.Direction.fromString(sortOrder), sortField);
        }
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<SystemSettingsVO> pageResult = settingsRepo.findAll(spec, pageRequest);
        PaginationMeta meta = PaginationMeta.of(pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages());

        return ResponseUtils.okWithDataPageable(dtoTransformer.transformList(pageResult.toList()), meta);
    }

    @Override
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> getSettingsByKey(String key) {
        List<SystemSettingsVO> list = settingsRepo.findBySettingKey(key);
        return ResponseUtils.okWithData(dtoTransformer.transformList(list));
    }
}