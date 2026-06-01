package org.medcontrol.dto.response;

import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SchemeResponseDto {
    private String id;
    private String profileId;
    private String name;
    private String medicationName;
    private double dosage;
    private String measure;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<LocalTime> alarmTimes = new ArrayList<>();
    private String schemeType;
    private String status;
    private AlternationDaysKeeper alternationDays = new AlternationDaysKeeper();
    private WeekdaysKeeper weekdays = new WeekdaysKeeper();

    public SchemeResponseDto() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public double getDosage() {
        return dosage;
    }

    public void setDosage(double dosage) {
        this.dosage = dosage;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
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

    public List<LocalTime> getAlarmTimes() {
        if (alarmTimes != null) return alarmTimes;
        return new ArrayList<>();
    }

    public void setAlarmTimes(List<LocalTime> alarmTimes) {
        this.alarmTimes = alarmTimes;
    }

    public String getSchemeType() {
        return schemeType;
    }

    public void setSchemeType(String schemeType) {
        this.schemeType = schemeType;
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
        if (weekdays != null) return weekdays;
        return new WeekdaysKeeper(
                false, false, false,
                false, false, false, false
        );
    }

    public void setWeekdays(WeekdaysKeeper weekdays) {
        this.weekdays = weekdays;
    }

}
