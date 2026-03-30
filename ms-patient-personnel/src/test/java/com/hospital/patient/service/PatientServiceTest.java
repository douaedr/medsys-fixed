package com.hospital.patient.service;

import com.hospital.patient.dto.PatientRequestDTO;
import com.hospital.patient.dto.PatientResponseDTO;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.enums.Sexe;
import com.hospital.patient.exception.PatientAlreadyExistsException;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.mapper.PatientMapper;
import com.hospital.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests PatientService")
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientMapper patientMapper;

    @InjectMocks private PatientService patientService;

    private Patient patient;
    private PatientResponseDTO responseDTO;
    private PatientRequestDTO requestDTO;

    @BeforeEach
    void setup() {
        patient = new Patient();
        patient.setId(1L);
        patient.setNom("Alami");
        patient.setPrenom("Hassan");
        patient.setCin("AB123456");
        patient.setDateNaissance(LocalDate.of(1990, 5, 15));
        patient.setSexe(Sexe.MASCULIN);

        responseDTO = PatientResponseDTO.builder()
                .id(1L)
                .nom("Alami")
                .prenom("Hassan")
                .cin("AB123456")
                .build();

        requestDTO = PatientRequestDTO.builder()
                .nom("Alami")
                .prenom("Hassan")
                .cin("AB123456")
                .dateNaissance(LocalDate.of(1990, 5, 15))
                .sexe(Sexe.MASCULIN)
                .build();
    }

    // ── getPatientById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPatientById retourne le patient si trouvé")
    void getPatientById_found() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        PatientResponseDTO result = patientService.getPatientById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("Alami");
        assertThat(result.getCin()).isEqualTo("AB123456");
    }

    @Test
    @DisplayName("getPatientById lève une exception si patient introuvable")
    void getPatientById_notFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(99L))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ── getPatientByCin ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPatientByCin retourne le patient si CIN trouvé")
    void getPatientByCin_found() {
        when(patientRepository.findByCin("AB123456")).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        PatientResponseDTO result = patientService.getPatientByCin("AB123456");

        assertThat(result.getCin()).isEqualTo("AB123456");
    }

    @Test
    @DisplayName("getPatientByCin lève une exception si CIN introuvable")
    void getPatientByCin_notFound() {
        when(patientRepository.findByCin("INCONNU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientByCin("INCONNU"))
                .isInstanceOf(PatientNotFoundException.class);
    }

    // ── deletePatient ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePatient supprime un patient existant")
    void deletePatient_success() {
        when(patientRepository.existsById(1L)).thenReturn(true);

        patientService.deletePatient(1L);

        verify(patientRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deletePatient lève une exception si patient introuvable")
    void deletePatient_notFound() {
        when(patientRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> patientService.deletePatient(99L))
                .isInstanceOf(PatientNotFoundException.class);

        verify(patientRepository, never()).deleteById(any());
    }

    // ── createPatient ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPatient échoue si CIN déjà existant")
    void createPatient_duplicateCin_throws() {
        when(patientRepository.existsByCin("AB123456")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(requestDTO))
                .isInstanceOf(PatientAlreadyExistsException.class)
                .hasMessageContaining("AB123456");
    }

    // ── getAllPatients ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllPatients retourne une page de patients")
    void getAllPatients_returnPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Patient> patientPage = new PageImpl<>(List.of(patient));

        when(patientRepository.findAll(pageable)).thenReturn(patientPage);
        when(patientMapper.toResponseDTO(patient)).thenReturn(responseDTO);

        Page<PatientResponseDTO> result = patientService.getAllPatients(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getNom()).isEqualTo("Alami");
    }
}
