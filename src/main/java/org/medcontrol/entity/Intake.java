package org.medcontrol.entity;

import jakarta.persistence.*;
import org.medcontrol.entity.enums.IntakeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "intakes")
public class Intake {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, updatable = false, name = "schemeId")
    private Scheme scheme;

    @Column
    private LocalDateTime scheduledAt;

    @Column
    private LocalDateTime takenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntakeStatus intakeStatus;

    @Column(length = 500)
    private String description;

    public Intake() {}

    public Intake(Scheme scheme) {
        this.scheme = scheme;
        String measure = scheme.getMeasure().isEmpty() ? "" : scheme.getMeasure();
        String dosage = scheme.getDosage() == 0 ? "" : String.valueOf(scheme.getDosage());
        if (measure.isEmpty() && dosage.isEmpty())
            this.description = scheme.getMedicationName();
        else this.description = String.format("%s: %s %s", scheme.getMedicationName(), dosage, measure);
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public IntakeStatus getIntakeStatus() {
        return intakeStatus;
    }

    public void setIntakeStatus(IntakeStatus intakeStatus) {
        this.intakeStatus = intakeStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(Scheme updatedScheme) {
        if (updatedScheme.getId() == scheme.getId()) {
            String measure = updatedScheme.getMeasure().isEmpty() ? "" : updatedScheme.getMeasure();
            String dosage = updatedScheme.getDosage() == 0 ? "" : String.valueOf(updatedScheme.getDosage());

            if (measure.isEmpty() && dosage.isEmpty())
                this.description = updatedScheme.getMedicationName();
            else this.description = String.format("%s: %s %s", updatedScheme.getMedicationName(), dosage, measure);
        }
    }
}
