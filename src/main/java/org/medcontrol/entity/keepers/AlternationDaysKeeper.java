package org.medcontrol.entity.keepers;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AlternationDaysKeeper {

    @Column(name = "intake_days_number")
    private Integer takeDays;

    @Column(name = "skip_days_number")
    private Integer skipDays;

    public AlternationDaysKeeper() {
        this.takeDays = 1;
        this.skipDays = 0;
    }

    public AlternationDaysKeeper(int takeDays, int skipDays) {
        this.takeDays = takeDays;
        this.skipDays = skipDays;
    }

    public Integer getTakeDays() {
        return takeDays;
    }

    public void setTakeDays(int takeDays) {
        this.takeDays = takeDays;
    }

    public Integer getSkipDays() {
        return skipDays;
    }

    public void setSkipDays(int skipDays) {
        this.skipDays = skipDays;
    }
}
