package org.medcontrol.repository;

import org.medcontrol.entity.Profile;
import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.enums.SchemeStatus;
import org.medcontrol.entity.enums.SchemeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, UUID> {

    List<Scheme> findByProfile(Profile profile);
    List<Scheme> findByProfileId(UUID profileId);

    List<Scheme> findByName(String name);

    List<Scheme> findBySchemeType(SchemeType schemeType);

    List<Scheme> findByStartDateBetween(LocalDate start, LocalDate end);

    List<Scheme> findByProfileIdAndStartDateLessThanEqual(UUID profileId, LocalDate maxStartDate);

    List<Scheme> findByProfileIdAndEndDateGreaterThanEqual(UUID profileId, LocalDate minEndDate);

    List<Scheme> findByProfileAndSchemeType(Profile profile, SchemeType schemeType);

    List<Scheme> findByProfileIdAndStatus(UUID profileId, SchemeStatus status);

    boolean existsByName(String name);
    boolean existsByNameAndProfileId(String name, UUID profileId);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteById(UUID uuid);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void delete(Scheme entity);
}
