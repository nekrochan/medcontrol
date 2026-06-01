package org.medcontrol.service;

import org.medcontrol.dto.request.IntakeCreateRequestDto;
import org.medcontrol.dto.request.IntakeDetailsRequestDto;
import org.medcontrol.dto.response.IntakeResponseDto;
import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IntakeService {
    void createIntake(IntakeCreateRequestDto dto);

    IntakeResponseDto updateIntakeStatus(String intakeId, String newStatusStr);

    IntakeResponseDto moveIntakeForward(String intakeId);

    IntakeResponseDto updateActualTime(String intakeId, LocalDateTime newTime);

    void deleteBySchemeId(String schemeId);

    List<IntakeResponseDto> getIntakesForProfileAndDate(String profileId, LocalDate date);

    List<IntakeResponseDto> getNearestIntakes(String profileId);

    List<IntakeResponseDto> batchUpdateIntakeStatus(String profileId, IntakeStatus targetStatus);

    List<IntakeResponseDto> batchMoveIntakesForward(String profileId);

    IntakeResponseDto getIntakeById(String intakeId);

    void deleteIntakes(List<Intake> intakes);

    List<Intake> findIntakesBySchemeIdAndStatus(String schemeId, String intakeStatus);

    IntakeDetailsRequestDto getIntakeDetailsWithAvailableStatuses(String intakeId);
}
