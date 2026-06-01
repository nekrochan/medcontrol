package org.medcontrol.repository;

import org.medcontrol.entity.PushSubscriptionEntity;
import org.medcontrol.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {
    List<PushSubscriptionEntity> findByUser(User user);
    void deleteByUser(User user);
    void deleteByEndpoint(String endpoint);
    boolean existsByEndpoint(String endpoint);
}