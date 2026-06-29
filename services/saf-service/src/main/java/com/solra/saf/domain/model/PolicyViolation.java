package com.solra.saf.domain.model;

/**
 * Value object — a policy violation detected during content review.
 */
public class PolicyViolation {

    public enum Category {
        ILLEGAL, NSFW, HATE_SPEECH, HARASSMENT, SPAM,
        FRAUD, SELF_HARM, VIOLENCE, MINOR_SAFETY, PERSONAL_INFO
    }

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    private String policyId;
    private String policyName;
    private Category category;
    private Severity severity;
    private String description;
    private String evidence;

    private PolicyViolation() {}

    public static PolicyViolation detected(String policyId, String policyName,
                                            Category category, Severity severity,
                                            String description, String evidence) {
        PolicyViolation v = new PolicyViolation();
        v.policyId = policyId;
        v.policyName = policyName;
        v.category = category;
        v.severity = severity;
        v.description = description;
        v.evidence = evidence;
        return v;
    }

    public Category getCategory() { return category; }
    public Severity getSeverity() { return severity; }
    public String getPolicyId() { return policyId; }
    public String getPolicyName() { return policyName; }
    public String getDescription() { return description; }
    public String getEvidence() { return evidence; }
}
