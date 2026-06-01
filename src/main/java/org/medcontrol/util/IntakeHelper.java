package org.medcontrol.util;

import org.medcontrol.dto.response.IntakeResponseDto;
import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.repository.IntakeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class IntakeHelper {

    private final int minutesToMove = 1;
    private final int nearestHalfRange = 3;

    private IntakeRepository intakeRepository;

    public IntakeHelper(IntakeRepository intakeRepository) {
        this.intakeRepository = intakeRepository;
    }

    public IntakeResponseDto entityToResponseDto(Intake intake) {
        IntakeResponseDto dto = new IntakeResponseDto();

        dto.setId(intake.getId().toString());
        dto.setSchemeId(intake.getScheme().getId().toString());
        dto.setIntakeStatus(intake.getIntakeStatus().toString());
        dto.setScheduledAt(intake.getScheduledAt());
        dto.setTakenAt(intake.getTakenAt());
        dto.setDescription(intake.getDescription());
        return dto;
    }

    public int getMinutesToMove() {
        return minutesToMove;
    }

    public int getNearestHalfRange() {
        return nearestHalfRange;
    }

    public IntakeStatus getIntakeStatusFromString(String status) {
        if (status.equalsIgnoreCase(IntakeStatus.TAKEN.toString()))
            return IntakeStatus.TAKEN;
        else if (status.equalsIgnoreCase(IntakeStatus.CANCELLED.toString())) {
            return IntakeStatus.CANCELLED;
        } else if (status.equalsIgnoreCase(IntakeStatus.SCHEDULED.toString())) {
            return IntakeStatus.SCHEDULED;
        } else if (status.equalsIgnoreCase(IntakeStatus.MOVED.toString())) {
            return IntakeStatus.MOVED;
        }  else if (status.equalsIgnoreCase(IntakeStatus.PAUSED.toString())) {
            return IntakeStatus.PAUSED;
        } else throw new IllegalArgumentException("Не удалось определить статус приема");
    }

    public Intake getIntakeOrThrow(String intakeId) {
        return intakeRepository.findById(UUID.fromString(intakeId))
                .orElseThrow(() -> new IllegalArgumentException("Прием не найден"));
    }

    public List<IntakeStatus> getAvailableStatuses(Intake intake, LocalDateTime nearestFloor, LocalDateTime nearestCeil) {
        List<IntakeStatus> availableStatuses = new ArrayList<>();

        if (intake.getScheduledAt().isBefore(nearestFloor)) {
            availableStatuses.add(IntakeStatus.TAKEN);
            availableStatuses.add(IntakeStatus.CANCELLED);
        } else if (intake.getScheduledAt().isBefore(nearestCeil)) {
            availableStatuses.add(IntakeStatus.TAKEN);
            availableStatuses.add(IntakeStatus.CANCELLED);
            availableStatuses.add(IntakeStatus.SCHEDULED);
            availableStatuses.add(IntakeStatus.MOVED);
        } else if (intake.getIntakeStatus().equals(IntakeStatus.MOVED)) {
            availableStatuses.add(IntakeStatus.MOVED);
        } else {
            availableStatuses.add(IntakeStatus.TAKEN);
            availableStatuses.add(IntakeStatus.CANCELLED);
            availableStatuses.add(IntakeStatus.SCHEDULED);
        }

        return availableStatuses;
    }

}
