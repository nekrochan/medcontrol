package org.medcontrol.service.impl;
import jakarta.transaction.Transactional;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.entity.enums.SchemeType;
import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;
import org.medcontrol.service.SchemeService;
import org.medcontrol.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.medcontrol.dto.request.*;
import org.medcontrol.dto.response.SchemeResponseDto;
import org.medcontrol.dto.response.SchemeStatisticResponseDto;
import org.medcontrol.entity.*;
import org.medcontrol.entity.keepers.AlarmKeeper;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.ProfileRepository;
import org.medcontrol.repository.SchemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SchemeServiceImpl implements SchemeService {

    private static final Logger log = LoggerFactory.getLogger(SchemeServiceImpl.class);

    private final SchemeRepository schemeRepository;
    private final ProfileRepository profileRepository;
    private final IntakeRepository intakeRepository;
    private final IntakeServiceImpl intakeService;
    private final IntakeDateTimeCalculator dateTimeCalculator;
    private final SchemeHelper schemeHelper;
    private final SchemeValidator schemeValidator;
    private IntakeStatusManager intakeStatusManager;
    private final int nearestHalfRange;

    @Autowired
    public SchemeServiceImpl(
            SchemeRepository schemeRepository,
            ProfileRepository profileRepository,
            IntakeRepository intakeRepository,
            IntakeServiceImpl intakeService,
            IntakeDateTimeCalculator dateTimeCalculator,
            SchemeHelper schemeHelper,
            SchemeValidator schemeValidator,
            IntakeStatusManager intakeStatusManager,
            IntakeHelper intakeHelper
    ) {
        this.schemeRepository = schemeRepository;
        this.profileRepository = profileRepository;
        this.intakeRepository = intakeRepository;
        this.intakeService = intakeService;
        this.dateTimeCalculator = dateTimeCalculator;
        this.schemeHelper = schemeHelper;
        this.schemeValidator = schemeValidator;
        this.intakeStatusManager = intakeStatusManager;
        this.nearestHalfRange = intakeHelper.getNearestHalfRange();
    }

    @Override
    public Scheme createScheme(CreateSchemeDto dto) {
        log.info("ВЫЗВАН МЕТОД: SchemeServiceImpl.createScheme()");
        log.info("Сервис: запрос на создание схемы '{}' профилем: {}", dto.getName(), dto.getProfileId());

        Profile profile = profileRepository.findById(UUID.fromString(dto.getProfileId()))
                .orElseThrow(() -> new IllegalArgumentException("Профиль не найден"));

        schemeValidator.existsByNameAndProfileId(dto.getName(), dto.getProfileId());

        schemeValidator.createEmptyMedicationName(dto.getMedicationName());

        Scheme scheme = new Scheme(profile, dto.getName());
        scheme.setMedicationName(dto.getMedicationName());
        scheme.setStatus(SchemeStatus.INACTIVE);

        scheme.setSchemeType(schemeHelper.getSchemeTypeFromString(dto.getSchemeType()));


        scheme.setDosage(dto.getDosage());
        scheme.setMeasure(dto.getMeasure());
        scheme.setMedicationName(dto.getMedicationName());
        schemeRepository.save(scheme);

        log.info("Сервис: сохранение схемы '{}' для профиля: {}, тип схемы: {}",
                dto.getName(), dto.getProfileId(), scheme.getSchemeType());

        log.info("ЗАВЕРШЕН МЕТОД: SchemeServiceImpl.createScheme()");
        return scheme;
    }

    @Override
    public SchemeResponseDto getSchemeDtoById(String schemeId) {
        if (!schemeRepository.existsById(UUID.fromString(schemeId)))
            throw new IllegalArgumentException("Схема не найдена: id" + schemeId);

        Scheme scheme =  schemeHelper.getSchemeOrThrow(schemeId);

        log.info("Получена схема: id={}, takeDays={}, skipdays={}",
                scheme.getId(), scheme.getAlternationDays().getTakeDays(),
                scheme.getAlternationDays().getSkipDays());

        return schemeHelper.entityToResponseDto(scheme);
    }

    @Override
    public List<SchemeResponseDto> getSchemesByProfileId(String profileId) {
        List<Scheme> schemes = schemeHelper.getSchemesByProfileIdOrThrow(profileId);
        List<SchemeResponseDto> dtos = new ArrayList<>();
        for (Scheme scheme : schemes) {
            dtos.add(schemeHelper.entityToResponseDto(scheme));
        }
        return dtos;
    }

    @Override
    public void updateSchemeSimpleData(SchemeSimpleDataRequestDto dto) {
        Scheme scheme = schemeHelper.getSchemeOrThrow(dto.getSchemeId());
        scheme.setName(dto.getName());
        scheme.setDosage(dto.getDosage());
        scheme.setMeasure(dto.getMeasure());
        scheme.setMedicationName(dto.getMedicationName());
        schemeRepository.save(scheme);

        List<Intake> intakes = intakeRepository.findBySchemeId(scheme.getId());
        for (Intake intake : intakes) {
            intake.setDescription(scheme);
        }
        intakeRepository.saveAllAndFlush(intakes);
    }

    @Override
    public void updateSchemeDateTime(SchemeDateTimeRequestDto dto) {
        log.info("ВЫЗВАН МЕТОД: SchemeServiceImpl.updateSchemeDateTime()");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nearestBottom = now.minusMinutes(nearestHalfRange);

        Scheme updatingScheme = schemeHelper.setSchemeTypeFromDtoAndReturnScheme(dto);

        updatingScheme.setStartDate(schemeHelper.getStartDateFromDto(dto));

        updatingScheme.setEndDate(schemeHelper.getEndDateFromDto(dto));

        schemeHelper.setAlarmsPerDayFromDto(dto);

        if (updatingScheme.getStatus() == SchemeStatus.ACTIVE) {
            int deleting = intakeRepository.findBySchemeIdAndScheduledAtAfter(
                    updatingScheme.getId(), nearestBottom
            ).size();
            intakeRepository.deleteBySchemeIdAndScheduledAtAfter(
                    updatingScheme.getId(), nearestBottom
            );
            intakeRepository.flush();
            List<Intake> reloadIntakes = intakeRepository.findBySchemeId(updatingScheme.getId());
            Scheme reloadScheme= schemeHelper.getSchemeOrThrow(updatingScheme.getId().toString());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<LocalDateTime> intakeTimes = calculateIntakeTimes(updatingScheme);
            for (LocalDateTime dateTime : intakeTimes) {
                if (dateTime.isAfter(LocalDateTime.now())) {
                    createIntakeForDateTime(updatingScheme, dateTime);
                }
            }
        } else activateScheme(updatingScheme);

        log.info("ЗАВЕРШЕН МЕТОД: SchemeServiceImpl.updateSchemeDateTime()");
        schemeRepository.save(updatingScheme);
    }


    @Override
    public void updateSchemeStatus(String schemeId, SchemeStatusRequestDto dto) {
        SchemeStatus targetStatus = schemeHelper.getSchemeStatusFromString(dto.getStatus());
        Scheme scheme = schemeHelper.getSchemeOrThrow(schemeId);
        SchemeStatus currentStatus = scheme.getStatus();

        schemeValidator.matchStatuses(currentStatus, targetStatus);

        schemeValidator.completeSchemeWithoutIntakes(schemeId, targetStatus);

        if (targetStatus == SchemeStatus.ACTIVE)
            activateScheme(scheme);
        else schemeHelper.manageSchemeStatusExceptActivation(scheme, targetStatus);

        schemeRepository.saveAndFlush(scheme);
    }

    @Override
    public void deleteScheme(String schemeId) {
        intakeService.deleteBySchemeId(schemeId);
        intakeRepository.flush();
        schemeRepository.deleteById(UUID.fromString(schemeId));
        schemeRepository.flush();

    }

    private void calculateIntakes(Scheme scheme) {
        calculateIntakes(scheme, false);
    }

    private void createIntakeForDateTime(Scheme scheme, LocalDateTime dateTime) {
        IntakeCreateRequestDto intakeDto = new IntakeCreateRequestDto();
        intakeDto.setSchemeId(scheme.getId().toString());
        intakeDto.setScheduledAt(dateTime);

        intakeDto.setIntakeStatus(intakeStatusManager
                .autoCancelNewIntake(dateTime)
                .toString()
        );

        intakeDto.setMedicationName(scheme.getMedicationName());
        intakeDto.setDosage(String.valueOf(scheme.getDosage()));
        intakeDto.setMeasure(scheme.getMeasure());

        try {
            intakeService.createIntake(intakeDto);
        } catch (Exception e) {
            log.error("Ошибка при создании приема для времени {}: {}", dateTime, e.getMessage());
        }
    }


    private void recalculateIntakesFromNow(Scheme scheme) {
        log.info("Пересчет приемов с текущей даты для схемы: {}", scheme.getId());

        LocalDateTime now = LocalDateTime.now();
        int deletedCount = intakeRepository.countBySchemeIdAndScheduledAtAfter(scheme.getId(), now);
        intakeRepository.deleteBySchemeIdAndScheduledAtAfter(scheme.getId(), now);
        intakeRepository.flush();

        long totalDays = ChronoUnit.DAYS.between(scheme.getStartDate(), scheme.getEndDate());
        long passedDays = ChronoUnit.DAYS.between(scheme.getStartDate(), LocalDate.now());
        long remainingDays = totalDays - passedDays;

        if (remainingDays <= 0) {
            log.warn("Нет осталось дней для схемы {}", scheme.getId());
            return;
        }

        LocalDate newStartDate = LocalDate.now();
        LocalDate newEndDate = newStartDate.plusDays(remainingDays);

        Scheme tempScheme = new Scheme(scheme.getProfile(), scheme.getName() + "_temp");
        tempScheme.setMedicationName(scheme.getMedicationName());
        tempScheme.setDosage(scheme.getDosage());
        tempScheme.setMeasure(scheme.getMeasure());
        tempScheme.setSchemeType(scheme.getSchemeType());
        tempScheme.setAlternationDays(scheme.getAlternationDays());
        tempScheme.setWeekdays(scheme.getWeekdays());
        tempScheme.setStartDate(newStartDate);
        tempScheme.setEndDate(newEndDate);

        tempScheme.getAlarmsPerDay().clear();
        for (AlarmKeeper alarm : scheme.getAlarmsPerDay()) {
            AlarmKeeper newAlarm = new AlarmKeeper();
            newAlarm.setAlarmTime(alarm.getAlarmTime());
            newAlarm.setScheme(tempScheme);
            tempScheme.getAlarmsPerDay().add(newAlarm);
        }

        List<LocalDateTime> newIntakeTimes = calculateIntakeTimes(tempScheme);
        log.info("Рассчитано приемов для временной схемы: {}", newIntakeTimes.size());

        for (LocalDateTime dateTime : newIntakeTimes) {
            if (dateTime.isAfter(now)) {
                IntakeCreateRequestDto intakeDto = new IntakeCreateRequestDto();
                intakeDto.setSchemeId(scheme.getId().toString());
                intakeDto.setScheduledAt(dateTime);
                intakeDto.setIntakeStatus(IntakeStatus.SCHEDULED.toString());
                intakeDto.setMedicationName(scheme.getMedicationName());
                intakeDto.setDosage(String.valueOf(scheme.getDosage()));
                intakeDto.setMeasure(scheme.getMeasure());

                try {
                    intakeService.createIntake(intakeDto);
                    log.debug("Создан прием на время: {}", dateTime);
                } catch (Exception e) {
                    log.error("Ошибка при создании приема на {}: {}", dateTime, e.getMessage());
                }
            }
        }

        scheme.setStartDate(newStartDate);
        scheme.setEndDate(newEndDate);

        log.info("Пересчет завершен. Создано приемов: {}", newIntakeTimes.stream()
                .filter(dt -> dt.isAfter(now)).count());
    }

    private void calculateIntakesWithCountControl(Scheme scheme, boolean savePastIntakes, int expectedNewCount) {
        log.info("Расчет приемов для схемы: {}, savePastIntakes={}, ожидается новых приемов: {}",
                scheme.getId(), savePastIntakes, expectedNewCount);

        schemeValidator.validateDateRange(scheme);

        List<LocalDateTime> allIntakeTimes = calculateIntakeTimes(scheme);
        List<Intake> existingIntakes = intakeRepository.findBySchemeId(scheme.getId());

        int createdCount = 0;
        List<Intake> createdIntakes = new ArrayList<>();

        for (LocalDateTime dateTime : allIntakeTimes) {
            boolean intakeExists = false;
            Intake existingIntake = null;

            for (Intake intake : existingIntakes) {
                if (intake.getScheduledAt() != null && intake.getScheduledAt().equals(dateTime)) {
                    intakeExists = true;
                    existingIntake = intake;
                    break;
                }
            }

            if (savePastIntakes && dateTime.isBefore(LocalDateTime.now())) {
                if (!intakeExists) {
                    Intake newIntake = createIntakeForDateTimeWithReturn(scheme, dateTime);
                    if (newIntake != null) {
                        createdCount++;
                        createdIntakes.add(newIntake);
                    }
                }
            } else if (!intakeExists) {
                Intake newIntake = createIntakeForDateTimeWithReturn(scheme, dateTime);
                if (newIntake != null) {
                    createdCount++;
                    createdIntakes.add(newIntake);
                }
            } else if (existingIntake != null && existingIntake.getIntakeStatus() == IntakeStatus.MOVED) {
                existingIntake.setIntakeStatus(IntakeStatus.SCHEDULED);
                intakeRepository.save(existingIntake);
            }
        }

        if (createdCount != expectedNewCount) {
            log.error("НЕСООТВЕТСТВИЕ: создано {} новых приемов, а должно было быть {} (удалено PAUSED)",
                    createdCount, expectedNewCount);

            if (!createdIntakes.isEmpty()) {
                log.warn("Откат созданных приемов из-за несоответствия количества");
                intakeRepository.deleteAll(createdIntakes);
            }

            throw new IllegalStateException(String.format(
                    "Контроль количества приемов не пройден: создано %d, ожидалось %d",
                    createdCount, expectedNewCount));
        }

        log.info("Контроль пройден: создано {} новых приемов (соответствует количеству удаленных PAUSED)", createdCount);
    }

    private void calculateIntakes(Scheme scheme, boolean savePastIntakes) {
        calculateIntakes(scheme, savePastIntakes, -1);
    }

    private void calculateIntakes(Scheme scheme, boolean preservePastIntakes, int expectedNewCount) {
        log.info("Расчет приемов для схемы: {}, preservePastIntakes={}, expectedNewCount={}",
                scheme.getId(), preservePastIntakes, expectedNewCount);

        schemeValidator.validateDateRange(scheme);

        List<LocalDateTime> allIntakeTimes = calculateIntakeTimes(scheme);
        List<Intake> existingIntakes = intakeRepository.findBySchemeId(scheme.getId());

        int createdCount = 0;

        for (LocalDateTime dateTime : allIntakeTimes) {
            boolean intakeExists = false;
            Intake existingIntake = null;

            for (Intake intake : existingIntakes) {
                if (intake.getScheduledAt() != null && intake.getScheduledAt().equals(dateTime)) {
                    intakeExists = true;
                    existingIntake = intake;
                    break;
                }
            }

            if (preservePastIntakes && dateTime.isBefore(LocalDateTime.now())) {
                if (!intakeExists) {
                    createIntakeForDateTime(scheme, dateTime);
                    createdCount++;
                }
            } else if (!intakeExists) {
                createIntakeForDateTime(scheme, dateTime);
                createdCount++;
            } else if (existingIntake != null && existingIntake.getIntakeStatus() == IntakeStatus.MOVED) {
                existingIntake.setIntakeStatus(IntakeStatus.SCHEDULED);
                intakeRepository.saveAndFlush(existingIntake);
            }
        }

        if (expectedNewCount != -1 && createdCount != expectedNewCount) {
            log.error("НЕСООТВЕТСТВИЕ: создано {} новых приемов, ожидалось {}", createdCount, expectedNewCount);
            throw new IllegalStateException(String.format(
                    "Контроль количества приемов не пройден: создано %d, ожидалось %d",
                    createdCount, expectedNewCount));
        }
    }

    private Intake createIntakeForDateTimeWithReturn(Scheme scheme, LocalDateTime dateTime) {
        LocalDateTime nearestBottom = LocalDateTime.now().minusMinutes(nearestHalfRange);
        Intake intake = new Intake(scheme);
        intake.setScheduledAt(dateTime);
        if (dateTime.isBefore(nearestBottom))
            intake.setIntakeStatus(IntakeStatus.CANCELLED);
        else intake.setIntakeStatus(IntakeStatus.SCHEDULED);

        return intakeRepository.saveAndFlush(intake);
    }

    private void createCopyAndActivate(Scheme scheme) {
        log.info("Создание копии схемы {} при переходе из COMPLETED в ACTIVE", scheme.getId());

        long longevity = ChronoUnit.DAYS.between(scheme.getStartDate(), scheme.getEndDate());
        LocalDate newStart = LocalDate.now();
        LocalDate newEnd = newStart.plusDays(longevity);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String copyName = scheme.getName() + "_copy_" + timestamp;

        log.info("Создание копии с именем: {}", copyName);

        CreateSchemeDto copyCreateSchemeDto = new CreateSchemeDto();
        copyCreateSchemeDto.setProfileId(scheme.getProfile().getId().toString());
        copyCreateSchemeDto.setName(copyName);
        copyCreateSchemeDto.setMedicationName(scheme.getMedicationName());
        copyCreateSchemeDto.setDosage(scheme.getDosage());
        copyCreateSchemeDto.setMeasure(scheme.getMeasure());
        copyCreateSchemeDto.setSchemeType(scheme.getSchemeType().toString());

        Scheme copyScheme = createScheme(copyCreateSchemeDto);
        copyScheme.setStartDate(newStart);
        copyScheme.setEndDate(newEnd);
        copyScheme.setSchemeType(scheme.getSchemeType());
        copyScheme.setAlternationDays(scheme.getAlternationDays());
        copyScheme.setWeekdays(scheme.getWeekdays());
        copyScheme.setAlarmsPerDay(
                scheme.getAlarmsPerDay().stream()
                        .map(alarm -> {
                            AlarmKeeper newAlarm = new AlarmKeeper();
                            newAlarm.setAlarmTime(alarm.getAlarmTime());
                            newAlarm.setScheme(copyScheme);
                            return newAlarm;
                        })
                        .collect(Collectors.toList()));
        copyScheme.setStatus(SchemeStatus.ACTIVE);
        schemeRepository.saveAndFlush(copyScheme);

        calculateIntakesOnActivation(copyScheme);
    }

    private void calculateIntakesOnActivation(Scheme scheme) {
        LocalDate startDate = LocalDate.now();
        long remainingDays = ChronoUnit.DAYS.between(startDate, scheme.getEndDate());

        if (scheme.getStatus() == SchemeStatus.PAUSED) {
            remainingDays = ChronoUnit.DAYS.between(scheme.getStartDate(), scheme.getEndDate());
            long passedDays = ChronoUnit.DAYS.between(scheme.getStartDate(), LocalDate.now());
            remainingDays = remainingDays - passedDays;
            if (intakeRepository.existsBySchemeIdAndScheduledAt(scheme.getId(), LocalDateTime.now()))
                intakeRepository.deleteBySchemeIdAndScheduledAtAfter(
                        scheme.getId(), LocalDateTime.now()
                );
            intakeRepository.flush();
            List<Intake> reloadIntakes = intakeRepository.findBySchemeId(scheme.getId());
            Scheme reloadScheme= schemeHelper.getSchemeOrThrow(scheme.getId().toString());
        }

        LocalDate endDate = startDate.plusDays(Math.max(remainingDays, 1));
        scheme.setStartDate(startDate);
        scheme.setEndDate(endDate);

        List<LocalDateTime> newIntakeTimes = calculateIntakeTimes(scheme);
        for (LocalDateTime dateTime : newIntakeTimes) {
            if (dateTime.isAfter(LocalDateTime.now())){
                IntakeCreateRequestDto intakeDto = new IntakeCreateRequestDto();
                intakeDto.setSchemeId(scheme.getId().toString());
                intakeDto.setScheduledAt(dateTime);
                intakeDto.setIntakeStatus(IntakeStatus.SCHEDULED.toString());
                intakeDto.setMedicationName(scheme.getMedicationName());
                intakeDto.setDosage(String.valueOf(scheme.getDosage()));
                intakeDto.setMeasure(scheme.getMeasure());
                intakeService.createIntake(intakeDto);
            }
        }
    }

    private List<LocalDateTime> calculateIntakeTimes(Scheme scheme) {
        CalculateIntakesDto calcDto = new CalculateIntakesDto();
        calcDto.setStartDate(scheme.getStartDate());
        calcDto.setEndDate(scheme.getEndDate());
        calcDto.setAlarms(scheme.getAlarmTimes());

        if (scheme.getAlarmTimes().isEmpty()) {
            log.warn("У схемы {} нет времен приема", scheme.getId());
            return Collections.emptyList();
        }

        if (scheme.getSchemeType() == SchemeType.WEEKDAYS) {
            if (scheme.getWeekdays() == null) {
                log.error("У схемы с id {} Weekdays содержит null, устанавливаем значения по умолчанию", scheme.getId());
                scheme.setWeekdays(new WeekdaysKeeper(false, false, false, false, false, false, false));
                schemeRepository.save(scheme);
            }
            calcDto.setWeekdays(scheme.getWeekdays().toList());
            return dateTimeCalculator.weekdays(calcDto);

        } else if (scheme.getSchemeType() == SchemeType.ALTERNATION) {
            if (scheme.getAlternationDays() == null) {
                log.error("У схемы с id {} AlternationDays содержит null, устанавливаем значения по умолчанию", scheme.getId());
                scheme.setAlternationDays(new AlternationDaysKeeper(1, 0));
                schemeRepository.save(scheme);
            }
            calcDto.setTakeDays(scheme.getAlternationDays().getTakeDays());
            calcDto.setSkipDays(scheme.getAlternationDays().getSkipDays());

            log.info("Расчет alternation: takeDays={}, skipDays={}",
                    calcDto.getTakeDays(), calcDto.getSkipDays());

            return dateTimeCalculator.alternation(calcDto);

        } else if (scheme.getSchemeType() == SchemeType.UNSPECIFIED) {
            log.warn("Схема {} имеет тип UNSPECIFIED, переводим в ALTERNATION", scheme.getId());
            scheme.setSchemeType(SchemeType.ALTERNATION);
            scheme.setAlternationDays(new AlternationDaysKeeper(1, 0));
            schemeRepository.save(scheme);

            calcDto.setTakeDays(1);
            calcDto.setSkipDays(0);
            return dateTimeCalculator.alternation(calcDto);
        }

        throw new IllegalArgumentException("Неизвестный тип схемы: " + scheme.getSchemeType());
    }

    @Override
    public Scheme getSchemeById(String schemeId) {
        return schemeHelper.getSchemeOrThrow(schemeId);
    }

    @Override
    public SchemeStatisticResponseDto getSchemeStatistics(String schemeId) {

        int total;
        try {
            total = intakeRepository.findBySchemeId(UUID.fromString(schemeId)).size();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Схема не найдена: " + schemeId);
        }

        if (total==0.0) {
            SchemeStatisticResponseDto emptyDto = new SchemeStatisticResponseDto();
            emptyDto.setSchemeId(schemeId);
            emptyDto.setTakenPart(0.0);
            return emptyDto;
        }

        int taken = intakeRepository
                .countBySchemeIdAndIntakeStatus(UUID.fromString(schemeId), IntakeStatus.TAKEN);
        int cancelled = intakeRepository
                .countBySchemeIdAndIntakeStatus(UUID.fromString(schemeId), IntakeStatus.CANCELLED);

        double takenPercent = (double) (taken * 100.00 / total);
        double cancelledPercent = (double) (cancelled * 100.00 / total);

        SchemeStatisticResponseDto dto = new SchemeStatisticResponseDto();
        dto.setSchemeId(schemeId);
        dto.setTakenPart(takenPercent);
        dto.setCancelledPart(cancelledPercent);

        return dto;
    }

    @Scheduled(fixedDelay = 60000)
    public void autoCompleteExpiredSchemes() {
        log.info("Поиск схем для автоматического завершения");
        LocalDateTime nearestCeil = LocalDateTime.now().plusMinutes(nearestHalfRange);
        List<Scheme> activeSchemes = schemeRepository.findAll().stream()
                .filter(s -> s.getStatus() == SchemeStatus.ACTIVE)
                .collect(Collectors.toList());
        for (Scheme scheme : activeSchemes) {
            List<Intake> futureIntakes = intakeRepository.findBySchemeIdAndScheduledAtAfter(
                    scheme.getId(), nearestCeil);

            if (futureIntakes.isEmpty()) {
                log.info("Нет будущих приемов для схемы {}. Схема будет завершена", scheme.getId());
                List<Intake> intakes = intakeRepository.findBySchemeId(scheme.getId());
                List<Intake> toDelete = new ArrayList<>();
                for (Intake intake : intakes) {
                    if (intake.getIntakeStatus() == IntakeStatus.SCHEDULED ||
                            intake.getIntakeStatus() == IntakeStatus.MOVED) {
                        toDelete.add(intake);
                    }
                }
                intakeRepository.deleteAll(toDelete);
                if (scheme.getEndDate() != null && scheme.getEndDate().isAfter(LocalDate.now()))
                    scheme.setEndDate(LocalDate.now());
                scheme.setStatus(SchemeStatus.COMPLETED);
                log.info("Схема {} завершена", scheme.getId());
                schemeRepository.save(scheme);
            }
        }
    }

    private void activateScheme (Scheme scheme) {
        SchemeStatus currentStatus = scheme.getStatus();
        switch (currentStatus) {
            case COMPLETED -> {
                createCopyAndActivate(scheme);
            }
            case PAUSED -> {
                recalculateIntakesFromNow(scheme);
                intakeRepository.flush();
                List<Intake> reloadIntakes = intakeRepository.findBySchemeId(scheme.getId());
                scheme.setStatus(SchemeStatus.ACTIVE);
                schemeRepository.saveAndFlush(scheme);
                Scheme reloadScheme = schemeHelper.getSchemeOrThrow(scheme.getId().toString());
            }
            case INACTIVE -> {
                calculateIntakes(scheme);
                intakeRepository.flush();
                List<Intake> reloadIntakes = intakeRepository.findBySchemeId(scheme.getId());
                scheme.setStatus(SchemeStatus.ACTIVE);
                schemeRepository.saveAndFlush(scheme);
                Scheme reloadScheme= schemeHelper.getSchemeOrThrow(scheme.getId().toString());
            }
            default -> throw new IllegalStateException("Невозможно активировать схему в статусе: " + currentStatus);
        }

    }
}
