package org.medcontrol.util;

import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.SchemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class SchemeValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemeValidator.class);
    private SchemeRepository schemeRepository;
    private IntakeRepository intakeRepository;

    public SchemeValidator(SchemeRepository schemeRepository, IntakeRepository intakeRepository) {
        this.schemeRepository = schemeRepository;
        this.intakeRepository = intakeRepository;
    }

    public void existsByNameAndProfileId(String schemeName, String profileId) {
        if (schemeRepository.existsByNameAndProfileId(schemeName, UUID.fromString(profileId))) {
            log.info("Исключение на уровне сервиса: попытка создания схемы с неуникальным именем");
            throw new IllegalArgumentException("Схема с таким названием уже существует в профиле");
        }
    }

    public void createEmptyMedicationName(String medicationName) {
        if (medicationName.isEmpty()) {
            log.info("Исключение на уровне сервиса: попытка создания схемы без указания препарата");
            throw new IllegalArgumentException("Название препарата не может быть пустым");
        }
    }

    public void matchStatuses(SchemeStatus currentStatus, SchemeStatus targetStatus) {
        if (targetStatus == currentStatus)
            throw new IllegalStateException("Новый статус не может совпадать с текущим статусом");
    }

    public void completeSchemeWithoutIntakes(String schemeId, SchemeStatus targetStatus) {
        if (targetStatus == SchemeStatus.COMPLETED && !hasIntakesForScheme(schemeId)) {
            throw new IllegalStateException("Невозможно завершить схему без приемов");
        }
    }

    public boolean hasIntakesForScheme(String schemeId) {
        return intakeRepository.existsBySchemeId(UUID.fromString(schemeId));
    }

    public void validateDateRange(Scheme scheme) {
        if (!isValidDateRange(scheme.getStartDate(), scheme.getEndDate())) {
            throw new IllegalArgumentException("Неверный диапазон дат");
        }
    }

    private boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return false;
        }
        if (endDate != null && startDate.isAfter(endDate)) {
            return false;
        }
        return true;
    }
}
