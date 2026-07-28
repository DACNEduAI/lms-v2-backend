package edu.ai.lms.core.material.api;

import java.util.UUID;

public record MaterialEligibility(UUID materialId, boolean eligible, String reason) {
}
