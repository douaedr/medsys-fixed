package com.hospital.patient.service;

import com.hospital.patient.dto.PatientRequestDTO;
import com.hospital.patient.dto.PatientResponseDTO;
import com.hospital.patient.entity.Patient;
import com.hospital.patient.enums.GroupeSanguin;
import com.hospital.patient.enums.Sexe;
import com.hospital.patient.exception.PatientAlreadyExistsException;
import com.hospital.patient.exception.PatientNotFoundException;
import com.hospital.patient.mapper.PatientMapper;
import com.hospital.patient.repository.DossierMedicalRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private DossierMedicalRepository dossierRepository;
    @Mock private PatientMapper patientMapper;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;
    private PatientResponseDTO patientDTO;

    @BeforeEach
    void setup() {
        patient = Patient.builder()
                .id(1L)
                .nom("Alami")
                .prenom("Fatima")
                .cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .sexe(Sexe.FEMININ)
                .groupeSanguin(GroupeSanguin.A_POSITIF)
                .telephone("0612345678")
                .email("fatima@medsys.ma")
                .build();

        patientDTO = PatientResponseDTO.builder()
                .id(1L)
                .nom("Alami")
                .prenom("Fatima")
                .cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .sexe(Sexe.FEMININ)
                .groupeSanguin(GroupeSanguin.A_POSITIF)
                .telephone("0612345678")
                .email("fatima@medsys.ma")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getPatientById() - patient existant → DTO retourné")
    void getPatientById_found() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDTO(patient)).thenReturn(patientDTO);

        PatientResponseDTO result = patientService.getPatientById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getNom()).isEqualTo("Alami");
        assertThat(result.getCin()).isEqualTo("AB123456");
    }

    @Test
    @DisplayName("getPatientById() - patient inexistant → PatientNotFoundException")
    void getPatientById_notFound() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(999L))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    @DisplayName("getPatientByCin() - CIN existant → DTO retourné")
    void getPatientByCin_found() {
        when(patientRepository.findByCin("AB123456")).thenReturn(Optional.of(patient));
        when(patientMapper.toResponseDTO(patient)).thenReturn(patientDTO);

        PatientResponseDTO result = patientService.getPatientByCin("AB123456");

        assertThat(result).isNotNull();
        assertThat(result.getCin()).isEqualTo("AB123456");
    }

    @Test
    @DisplayName("createPatient() - nouveau patient → sauvegardé")
    void createPatient_success() {
        PatientRequestDTO req = PatientRequestDTO.builder()
                .nom("Alami").prenom("Fatima").cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .build();

        when(patientRepository.existsByCin("AB123456")).thenReturn(false);
        when(patientMapper.toEntity(req)).thenReturn(patient);
        when(patientRepository.save(any())).thenReturn(patient);
        when(patientMapper.toResponseDTO(patient)).thenReturn(patientDTO);

        PatientResponseDTO result = patientService.createPatient(req);

        assertThat(result).isNotNull();
        verify(patientRepository).save(any());
    }

    @Test
    @DisplayName("createPatient() - CIN déjà existant → PatientAlreadyExistsException")
    void createPatient_duplicateCin() {
        PatientRequestDTO req = PatientRequestDTO.builder()
                .nom("Alami").prenom("Fatima").cin("AB123456")
                .dateNaissance(LocalDate.of(1985, 3, 20))
                .build();

        when(patientRepository.existsByCin("AB123456")).thenReturn(true);

        assertThatThrownBy(() -> patientService.createPatient(req))
                .isInstanceOf(PatientAlreadyExistsException.class);
    }

    @Test
    @DisplayName("deletePatient() - patient existant → supprimé")
    void deletePatient_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        doNothing().when(patientRepository).delete(patient);

        patientService.deletePatient(1L);

        verify(patientRepository).delete(patient);
    }

    @Test
    @DisplayName("getAllPatients() - pagination → page retournée")
    void getAllPatients_paginated() {
        Page<Patient> patientPage = new PageImpl<>(List.of(patient));
        when(patientRepository.findAll(any(PageRequest.class))).thenReturn(patientPage);
        when(patientMapper.toResponseDTO(any())).thenReturn(patientDTO);

        Page<PatientResponseDTO> result = patientService.getAllPatients(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getNom()).isEqualTo("Alami");
    }
}
