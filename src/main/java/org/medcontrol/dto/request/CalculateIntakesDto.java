package org.medcontrol.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CalculateIntakesDto {
    private List<Boolean> weekdays;
    private int takeDays;
    private int skipDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LocalTime> alarms;

    public CalculateIntakesDto() {}

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<LocalTime> getAlarms() {
        return alarms;
    }

    public void setAlarms(List<LocalTime> alarms) {
        this.alarms = alarms;
    }

    public int getTakeDays() {
        return takeDays;
    }

    public void setTakeDays(int takeDays) {
        this.takeDays = takeDays;
    }

    public int getSkipDays() {
        return skipDays;
    }

    public void setSkipDays(int skipDays) {
        this.skipDays = skipDays;
    }

    public List<Boolean> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(List<Boolean> weekdays) {
        this.weekdays = weekdays;
    }
}
