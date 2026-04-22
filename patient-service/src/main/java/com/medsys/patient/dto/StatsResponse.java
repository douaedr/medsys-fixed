package com.medsys.patient.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StatsResponse {
    private long totalPatients;
    private long nouveauxCeMois;
    private Map<String, Long> parSexe;
    private Map<String, Long> parGroupeSanguin;
    private Map<String, Long> parVille;
}
