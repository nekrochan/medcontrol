package org.medcontrol.dto.request;

import jakarta.validation.constraints.NotNull;
import org.medcontrol.entity.enums.SchemeType;
import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SchemeDateTimeRequestDto {
    private String schemeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LocalTime> alarmsPerDay = new ArrayList<>();
    @NotNull(message = "Тип схемы обязателен")
    private String schemeType;
    private String status;
    private AlternationDaysKeeper alternationDays = new AlternationDaysKeeper();
    private WeekdaysKeeper weekdays = new WeekdaysKeeper();

    public SchemeDateTimeRequestDto() {
    }

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

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

    public String getSchemeType() {
        return schemeType;
    }

    public void setSchemeType(String schemeType) {
        if (schemeType == null || schemeType.equals("UNSPECIFIED")) {
            this.schemeType = SchemeType.ALTERNATION.toString();
        } else {
            this.schemeType = schemeType;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AlternationDaysKeeper getAlternationDays() {
        return alternationDays;
    }

    public void setAlternationDays(AlternationDaysKeeper alternationDays) {
        this.alternationDays = alternationDays;
    }

    public WeekdaysKeeper getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(WeekdaysKeeper weekdays) {
        this.weekdays = weekdays;
    }

    public List<LocalTime> getAlarmsPerDay() {
        return alarmsPerDay;
    }

    public void setAlarmsPerDay(List<LocalTime> alarmsPerDay) {
        this.alarmsPerDay = alarmsPerDay;
    }
}
