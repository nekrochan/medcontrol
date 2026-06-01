package org.medcontrol.entity.keepers;

import jakarta.persistence.*;
import org.medcontrol.entity.Scheme;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_alarms")
public class AlarmKeeper {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Scheme scheme;

    @Column
    private LocalTime alarmTime;

    public AlarmKeeper() {}

    public AlarmKeeper(Scheme scheme, LocalTime alarmTime) {
        this.alarmTime = alarmTime;
        this.id = UUID.randomUUID();
        this.scheme = scheme;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme scheme) {
        this.scheme = scheme;
    }

    public LocalTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalTime alarmTime) {
        this.alarmTime = alarmTime;
    }
}
