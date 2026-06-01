package org.medcontrol.scheduler;

import org.medcontrol.entity.Intake;
import org.medcontrol.entity.PushSubscriptionEntity;
import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.User;
import org.medcontrol.entity.enums.IntakeStatus;
import org.medcontrol.repository.IntakeRepository;
import org.medcontrol.repository.PushSubscriptionRepository;
import org.medcontrol.service.impl.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MedicationReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(MedicationReminderScheduler.class);

    @Autowired
    private IntakeRepository intakeRepository;

    @Autowired
    private PushNotificationService pushService;

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusSeconds(30);
        LocalDateTime end = now.plusSeconds(30);

        List<Intake> intakes = intakeRepository
                .findByIntakeStatusAndScheduledAtBetween(IntakeStatus.SCHEDULED, start, end);

        for (Intake intake : intakes) {
            try {
                Scheme scheme = intake.getScheme();
                if (scheme == null) continue;
                User user = scheme.getProfile().getUser();
                if (user == null) continue;

                String title = "⏰ Напоминание о приеме";
                String body = String.format("%s: %s",
                        scheme.getMedicationName(),
                        (scheme.getDosage() > 0 ? scheme.getDosage() + " " + scheme.getMeasure() : "принять"));

                List<PushSubscriptionEntity> subscriptions = subscriptionRepository.findByUser(user);
                for (PushSubscriptionEntity sub : subscriptions) {
                    pushService.sendPush(sub, title, body);
                }
            } catch (Exception e) {
                log.error("Ошибка при отправке напоминания для intake {}: {}", intake.getId(), e.getMessage());
            }
        }
    }
}