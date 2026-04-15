package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.platform.repository.SystemSettingRepository;

@DomainLogicFor(SystemSettingsVO.class)
public abstract sealed class SystemSettingAbstractBL extends AbstractBusinessLogic<SystemSettingsVO, SystemSettingRepository> permits SystemSettingImpl {
    protected SystemSettingAbstractBL(SystemSettingRepository repo) {
        super(repo);
    }
}