package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.SystemSettingsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.service.businesslogic.SystemSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system-settings")
@Tag(name = "System Settings", description = "System Settings APIs")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService settingsService;

    @PostMapping("/add")
    @Operation(summary = "Add a new system setting")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> addSystemSetting(@RequestBody SystemSettingsDto dto) {
        return settingsService.addSystemSetting(dto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing system setting")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> updateSystemSetting(@RequestBody SystemSettingsDto dto) {
        return settingsService.updateSystemSetting(dto);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Delete system setting by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> deleteSystemSetting(@PathVariable Integer id) {
        return settingsService.deleteSystemSetting(id);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "Get system setting by ID")
    public ResponseEntity<GlobalResponse<SystemSettingsDto>> getSystemSetting(@PathVariable Integer id) {
        return settingsService.getSystemSetting(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search system settings")
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> searchSystemSettings(
            @RequestParam(required = false) String settingType,
            @RequestParam(required = false) String settingKey,
            @RequestParam(required = false) String settingValue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return settingsService.searchSystemSettings(settingType, settingKey, settingValue, page, size, sortField,
                sortOrder);
    }

    @GetMapping("/by-key/{key}")
    @Operation(summary = "Get settings by key (e.g. Fee, Payment)")
    public ResponseEntity<GlobalResponse<List<SystemSettingsDto>>> getSettingsByKey(@PathVariable String key) {
        return settingsService.getSettingsByKey(key);
    }
}