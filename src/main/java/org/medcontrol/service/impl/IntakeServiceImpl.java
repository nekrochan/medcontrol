package org.medcontrol.service.impl;
import jakarta.transaction.Transactional;
import org.medcontrol.dto.request.IntakeDetailsRequestDto;
import org.medcontrol.service.IntakeService;
import org.medcontrol.util.IntakeHelper;
import org.medcontrol.util.IntakeStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.medcontrol.dto.request.IntakeCreateRequestDto;
import org.medcontrol.dto.response.IntakeResponseDto;
import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.entity.Scheme;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.SchemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class IntakeServiceImpl implements IntakeService {

    private static final Logger log = LoggerFactory.getLogger(IntakeServiceImpl.class);
    private final IntakeRepository intakeRepository;
    private final SchemeRepository schemeRepository;
    private final IntakeStatusManager intakeStatusManager;
    private final IntakeHelper intakeHelper;
    private final int nearestHalfRange;

    @Autowired
    public IntakeServiceImpl(
            IntakeRepository intakeRepository,
            SchemeRepository schemeRepository,
            IntakeStatusManager intakeStatusManager,
            IntakeHelper intakeHelper
    ) {
        this.intakeRepository = intakeRepository;
        this.schemeRepository = schemeRepository;
        this.intakeStatusManager = intakeStatusManager;
        this.intakeHelper = intakeHelper;
        this.nearestHalfRange = intakeHelper.getNearestHalfRange();
    }

    @Override
    public void createIntake(IntakeCreateRequestDto dto) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestFloor = now.minusMinutes(nearestHalfRange);
        if (dto.getScheduledAt() == null)
            throw new IllegalArgumentException("Запланированная дата приема не может быть пустой");

        Intake intake = new Intake(
                schemeRepository.findById(UUID.fromString(dto.getSchemeId()))
                        .orElseThrow(() -> new IllegalArgumentException("Схема не найдена"))
        );
        intake.setScheduledAt(dto.getScheduledAt());
        intake.setTakenAt(null);
        if (dto.getScheduledAt().isBefore(nearestFloor))
            intake.setIntakeStatus(IntakeStatus.CANCELLED);
        else intake.setIntakeStatus(IntakeStatus.SCHEDULED);

        Intake saved = intakeRepository.saveAndFlush(intake);
        log.info("Создан прием с ID: {} на время: {}", saved.getId(), saved.getScheduledAt());
    }


    @Override
    public IntakeResponseDto updateIntakeStatus(String intakeId, String newStatusStr) {
        Intake intake = intakeHelper.getIntakeOrThrow(intakeId);
        IntakeStatus currentStatus = intake.getIntakeStatus();
        log.info("Попытка изменения статуса приема {} с {} на {}", intake.getId().toString(), currentStatus.toString(), newStatusStr);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestBottom = now.minusMinutes(nearestHalfRange);
        LocalDateTime nearestCeil = now.plusMinutes(nearestHalfRange);

        IntakeStatus targetStatus;
        try {
            targetStatus = IntakeStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Статус не распознан: " + newStatusStr);
        }

        if (currentStatus == targetStatus) {
            log.info("Попытка изменить статус на идентичный текущему: {}", targetStatus);
            return intakeHelper.entityToResponseDto(intake);

        }

        try {
            intakeStatusManager.updateIntakeStatusManually(intake, targetStatus, nearestBottom, nearestCeil, now);
        } catch (IllegalArgumentException e) {
            log.info("Не удалось изменить статус: {}", e.getMessage());
        }

        intakeRepository.saveAndFlush(intake);
        log.info("Сохранено в репозиторий со статусом: {}", intake.getIntakeStatus());
        return intakeHelper.entityToResponseDto(intake);
    }

    @Override
    public IntakeResponseDto moveIntakeForward(String intakeId) {
        Intake intake = intakeHelper.getIntakeOrThrow(intakeId);
        log.info("Попытка перенести прием {} с {} на {}",
                intake.getId().toString(),
                intake.getScheduledAt().toString(),
                intake.getScheduledAt().plusMinutes(nearestHalfRange).toString());
        IntakeStatus targetStatus = IntakeStatus.MOVED;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestBottom = now.minusMinutes(nearestHalfRange);
        LocalDateTime nearestCeil = now.plusMinutes(nearestHalfRange);
        intakeStatusManager.updateIntakeStatusManually(intake, targetStatus, nearestBottom, nearestCeil, now);
        intakeRepository.saveAndFlush(intake);

        return intakeHelper.entityToResponseDto(intake);
    }

    @Override
    public IntakeResponseDto updateActualTime(String intakeId, LocalDateTime newTime) {
        Intake intake = intakeHelper.getIntakeOrThrow(intakeId);
        intake.setTakenAt(newTime);
        if (intake.getIntakeStatus() != IntakeStatus.TAKEN) {
            intake.setIntakeStatus(IntakeStatus.TAKEN);
        }
        intakeRepository.saveAndFlush(intake);
        return intakeHelper.entityToResponseDto(intake);
    }

    @Override
    public void deleteBySchemeId(String schemeId) {
        intakeRepository.deleteBySchemeId(UUID.fromString(schemeId));
        intakeRepository.flush();
    }

    @Override
    public List<IntakeResponseDto> getIntakesForProfileAndDate(String profileId, LocalDate date) {
        UUID profileUuid = UUID.fromString(profileId);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<Scheme> profileSchemes = schemeRepository.findByProfileId(profileUuid);

        List<Intake> allIntakes = new ArrayList<>();
        for (Scheme scheme : profileSchemes) {
            List<Intake> schemeIntakes = intakeRepository
                    .findBySchemeIdAndScheduledAtBetween(
                            scheme.getId(), startOfDay, endOfDay
                    );
            allIntakes.addAll(schemeIntakes);
        }

        return allIntakes.stream()
                .sorted(Comparator.comparing(Intake::getScheduledAt))
                .map(this::intakeToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<IntakeResponseDto> getNearestIntakes(String profileId) {

        UUID profileUuid = UUID.fromString(profileId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.minusMinutes(nearestHalfRange);
        LocalDateTime endWindow = now.plusMinutes(nearestHalfRange);

        List<Scheme> profileSchemes = schemeRepository.findByProfileId(profileUuid);

        List<Intake> nearestIntakes = new ArrayList<>();
        for (Scheme scheme : profileSchemes) {
            List<Intake> schemeIntakes = intakeRepository
                    .findBySchemeIdAndScheduledAtBetween(
                            scheme.getId(), startWindow, endWindow
                    );
            nearestIntakes.addAll(schemeIntakes);
        }

        return nearestIntakes.stream()
                .sorted(Comparator.comparing(Intake::getScheduledAt))
                .map(this::intakeToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<IntakeResponseDto> batchUpdateIntakeStatus(String profileId, IntakeStatus targetStatus) {
        List<Intake> nearestIntakes = getNearestIntakesEntities(profileId);
        List<IntakeResponseDto> updatedDtos = new ArrayList<>();

        for (Intake intake : nearestIntakes) {
            try {
                IntakeResponseDto dto = updateIntakeStatus(intake.getId().toString(), targetStatus.toString());
                updatedDtos.add(dto);
            } catch (Exception e) {
                log.error("Не удалось обновить прием {}: {}", intake.getId(), e.getMessage());
            }
        }
        return updatedDtos;
    }

    @Override
    public List<IntakeResponseDto> batchMoveIntakesForward(String profileId) {
        List<Intake> nearestIntakes = getNearestIntakesEntities(profileId);
        List<IntakeResponseDto> movedDtos = new ArrayList<>();

        for (Intake intake : nearestIntakes) {
            try {
                IntakeResponseDto dto = moveIntakeForward(intake.getId().toString());
                movedDtos.add(dto);
            } catch (Exception e) {
                log.error("Не удалось перенести прием {}: {}", intake.getId(), e.getMessage());
            }
        }
        return movedDtos;
    }

    private List<Intake> getNearestIntakesEntities(String profileId) {
        UUID profileUuid = UUID.fromString(profileId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.minusMinutes(nearestHalfRange);
        LocalDateTime endWindow = now.plusMinutes(nearestHalfRange);

        List<Scheme> profileSchemes = schemeRepository.findByProfileId(profileUuid);

        List<Intake> nearestIntakes = new ArrayList<>();
        for (Scheme scheme : profileSchemes) {
            List<Intake> schemeIntakes = intakeRepository
                    .findBySchemeIdAndScheduledAtBetween(
                            scheme.getId(), startWindow, endWindow
                    );
            nearestIntakes.addAll(schemeIntakes);
        }

        return nearestIntakes.stream()
                .sorted(Comparator.comparing(Intake::getScheduledAt))
                .collect(Collectors.toList());
    }

    @Override
    public IntakeResponseDto getIntakeById(String intakeId) {
        Intake intake = intakeHelper.getIntakeOrThrow(intakeId);
        return intakeHelper.entityToResponseDto(intake);
    }

    @Override
    public void deleteIntakes(List<Intake> intakes) {
        intakeRepository.deleteAll(intakes);
        intakeRepository.flush();
        log.info("Удалено {} приемов", intakes.size());
    }

    @Override
    public List<Intake> findIntakesBySchemeIdAndStatus(String schemeId, String intakeStatus) {
        IntakeStatus status = intakeHelper.getIntakeStatusFromString(intakeStatus);
        return intakeRepository.findBySchemeIdAndIntakeStatus(UUID.fromString(schemeId), status);
    }

    @Override
    public IntakeDetailsRequestDto getIntakeDetailsWithAvailableStatuses(String intakeId) {
        Intake intake = intakeHelper.getIntakeOrThrow(intakeId);
        IntakeDetailsRequestDto details = new IntakeDetailsRequestDto();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestBottom = now.minusMinutes(nearestHalfRange);
        LocalDateTime nearestCeil = now.plusMinutes(nearestHalfRange);

        List<String> availableStatuses = intakeHelper
                .getAvailableStatuses(intake, nearestBottom, nearestCeil)
                .stream()
                .map(IntakeStatus::name)
                .collect(Collectors.toList());

        details.setAvailableStatuses(availableStatuses);
        return details;
    }

    @Scheduled(fixedDelay = 10000)
    public void autoFinalizeNearestIntakes() {
        log.info("Проверка ближайших приемов (каждые {} секунд)", 10);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestBottom = now.minusMinutes(nearestHalfRange);
        LocalDateTime nearestCeil = now.plusMinutes(nearestHalfRange);

        int countFinalizingMoved = intakeRepository.countByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.MOVED);
        int countFinalizingScheduled = intakeRepository.countByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.SCHEDULED);

        if (countFinalizingMoved!=0) {
            log.info("Будет отмечено {} приемов в статусе MOVED", countFinalizingMoved);
            List<Intake> movedToFinalize = intakeRepository
                    .findByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.MOVED);


            for (Intake intake : movedToFinalize) {
                intakeStatusManager.autoFinalizeIntakeStatus(intake, nearestBottom, nearestCeil, now);
            }
            int countFinalizingMovedNew = intakeRepository.countByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.MOVED);
            log.info("Приемов в статусе MOVED уменьшилось на {}", countFinalizingMoved-countFinalizingMovedNew);
        }

        if (countFinalizingScheduled != 0) {
            log.info("Будет отмечено {} приемов в статусе SCHEDULED", countFinalizingScheduled);
            List<Intake> scheduledToToFinalize = intakeRepository
                    .findByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.SCHEDULED);

            for (Intake intake : scheduledToToFinalize) {
                intakeStatusManager.autoFinalizeIntakeStatus(intake, nearestBottom, nearestCeil, now);
            }
            int countFinalizingScheduledNew = intakeRepository.countByScheduledAtBeforeAndIntakeStatus(nearestBottom, IntakeStatus.SCHEDULED);
            log.info("Приемов в статусе MOVED уменьшилось на {}", countFinalizingScheduled-countFinalizingScheduledNew);
        }
    }

    private IntakeResponseDto intakeToDto(Intake intake) {
        return intakeHelper.entityToResponseDto(intake);
    }

}
