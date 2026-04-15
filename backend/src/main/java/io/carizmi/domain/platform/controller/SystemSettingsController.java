package io.carizmi.domain.platform.controller;

import java.util.List;

import io.carizmi.domain.platform.data.dto.SystemSettingsDto;
import io.carizmi.domain.platform.data.dto.request.SystemSettingsSearchRequestDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.infrastructure.security.annotation.IsAdmin;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import static io.carizmi.shared.message.ValidationMessages.*;

@RestController
@RequestMapping("/system-settings")
@Tag(name = "System Settings", description = "System Settings APIs")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSetting systemSetting;

    @GetMapping("/get/{id}")
    @Operation(summary = "Get system setting by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<SystemSettingsDto>> getSystemSetting(@PathVariable Integer id) {
        return ResponseUtils.okWithData(systemSetting.getSystemSetting(id));
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing system setting")
    @IsAdmin
    public ResponseEntity<GlobalResponse<Void>> updateSystemSetting(@Valid @RequestBody SystemSettingsDto dto) {
        systemSetting.updateSystemSetting(dto);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("System Setting").getMessageString());
    }

    @PostMapping("/search")
    @Operation(summary = "Search system settings")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> searchSystemSettings(
            @RequestBody SystemSettingsSearchRequestDto request) {
        PagedResult<SystemSettingsDto> result = systemSetting.searchSystemSettings(request);
        return ResponseUtils.okWithDataPageable(result.items(), result.meta());
    }

    @GetMapping("/by-key/{key}")
    @Operation(summary = "Get settings by key (e.g. Fee, Payment)")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> getSettingsByKey(@PathVariable String key) {
        return ResponseUtils.okWithData(systemSetting.getSettingsByKey(key));
    }
}