package org.medcontrol.util;

import org.medcontrol.dto.request.SchemeDateTimeRequestDto;
import org.medcontrol.dto.response.SchemeResponseDto;
import org.medcontrol.entity.Intake;
import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.entity.enums.SchemeType;
import org.medcontrol.entity.keepers.AlarmKeeper;
import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.ProfileRepository;
import org.medcontrol.repository.SchemeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SchemeHelper {
    private static final Logger log = LoggerFactory.getLogger(SchemeHelper.class);
    private final ProfileRepository profileRepository;

    private SchemeRepository schemeRepository;
    private IntakeRepository intakeRepository;
    private SchemeValidator schemeValidator;

    public SchemeHelper(ProfileRepository profileRepository, SchemeRepository schemeRepository, IntakeRepository intakeRepository, SchemeValidator schemeValidator) {
        this.profileRepository = profileRepository;
        this.schemeRepository = schemeRepository;
        this.intakeRepository = intakeRepository;
        this.schemeValidator = schemeValidator;
    }

    public SchemeResponseDto entityToResponseDto(Scheme scheme) {
        SchemeResponseDto dto = new SchemeResponseDto();
        dto.setId(scheme.getId().toString());
        dto.setProfileId(scheme.getProfile().getId().toString());
        dto.setName(scheme.getName());
        dto.setMedicationName(scheme.getMedicationName());
        dto.setDosage(scheme.getDosage());
        dto.setMeasure(scheme.getMeasure());
        dto.setStartDate(scheme.getStartDate());
        dto.setEndDate(scheme.getEndDate());

        List<LocalTime> alarmTimes = new ArrayList<>();
        for (AlarmKeeper alarm : scheme.getAlarmsPerDay()) {
            alarmTimes.add(alarm.getAlarmTime());
        }
        dto.setAlarmTimes(alarmTimes);

        dto.setSchemeType(scheme.getSchemeType().name());
        dto.setStatus(scheme.getStatus().name());

        if (scheme.getAlternationDays() != null) {
            dto.setAlternationDays(scheme.getAlternationDays());
        }

        if (scheme.getWeekdays() != null) {
            dto.setWeekdays(scheme.getWeekdays());
        }

        return dto;
    }

    public SchemeStatus getSchemeStatusFromString(String status) {
        if (status.equalsIgnoreCase(SchemeStatus.ACTIVE.toString()))
            return SchemeStatus.ACTIVE;
        else if (status.equalsIgnoreCase(SchemeStatus.INACTIVE.toString())) {
            return SchemeStatus.INACTIVE;
        } else if (status.equalsIgnoreCase(SchemeStatus.COMPLETED.toString())) {
            return SchemeStatus.COMPLETED;
        } else if (status.equalsIgnoreCase(SchemeStatus.PAUSED.toString())) {
            return SchemeStatus.PAUSED;
        } else throw new IllegalArgumentException("Не удалось определить статус схемы");
    }

    public Scheme getSchemeOrThrow(String schemeId) {
        return schemeRepository.findById(UUID.fromString(schemeId))
                .orElseThrow(() -> new IllegalArgumentException("Схема не найдена: " + schemeId));
    }

    public List<Scheme> getSchemesByProfileIdOrThrow(String profileId) {
        if (!profileRepository.existsById(UUID.fromString(profileId)))
            throw new IllegalArgumentException("Профиль не найден");
        return schemeRepository.findByProfileId(UUID.fromString(profileId));
    }

    public SchemeType getSchemeTypeFromString(String schemeType) {
        if (schemeType.equalsIgnoreCase(SchemeType.ALTERNATION.name())){
            return SchemeType.ALTERNATION;
        } else if (schemeType.equalsIgnoreCase(SchemeType.WEEKDAYS.toString())) {
            return SchemeType.WEEKDAYS;
        } else {
            log.info("Был запрошен тип схемы: '" + schemeType+"'. Возвращен тип схемы: UNSPECIFIED");
            return SchemeType.UNSPECIFIED;
        }
    }

    private Scheme setWeekdaysSchemeType(SchemeDateTimeRequestDto dto) {
        Scheme updatingScheme = getSchemeOrThrow(dto.getSchemeId());
        SchemeType targetType = getSchemeTypeFromString(dto.getSchemeType());

        if (targetType == SchemeType.WEEKDAYS) {

            updatingScheme.setSchemeType(SchemeType.WEEKDAYS);
            log.info("Создание расписания для схемы с типом WEEKDAYS");

            if (dto.getWeekdays() != null) {
                log.info("Расписание сохранится с переданными параметрами: weekdaysList=[{}]",
                        dto.getWeekdays().toList());
                updatingScheme.setWeekdays(dto.getWeekdays());
            } else {
                log.info("Переданных параметров не обнаружено. Все дни недели установлены как 'false'.");
                updatingScheme.setWeekdays(new WeekdaysKeeper(false, false, false, false, false, false, false));
            }

            if (updatingScheme.getAlternationDays() == null)
                updatingScheme.setAlternationDays(new AlternationDaysKeeper(1, 0));
        }
        return updatingScheme;
    }

    private Scheme setAlternationSchemeType(SchemeDateTimeRequestDto dto) {
        Scheme updatingScheme = getSchemeOrThrow(dto.getSchemeId());
        SchemeType targetType = getSchemeTypeFromString(dto.getSchemeType());

        if (targetType == SchemeType.ALTERNATION) {
            log.info("Создание расписания для схемы с типом ALTERNATION");
            updatingScheme.setSchemeType(SchemeType.ALTERNATION);

            if (dto.getAlternationDays() != null && dto.getAlternationDays().getSkipDays() > 0) {
                log.info("Расписание сохранится с переданными параметрами: takeDays={}, skipDays={}",
                        dto.getAlternationDays().getTakeDays(), dto.getAlternationDays().getSkipDays());

                updatingScheme.setAlternationDays(dto.getAlternationDays());
            } else {
                if (updatingScheme.getAlternationDays() == null) {
                    log.info("Переданных параметров не обнаружено. " +
                            "Расписание сохранится с параметрами по умолчанию: takeDays=1, skipDays=0");

                    updatingScheme.setAlternationDays(new AlternationDaysKeeper(1, 0));
                }
            }
            if (updatingScheme.getWeekdays() == null) {
                updatingScheme.setWeekdays(new WeekdaysKeeper(false, false, false, false, false, false, false));
            }
        } else if (targetType == SchemeType.UNSPECIFIED) {
            log.info("Создание расписания для схемы с типом UNSPECIFIED. " +
                    "Будут заданы параметры; schemeType=ALTERNATION, takeDays=1, skipDays=0.");

            updatingScheme.setAlternationDays(new AlternationDaysKeeper(1, 0));
            updatingScheme.setSchemeType(SchemeType.ALTERNATION);
        }
        return updatingScheme;
    }

    public Scheme setSchemeTypeFromDtoAndReturnScheme(SchemeDateTimeRequestDto dto) {
        SchemeType targetType = getSchemeTypeFromString(dto.getSchemeType());
        if (targetType == SchemeType.WEEKDAYS) {
            return setWeekdaysSchemeType(dto);
        } else if (targetType == SchemeType.ALTERNATION) {
            return setAlternationSchemeType(dto);
        } else if (targetType == SchemeType.UNSPECIFIED) {
            return setAlternationSchemeType(dto);
        } else {
            dto.setSchemeType(SchemeType.ALTERNATION.toString());
            return setAlternationSchemeType(dto);
        }
    }

    public LocalDate getStartDateFromDto(SchemeDateTimeRequestDto dto) {
        if (dto.getStartDate() != null) return dto.getStartDate();
        else return LocalDate.now();
    }

    public LocalDate getEndDateFromDto(SchemeDateTimeRequestDto dto) {
        if (dto.getEndDate() != null) return dto.getEndDate();
        else if (dto.getStartDate() != null) return dto.getStartDate().plusMonths(1);
        else return LocalDate.now().plusMonths(1);
    }

    public void setAlarmsPerDayFromDto(SchemeDateTimeRequestDto dto) {
        Scheme updatingScheme = getSchemeOrThrow(dto.getSchemeId());

        try {
            updatingScheme.getAlarmsPerDay().size();
        } catch (Exception e) {
            log.warn("Коллекция alarmsPerDay битая, выполняется пересоздание");
            updatingScheme.setAlarmsPerDay(new ArrayList<>());
            schemeRepository.saveAndFlush(updatingScheme);
            updatingScheme = getSchemeOrThrow(dto.getSchemeId());
        }

        List<AlarmKeeper> alarmsPerDay = updatingScheme.getAlarmsPerDay();
        List<LocalTime> newAlarmTimesPerDay = dto.getAlarmsPerDay();

        if (newAlarmTimesPerDay.isEmpty()) return;
        if (alarmsPerDay.size() == newAlarmTimesPerDay.size()) {
            boolean isMatch = true;
            for (int i = 0; i < alarmsPerDay.size(); i++) {
                if (!alarmsPerDay.get(i).getAlarmTime().equals(newAlarmTimesPerDay.get(i))) {
                    isMatch = false;
                    break;
                }
            }
            if (isMatch) return;
        }

        updatingScheme.getAlarmsPerDay().clear();
        for (LocalTime time : newAlarmTimesPerDay) {
            AlarmKeeper keeper = new AlarmKeeper();
            keeper.setAlarmTime(time);
            keeper.setScheme(updatingScheme);
            updatingScheme.getAlarmsPerDay().add(keeper);
        }
        schemeRepository.saveAndFlush(updatingScheme);

    }

    public void manageSchemeStatusExceptActivation(Scheme scheme, SchemeStatus targetStatus) {
        SchemeStatus currentStatus = scheme.getStatus();

        if (targetStatus == null)
            throw new IllegalArgumentException("Попытка установить пустой статус");

        if (targetStatus == SchemeStatus.ACTIVE) {
            throw new IllegalArgumentException("Метод не может активировать схему");
        }
        else if (targetStatus == SchemeStatus.INACTIVE) {
            inactivateScheme(scheme);
        }
        else if (targetStatus == SchemeStatus.PAUSED) {
            pauseScheme(scheme);
        }
        else if (targetStatus == SchemeStatus.COMPLETED) {
            completeScheme(scheme);
        }
    }

    public void inactivateScheme (Scheme scheme) {
        SchemeStatus currentStatus = scheme.getStatus();

        intakeRepository.deleteBySchemeId(scheme.getId());
        intakeRepository.flush();
        scheme.setStatus(SchemeStatus.INACTIVE);
        schemeRepository.saveAndFlush(scheme);
    }

    public void pauseScheme(Scheme scheme) {
        SchemeStatus currentStatus = scheme.getStatus();

        if (currentStatus == SchemeStatus.ACTIVE) {
            scheme.setStatus(SchemeStatus.PAUSED);
            List<Intake> scheduledIntakes = intakeRepository.findBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.SCHEDULED);
            for (Intake intake : scheduledIntakes){
                intake.setIntakeStatus(IntakeStatus.PAUSED);
                intakeRepository.saveAndFlush(intake);
            }
        } else {
            throw new IllegalStateException("На паузу можно поставить только активную схему");
        }
    }

    public void completeScheme(Scheme scheme) {
        SchemeStatus currentStatus = scheme.getStatus();

        if (currentStatus == SchemeStatus.PAUSED || currentStatus == SchemeStatus.ACTIVE) {
            intakeRepository
                    .deleteBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.PAUSED);
            intakeRepository
                    .deleteBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.SCHEDULED);
            intakeRepository
                    .deleteBySchemeIdAndIntakeStatus(scheme.getId(), IntakeStatus.MOVED);

            intakeRepository.flush();

            if (scheme.getEndDate() != null && scheme.getEndDate().isAfter(LocalDate.now())) {
                scheme.setEndDate(LocalDate.now());
            }

            scheme.setStatus(SchemeStatus.COMPLETED);
            schemeRepository.saveAndFlush(scheme);

        } else if (currentStatus == SchemeStatus.INACTIVE) {
            if (schemeValidator.hasIntakesForScheme(scheme.getId().toString())) {
                if (scheme.getEndDate() != null && scheme.getEndDate().isBefore(LocalDate.now())) {
                    scheme.setStatus(SchemeStatus.COMPLETED);
                    schemeRepository.saveAndFlush(scheme);
                } else throw new IllegalStateException("Нельзя завершить неактивную схему с будущей датой окончания");
            } else throw new IllegalStateException("Нельзя завершить схему без приемов");
        } else throw new IllegalStateException("Невозможно завершить схему в статусе: " + currentStatus);
    }

}
