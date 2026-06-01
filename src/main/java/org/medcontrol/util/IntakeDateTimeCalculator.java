package org.medcontrol.util;

import org.medcontrol.dto.request.CalculateIntakesDto;
import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.keepers.AlarmKeeper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

@Component
public class IntakeDateTimeCalculator {
    public List<LocalDateTime> weekdays(CalculateIntakesDto dto) throws IllegalArgumentException{

        lookForSharedExceptionDto(dto);

        if (dto.getWeekdays() == null)
            throw new IllegalArgumentException("Список дней недели не задан");
        if (dto.getWeekdays().size()!=7)
            throw new IllegalArgumentException("Число дней недели должно быть равно 7");

        List<LocalTime> alarmsPerDay = dto.getAlarms();
        List<LocalDateTime> allAlarms = new LinkedList<>();
        List<Boolean> chosenDays = dto.getWeekdays();

        for (
                LocalDate day = dto.getStartDate();
                !day.isAfter(dto.getEndDate());
                day = day.plusDays(1)
        ) {
            int dayOfWeek = day.getDayOfWeek().getValue();
            if (chosenDays.get(dayOfWeek - 1)) {
                for (LocalTime time : alarmsPerDay) {
                    allAlarms.add(LocalDateTime.of(day, time));
                }
            }
        }
        return allAlarms;
    }

    public List<LocalDateTime> alternation(CalculateIntakesDto dto) throws IllegalArgumentException {

        lookForSharedExceptionDto(dto);

        if (dto.getSkipDays() < 0)
            throw new IllegalArgumentException("Число дней пропуска не может быть меньше нуля");
        if (dto.getTakeDays() <= 0)
            throw new IllegalArgumentException("Число дней приема должно быть больше нуля");

        List<LocalTime> alarmsPerDay = dto.getAlarms();
        List<LocalDateTime> allAlarms = new LinkedList<>();
        int takeDays = dto.getTakeDays();
        int skipDays = dto.getSkipDays();
        int cycleLength = takeDays + skipDays;

        if (skipDays == 0) {
            for (
                    LocalDate day = dto.getStartDate();
                    !day.isAfter(dto.getEndDate());
                    day = day.plusDays(1)
            ) {
                for (LocalTime time : alarmsPerDay) {
                    allAlarms.add(LocalDateTime.of(day, time));
                }
            }
        } else {
            int daysFromStart = 0;
            for (
                    LocalDate day = dto.getStartDate();
                    !day.isAfter(dto.getEndDate());
                    day = day.plusDays(1)
            ) {
                long dayOfCycle = (long) (daysFromStart % cycleLength);

                if (dayOfCycle < takeDays) {
                    for (LocalTime time : alarmsPerDay) {
                        allAlarms.add(LocalDateTime.of(day, time));
                    }
                }
                daysFromStart++;
            }
        }

        return allAlarms;
    }

    private void lookForSharedExceptionDto(CalculateIntakesDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("DTO не может быть null");

        if (dto.getStartDate() == null)
            throw new IllegalArgumentException("Нельзя рассчитать приемы без даты начала");

        if (dto.getEndDate() == null)
            throw new IllegalArgumentException("Нельзя рассчитать приемы без даты завершения");

        if (dto.getStartDate().isAfter(dto.getEndDate()))
            throw new IllegalArgumentException("Дата начала не может быть после даты завершения");

        long daysBetween = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
        if (daysBetween > 365 * 10)
            throw new IllegalArgumentException("Длительность схемы не может превышать 10 лет");

        if (dto.getAlarms() == null || dto.getAlarms().isEmpty())
            throw new IllegalArgumentException("Список времени напоминаний не может быть пустым");

    }

}
