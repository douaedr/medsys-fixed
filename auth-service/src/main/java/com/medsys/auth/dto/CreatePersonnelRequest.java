package com.medsys.auth.dto;

import com.medsys.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePersonnelRequest {
    @NotBlank private String nom;
    @NotBlank private String prenom;
    @NotBlank @Email private String email;
    @NotBlank private String password;
    private String cin;
    @NotNull  private Role role;
    private Long personnelId;
}
