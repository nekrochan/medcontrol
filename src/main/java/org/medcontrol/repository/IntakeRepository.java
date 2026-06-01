package org.medcontrol.repository;

import org.medcontrol.entity.Intake;
import org.medcontrol.entity.enums.IntakeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface IntakeRepository extends JpaRepository<Intake, UUID> {

    List<Intake> findBySchemeId(UUID schemeId);

    boolean existsBySchemeId(UUID schemeId);

    List<Intake> findBySchemeIdAndScheduledAtBetween(UUID schemeId, LocalDateTime start, LocalDateTime end);

    List<Intake> findByScheduledAtBeforeAndIntakeStatus(LocalDateTime dateTime, IntakeStatus status);

    List<Intake> findByIntakeStatusAndScheduledAtBetween(IntakeStatus status, LocalDateTime start, LocalDateTime end);

    List<Intake> findBySchemeIdAndScheduledAtAfter(UUID schemeId, LocalDateTime start);

    List<Intake> findBySchemeIdAndIntakeStatus(UUID schemeId, IntakeStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteBySchemeId(UUID schemeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteBySchemeIdAndScheduledAtAfter(UUID schemeId, LocalDateTime dateTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteBySchemeIdAndIntakeStatus(UUID schemeId, IntakeStatus intakeStatus);

    int countBySchemeIdAndIntakeStatus(UUID id, IntakeStatus intakeStatus);

    boolean existsBySchemeIdAndScheduledAt(UUID id, LocalDateTime dateTime);

    int countBySchemeIdAndScheduledAtAfter(UUID id, LocalDateTime now);

    int countByScheduledAtBeforeAndIntakeStatus(LocalDateTime outOfNearest, IntakeStatus intakeStatus);
}
