package org.medcontrol.entity.keepers;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.List;

@Embeddable
public class WeekdaysKeeper {

    @Column(nullable = true)
    private boolean monday;
    @Column(nullable = true)
    private boolean tuesday;
    @Column(nullable = true)
    private boolean wednesday;
    @Column(nullable = true)
    private boolean thursday;
    @Column(nullable = true)
    private boolean friday;
    @Column(nullable = true)
    private boolean saturday;
    @Column(nullable = true)
    private boolean sunday;

    public WeekdaysKeeper() {
        this.monday = false;
        this.tuesday = false;
        this.wednesday = false;
        this.thursday = false;
        this.friday = false;
        this.saturday = false;
        this.sunday = false;
    }

    public WeekdaysKeeper(boolean monday,
                          boolean tuesday,
                          boolean wednesday,
                          boolean thursday,
                          boolean friday,
                          boolean saturday,
                          boolean sunday
    ) {
        this.monday = monday;
        this.tuesday = tuesday;
        this.wednesday = wednesday;
        this.thursday = thursday;
        this.friday = friday;
        this.saturday = saturday;
        this.sunday = sunday;
    }

    public boolean isMonday() {
        return monday;
    }

    public void setMonday(boolean monday) {
        this.monday = monday;
    }

    public boolean isTuesday() {
        return tuesday;
    }

    public void setTuesday(boolean tuesday) {
        this.tuesday = tuesday;
    }

    public boolean isWednesday() {
        return wednesday;
    }

    public void setWednesday(boolean wednesday) {
        this.wednesday = wednesday;
    }

    public boolean isThursday() {
        return thursday;
    }

    public void setThursday(boolean thursday) {
        this.thursday = thursday;
    }

    public boolean isFriday() {
        return friday;
    }

    public void setFriday(boolean friday) {
        this.friday = friday;
    }

    public boolean isSaturday() {
        return saturday;
    }

    public void setSaturday(boolean saturday) {
        this.saturday = saturday;
    }

    public boolean isSunday() {
        return sunday;
    }

    public void setSunday(boolean sunday) {
        this.sunday = sunday;
    }

    public List<Boolean> toList() {
        return List.of(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
    }

    public boolean hasAnyDaySelected() {
        return monday || tuesday || wednesday || thursday || friday || saturday || sunday;
    }

    public boolean isEveryday() {
        return monday && tuesday && wednesday && thursday && friday && saturday && sunday;
    }

    public boolean isNever() {
        return !monday && !tuesday && !wednesday && !thursday && !friday && !saturday && !sunday;
    }

    public static WeekdaysKeeper fromList(List<Boolean> weekdays) {
        if (weekdays == null || weekdays.size() != 7)
            throw new IllegalArgumentException("Список дней недели должен содержать ровно 7 элементов");

        return new WeekdaysKeeper(
                weekdays.get(0),
                weekdays.get(1),
                weekdays.get(2),
                weekdays.get(3),
                weekdays.get(4),
                weekdays.get(5),
                weekdays.get(6)
        );
    }
}
