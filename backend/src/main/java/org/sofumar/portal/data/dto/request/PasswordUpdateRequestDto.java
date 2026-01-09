package org.sofumar.portal.data.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordUpdateRequestDto {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 5, max = 50)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}