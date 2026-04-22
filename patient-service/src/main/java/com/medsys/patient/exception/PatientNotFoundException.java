package com.medsys.patient.exception;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(Long id) {
        super("Patient non trouvé: id=" + id);
    }
    public PatientNotFoundException(String msg) {
        super(msg);
    }
}
