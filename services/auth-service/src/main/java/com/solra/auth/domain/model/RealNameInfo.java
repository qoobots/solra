package com.solra.auth.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Value object — real-name verification information (AUTH-004).
 * Required for compliance with Chinese real-name authentication regulations.
 */
public class RealNameInfo {

    private String realName;
    private String idNumber;      // encrypted identity card number
    private LocalDate birthDate;
    private boolean verified;
    private Instant verifiedAt;

    private RealNameInfo() {}

    /**
     * Create from raw data (idNumber should be encrypted before storage).
     */
    public static RealNameInfo create(String realName, String encryptedIdNumber, LocalDate birthDate) {
        RealNameInfo info = new RealNameInfo();
        info.realName = Objects.requireNonNull(realName, "realName must not be null");
        info.idNumber = Objects.requireNonNull(encryptedIdNumber, "idNumber must not be null");
        info.birthDate = Objects.requireNonNull(birthDate, "birthDate must not be null");
        info.verified = false;
        return info;
    }

    /**
     * Create a copy with verification status changed.
     */
    public RealNameInfo withVerified(boolean verified) {
        RealNameInfo copy = new RealNameInfo();
        copy.realName = this.realName;
        copy.idNumber = this.idNumber;
        copy.birthDate = this.birthDate;
        copy.verified = verified;
        copy.verifiedAt = verified ? Instant.now() : null;
        return copy;
    }

    /**
     * Check if this person is a minor (under 18) — for minor protection compliance.
     */
    public boolean isMinor() {
        if (birthDate == null) return false;
        return Period.between(birthDate, LocalDate.now()).getYears() < 18;
    }

    /**
     * Calculate age from birth date.
     */
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Validate that required fields are present and birthdate is reasonable.
     */
    public boolean isValid() {
        return realName != null && !realName.isBlank()
                && idNumber != null && !idNumber.isBlank()
                && birthDate != null
                && birthDate.isBefore(LocalDate.now())
                && Period.between(birthDate, LocalDate.now()).getYears() < 150;
    }

    public String getRealName() { return realName; }
    public String getIdNumber() { return idNumber; }
    public LocalDate getBirthDate() { return birthDate; }
    public boolean isVerified() { return verified; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
