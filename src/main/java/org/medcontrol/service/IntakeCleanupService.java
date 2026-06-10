package org.medcontrol.service;

import org.medcontrol.dto.response.IntakeCleanupResponseDto;

import java.util.List;

public interface IntakeCleanupService {
    IntakeCleanupResponseDto cleanupDuplicateIntakes(String schemeId, List<String> newAlarmTimes);
}
