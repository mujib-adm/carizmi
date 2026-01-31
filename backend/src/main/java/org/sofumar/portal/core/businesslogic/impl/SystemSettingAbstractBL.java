package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.SystemSettingRepository;

@DomainLogicFor(SystemSettingsVO.class)
public abstract sealed class SystemSettingAbstractBL extends AbstractBusinessLogic<SystemSettingsVO, SystemSettingRepository> permits SystemSettingImpl {
    protected SystemSettingAbstractBL(SystemSettingRepository repo) {
        super(repo);
    }
}