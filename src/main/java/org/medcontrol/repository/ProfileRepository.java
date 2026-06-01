package org.medcontrol.repository;

import org.medcontrol.entity.Profile;
import org.medcontrol.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findByUser(User user);

    List<Profile> findByUserId(UUID userId);

    Optional<Profile> findByUserAndIsDefaultTrue(User user);

    Optional<Profile> findByUserIdAndIsDefaultTrue(UUID userId);

    List<Profile> findByName(String name);

    boolean existsByName(String name);

    boolean  existsByNameAndUserId(String name, UUID userId);

    long countByUserId(UUID userId);

    List<Profile> findByUserIdOrderByIsDefaultDescNameAsc(UUID userId);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteById(UUID uuid);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void delete(Profile entity);
}