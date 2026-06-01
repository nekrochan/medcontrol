package org.medcontrol.entity;

import jakarta.persistence.*;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.entity.enums.SchemeType;
import org.medcontrol.entity.keepers.AlarmKeeper;
import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "schemes")
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false, name = "profileId")
    private Profile profile;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String medicationName;

    @Column(nullable = true)
    private double dosage;

    @Column
    private String measure;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AlarmKeeper> alarmsPerDay = new ArrayList<>();

    @Column
    @Enumerated(EnumType.STRING)
    private SchemeType schemeType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SchemeStatus status;

    @Embedded
    private AlternationDaysKeeper alternationDays  = new AlternationDaysKeeper();

    @Embedded
    private WeekdaysKeeper weekdays = new WeekdaysKeeper();

    public Scheme() {
        this.schemeType = SchemeType.ALTERNATION;
        this.status = SchemeStatus.INACTIVE;
        this.weekdays = new WeekdaysKeeper(
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        this.alarmsPerDay = new ArrayList<>();
    }

    public Scheme(String name) {
        this.name = name;
        this.schemeType = SchemeType.ALTERNATION;
        this.status = SchemeStatus.INACTIVE;
        this.weekdays = new WeekdaysKeeper(
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        this.alarmsPerDay = new ArrayList<>();
    }

    public Scheme(Profile profile, String name) {
        this.profile = profile;
        this.name = name;
        this.schemeType = SchemeType.ALTERNATION;
        this.status = SchemeStatus.INACTIVE;
        this.weekdays = new WeekdaysKeeper(
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        this.alarmsPerDay = new ArrayList<>();
    }

    public UUID getId() {
        return id;
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

    public void setDosage(Double dosage) {
        this.dosage = dosage;
    }

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public SchemeType getSchemeType() {
        return schemeType;
    }

    public void setSchemeType(SchemeType schemeType) {
        this.schemeType = schemeType;
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

    public Profile getProfile() {
        return profile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SchemeStatus getStatus() {
        return status;
    }

    public void setStatus(SchemeStatus status) {
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


    public List<AlarmKeeper> getAlarmsPerDay() {
        return alarmsPerDay;
    }

    public void setAlarmsPerDay(List<AlarmKeeper> alarmsPerDay) {
        this.alarmsPerDay = alarmsPerDay;
    }

    public List<LocalTime> getAlarmTimes() {
        if (alarmsPerDay == null) return new ArrayList<>();
        return alarmsPerDay.stream()
                .map(AlarmKeeper::getAlarmTime)
                .sorted()
                .collect(Collectors.toList());
    }

    public void setAlarmTimes(List<LocalTime> times) {
        if (this.alarmsPerDay == null) {
            this.alarmsPerDay = new ArrayList<>();
        }
        this.alarmsPerDay.clear();
        if (times != null) {
            for (LocalTime time : times) {
                AlarmKeeper keeper = new AlarmKeeper(this, time);
                this.alarmsPerDay.add(keeper);
            }
        }
    }
}
