package edu.ai.lms.core.learnermodel.app.domain;

public enum LearnerModelType {
    DKT_CUSTOM("dkt-custom", false, true),
    DKT_PYKT("dkt-pykt", false, true),
    AKT_CUSTOM("akt-custom", true, false),
    AKT_PYKT("akt-pykt", true, true);

    private final String wireName;
    private final boolean akt;
    private final boolean zeroBased;

    LearnerModelType(String wireName, boolean akt, boolean zeroBased) {
        this.wireName = wireName;
        this.akt = akt;
        this.zeroBased = zeroBased;
    }

    public String wireName() {
        return wireName;
    }

    public boolean isAkt() {
        return akt;
    }

    public long toModelId(long canonicalId) {
        return zeroBased ? canonicalId - 1 : canonicalId;
    }

    public long toCanonicalId(long modelId) {
        return zeroBased ? modelId + 1 : modelId;
    }
}
