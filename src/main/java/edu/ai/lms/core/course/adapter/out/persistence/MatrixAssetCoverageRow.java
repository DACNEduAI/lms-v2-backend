package edu.ai.lms.core.course.adapter.out.persistence;

import java.util.UUID;

public record MatrixAssetCoverageRow(UUID customLearningLevelId, String assetType, int percentage) { }
