package org.medcontrol.service.impl;

import org.medcontrol.dto.response.IntakeCleanupResponseDto;
import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.repository.IntakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class IntakeCleanupService {

    private static final Logger log = LoggerFactory.getLogger(IntakeCleanupService.class);

    @Autowired
    private IntakeRepository intakeRepository;

    public IntakeCleanupResponseDto cleanupDuplicateIntakes(String schemeId, List<String> newAlarmTimes) {
        IntakeCleanupResponseDto response = new IntakeCleanupResponseDto();
        response.setSuccess(true);

        try {
            UUID schemeUuid = UUID.fromString(schemeId);
            List<Intake> allIntakes = intakeRepository.findBySchemeId(schemeUuid);

            if (allIntakes.isEmpty()) {
                response.setDeletedCount(0);
                return response;
            }

            Set<LocalTime> newTimesSet = parseAlarmTimes(newAlarmTimes);
            int markedCount = processIntakes(allIntakes, newTimesSet);

            if (markedCount > 0) {
                intakeRepository.saveAllAndFlush(allIntakes);
                log.info("Помечено статусом DELETING {} приемов для схемы {}", markedCount, schemeId);
            }

            response.setDeletedCount(markedCount);

        } catch (Exception e) {
            log.error("Ошибка при очистке приемов: ", e);
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        }

        return response;
    }

    private Set<LocalTime> parseAlarmTimes(List<String> newAlarmTimes) {
        return newAlarmTimes.stream()
                .map(timeStr -> {
                    if (timeStr.length() == 5) {
                        return LocalTime.parse(timeStr + ":00");
                    }
                    return LocalTime.parse(timeStr);
                })
                .collect(Collectors.toSet());
    }

    private int processIntakes(List<Intake> allIntakes, Set<LocalTime> newTimesSet) {
        Map<LocalDate, List<Intake>> intakesByDate = allIntakes.stream()
                .filter(intake -> intake.getScheduledAt() != null)
                .collect(Collectors.groupingBy(
                        intake -> intake.getScheduledAt().toLocalDate()
                ));

        int markedCount = 0;

        for (Map.Entry<LocalDate, List<Intake>> entry : intakesByDate.entrySet()) {
            List<Intake> intakesOnDate = entry.getValue();

            if (intakesOnDate.size() > newTimesSet.size()) {
                markedCount += markDuplicateIntakes(intakesOnDate, newTimesSet);
            }
        }

        return markedCount;
    }

    private int markDuplicateIntakes(List<Intake> intakesOnDate, Set<LocalTime> newTimesSet) {
        int markedCount = 0;

        intakesOnDate.sort(Comparator.comparing(i -> i.getScheduledAt().toLocalTime()));

        List<Intake> toMarkForDeletion = findIntakesToDelete(intakesOnDate, newTimesSet);

        int expectedCount = newTimesSet.size();
        long remainingAfterMarking = intakesOnDate.size() - toMarkForDeletion.size();

        if (remainingAfterMarking > expectedCount) {
            addExcessIntakesToDelete(intakesOnDate, toMarkForDeletion, expectedCount);
        }

        for (Intake intake : toMarkForDeletion) {
            if (intake.getIntakeStatus() != IntakeStatus.TAKEN &&
                    intake.getIntakeStatus() != IntakeStatus.CANCELLED) {
                intake.setIntakeStatus(IntakeStatus.DELETING);
                markedCount++;
            }
        }

        return markedCount;
    }

    private List<Intake> findIntakesToDelete(List<Intake> intakesOnDate, Set<LocalTime> newTimesSet) {
        List<Intake> toMarkForDeletion = new ArrayList<>();

        for (Intake intake : intakesOnDate) {
            LocalTime intakeTime = intake.getScheduledAt().toLocalTime();

            boolean shouldExist = newTimesSet.contains(intakeTime);
            boolean shouldExistRounded = newTimesSet.stream()
                    .anyMatch(newTime -> Math.abs(newTime.toSecondOfDay() - intakeTime.toSecondOfDay()) < 60);

            if (!shouldExist && !shouldExistRounded) {
                toMarkForDeletion.add(intake);
            }
        }

        return toMarkForDeletion;
    }

    private void addExcessIntakesToDelete(List<Intake> allIntakes, List<Intake> alreadyMarked, int expectedCount) {
        List<Intake> notMarkedYet = allIntakes.stream()
                .filter(i -> !alreadyMarked.contains(i))
                .sorted(Comparator.comparing(i -> i.getScheduledAt().toLocalTime()))
                .collect(Collectors.toList());

        int excess = notMarkedYet.size() - expectedCount;
        for (int i = 0; i < excess; i++) {
            alreadyMarked.add(notMarkedYet.get(notMarkedYet.size() - 1 - i));
        }
    }
}