package com.hospital.auth.dto;
import com.hospital.auth.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class CreatePersonnelRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 8, message = "Minimum 8 caractères") private String password;
    @NotBlank @Size(min = 2, max = 100) private String nom;
    @NotBlank @Size(min = 2, max = 100) private String prenom;
    @Size(min = 6, max = 20)
    @Pattern(regexp = "^[A-Za-z0-9]*$", message = "CIN ne doit contenir que des lettres et chiffres")
    private String cin;
    private Role role;
    private Long personnelId;
}
