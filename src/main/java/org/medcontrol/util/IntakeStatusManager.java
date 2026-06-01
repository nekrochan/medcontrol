package org.medcontrol.util;

import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.repository.IntakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class IntakeStatusManager {

    private final IntakeRepository intakeRepository;
    private static final Logger log = LoggerFactory.getLogger(IntakeStatusManager.class);


    private final int nearestHalfRange;
    private final int minutesToMove;

    public IntakeStatusManager(IntakeRepository intakeRepository, IntakeHelper intakeHelper) {
        this.intakeRepository = intakeRepository;
        this.nearestHalfRange = intakeHelper.getNearestHalfRange();
        this.minutesToMove = intakeHelper.getMinutesToMove();
    }

    public IntakeStatus autoCancelNewIntake(LocalDateTime dateTime) {
        LocalDateTime nearestFloor = LocalDateTime.now().minusMinutes(nearestHalfRange);
        if (dateTime.isBefore(nearestFloor)) {
            return IntakeStatus.CANCELLED;
        } else {
            return IntakeStatus.SCHEDULED;
        }
    }

    public boolean isAllowed(Intake intake, IntakeStatus targetStatus, LocalDateTime nearestFloor) {
        IntakeStatus currentStatus = intake.getIntakeStatus();
        SchemeStatus schemeStatus = intake.getScheme().getStatus();
        boolean isScheduledBeforeNearest = intake.getScheduledAt().isBefore(nearestFloor);
        boolean hasTakenAt = (intake.getTakenAt() == null);

        if (currentStatus == targetStatus) return false;
        if (currentStatus == IntakeStatus.PAUSED) return false;

        switch (currentStatus) {
            case SCHEDULED -> {
                return targetStatus != IntakeStatus.PAUSED || schemeStatus == SchemeStatus.PAUSED;
            }
            case TAKEN -> {
                if (targetStatus == IntakeStatus.CANCELLED) return true;
                if (targetStatus == IntakeStatus.SCHEDULED && !isScheduledBeforeNearest) return true;
                return false;
            }
            case MOVED -> {
                if (targetStatus == IntakeStatus.TAKEN && hasTakenAt && isScheduledBeforeNearest) return true;
                if (targetStatus == IntakeStatus.CANCELLED && !hasTakenAt && isScheduledBeforeNearest) return true;
                if (targetStatus == IntakeStatus.PAUSED && schemeStatus == SchemeStatus.PAUSED) return true;
                return false;
            }
            case CANCELLED -> {
                if (targetStatus == IntakeStatus.TAKEN) return true;
                if (targetStatus == IntakeStatus.SCHEDULED && !isScheduledBeforeNearest) return true;
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    public void manageMovedIntake(Intake intake, LocalDateTime nearestFloor, LocalDateTime now) throws IllegalArgumentException {
        if (intake.getIntakeStatus() != IntakeStatus.MOVED)
            throw new IllegalArgumentException("Метод manageMovedIntake применим только к приемам в статусе MOVED");

        if (now.isAfter(nearestFloor)) {
            if (intake.getTakenAt() == null) {
                cancelIntake(intake, nearestFloor, now);
                log.info("Отмена перенесенного приема вызвана из метода manageMovedIntake");
            }
            else {
                performIntake(intake,nearestFloor,now.plusMinutes(nearestHalfRange), now);
                log.info("Выполнение перенесенного приема вызвано из метода manageMovedIntake");
            }
        }
        else {
            throw new IllegalArgumentException("Прием не удовлетворяет условию: now.isAfter(nearestFloor)");
        }
        intakeRepository.saveAndFlush(intake);
    }

    public void updateIntakeStatusManually(
            Intake intake, IntakeStatus targetStatus, LocalDateTime nearestFloor,
            LocalDateTime nearestCeil, LocalDateTime now) throws IllegalArgumentException {
        IntakeStatus currentStatus = intake.getIntakeStatus();

        if (!isAllowed(intake, targetStatus, nearestFloor))
            throw new IllegalArgumentException("Нельзя изменить статус для приема " + intake.getId().toString() +
                    ". Исходный статус: " + currentStatus.toString()
                    + ", назначаемый статус: " + targetStatus.toString());

        switch (targetStatus) {
            case SCHEDULED -> {
                try {
                    scheduleIntake(intake, nearestFloor);
                } catch (IllegalArgumentException e) {
                    log.info("Не удалось обновить статус: {}", e.getMessage());
                }
            }
            case TAKEN -> {
                performIntake(intake, nearestFloor, nearestCeil, now);
            }
            case MOVED -> {
                try {
                    moveIntake(intake, now, nearestCeil);
                } catch (IllegalArgumentException e) {
                    log.info("Не удалось обновить статус: {}", e.getMessage());
                }
            }
            case CANCELLED -> {
                try {
                    cancelIntake(intake, nearestFloor, now);
                } catch (IllegalArgumentException e) {
                    log.info("Не удалось обновить статус: {}", e.getMessage());
                }
            }
            case PAUSED -> {
                try {
                    pauseIntake(intake);
                } catch (IllegalArgumentException e) {
                    log.info("Не удалось обновить статус: {}", e.getMessage());
                }
            }
            default -> throw new IllegalArgumentException("Статус не распознан: " + currentStatus.toString());
        }
    }

    public void autoFinalizeIntakeStatus(
            Intake intake, LocalDateTime nearestFloor, LocalDateTime now
    ) {
        IntakeStatus currentStatus = intake.getIntakeStatus();

        if (currentStatus == IntakeStatus.MOVED) {
            try {
                manageMovedIntake(intake, nearestFloor, now);
                log.info(
                        "Автозавершение приема {} в статусе MOVED, вызов метода manageMovedIntake",
                        intake.getId().toString()
                );
            } catch (IllegalArgumentException e) {
                log.info("Не удалось завершить прием автоматически: {}", e.getMessage());
            }
        } else if (currentStatus == IntakeStatus.SCHEDULED && now.isAfter(nearestFloor)) {
            try {
                cancelIntake(intake, nearestFloor, now);
                log.info("Автозавершение приема {} в статусе SCHEDULED, вызов метода cancelIntake",
                        intake.getId().toString());
            } catch (IllegalArgumentException e) {
                log.info("Не удалось завершить прием автоматически: {}", e.getMessage());
            }
        } else throw new IllegalArgumentException(
                "Метод autoFinalizeIntakeStatus применим только к приемам в статусе MOVED и SCHEDULED");

    }

    private void scheduleIntake(Intake intake, LocalDateTime nearestFloor) throws IllegalArgumentException {
        IntakeStatus currentStatus = intake.getIntakeStatus();

        if (currentStatus == IntakeStatus.CANCELLED || currentStatus == IntakeStatus.TAKEN) {
            if (intake.getScheduledAt().isBefore(nearestFloor))
                throw new IllegalArgumentException("Нельзя назначить статус SCHEDULED, если прием не ближайший");

            intake.setTakenAt(null);
            intake.setIntakeStatus(IntakeStatus.SCHEDULED);
            intakeRepository.saveAndFlush(intake);
            log.info("Прием {} в статусе {} помечен как запланированный в {}", intake.getId().toString(), currentStatus.toString(), intake.getScheduledAt());

        } else throw new IllegalArgumentException("Запрещенный переход: из " + currentStatus.toString() + " в SCHEDULED");
    }

    private void performIntake(Intake intake, LocalDateTime nearestFloor, LocalDateTime nearestCeil, LocalDateTime now) throws IllegalArgumentException {
        IntakeStatus currentStatus = intake.getIntakeStatus();
        boolean isNearest = intake.getScheduledAt().isAfter(nearestFloor) 
                && intake.getScheduledAt().isBefore(nearestCeil);

        switch (currentStatus) {
            case CANCELLED -> {
                if (!isNearest) {
                    intake.setTakenAt(intake.getScheduledAt());
                }
                else {
                    intake.setTakenAt(now);
                }
            }
            case SCHEDULED -> {
                if (isNearest) {
                    intake.setTakenAt(now);
                }
                else intake.setTakenAt(intake.getScheduledAt());
            }
            case MOVED -> {
                intake.setTakenAt(now);
            }
            default -> throw new IllegalArgumentException(
                    "Нельзя перевести прием из статуса " + intake.getIntakeStatus() + " в TAKEN"
            );
        }

        log.info("Прием {} в статусе {} помечен как выполненный в {}", intake.getId().toString(), currentStatus.toString(), intake.getTakenAt());

        intakeRepository.saveAndFlush(intake);
    }

    private void moveIntake(
            Intake intake, LocalDateTime now, LocalDateTime nearestCeil
    ) throws IllegalArgumentException {
        if (now.isAfter(nearestCeil)) throw new IllegalArgumentException(
                "Перенести можно только ближайший прием"
        );
        if (intake.getIntakeStatus() == IntakeStatus.SCHEDULED) {
            LocalDateTime oldTime = intake.getScheduledAt();
            LocalDateTime newTime = oldTime.plusMinutes(minutesToMove);
            intake.setIntakeStatus(IntakeStatus.MOVED);
            intake.setScheduledAt(newTime);
            intakeRepository.saveAndFlush(intake);
            log.info("Статус {} перенесен с {} на {}", intake.getId(), oldTime, newTime);
        }
        else throw new IllegalArgumentException(
                "Нельзя перенести прием в статусе " + intake.getIntakeStatus().toString()
        );
    }

    private void cancelIntake(Intake intake, LocalDateTime nearestFloor, LocalDateTime now) throws IllegalArgumentException {
        IntakeStatus currentStatus = intake.getIntakeStatus();

        switch (currentStatus) {
            case TAKEN, SCHEDULED, MOVED -> {
                intake.setTakenAt(null);
                intake.setIntakeStatus(IntakeStatus.CANCELLED);
                intakeRepository.saveAndFlush(intake);
                log.info("Прием {} в статусе {} отменен", intake.getId().toString(), currentStatus.toString());
            }
            default -> throw new IllegalArgumentException(
                    "Нельзя отменить прием в статусе " + intake.getIntakeStatus().toString()
            );
        }
    }

    private void pauseIntake(Intake intake) throws IllegalArgumentException {
        IntakeStatus currentStatus = intake.getIntakeStatus();
        if (currentStatus == IntakeStatus.SCHEDULED || currentStatus == IntakeStatus.MOVED) {
            intake.setIntakeStatus(IntakeStatus.PAUSED);
            intakeRepository.saveAndFlush(intake);
            log.info("Прием {} в статусе {} приостановлен", intake.getId().toString(), currentStatus.toString());
        }
        else throw new IllegalArgumentException(
                "Нельзя приостановить прием в статусе " + intake.getIntakeStatus().toString()
        );
    }
}
