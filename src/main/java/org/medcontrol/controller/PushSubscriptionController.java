package org.medcontrol.controller;

import org.medcontrol.entity.PushSubscriptionEntity;
import org.medcontrol.entity.User;
import org.medcontrol.repository.PushSubscriptionRepository;
import org.medcontrol.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionController.class);

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserServiceImpl userService;

    @Value("${vapid.public-key-raw}")
    private String publicKey;

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestBody Map<String, Object> subscription) {
        String endpoint = (String) subscription.get("endpoint");
        Map<String, String> keys = (Map<String, String>) subscription.get("keys");
        String p256dh = keys.get("p256dh");
        String auth = keys.get("auth");

        if (endpoint == null || p256dh == null || auth == null) {
            return ResponseEntity.badRequest().body("Invalid subscription data");
        }

        User user = userService.getUser(userDetails.getUsername());

        if (!subscriptionRepository.existsByEndpoint(endpoint)) {
            PushSubscriptionEntity entity = new PushSubscriptionEntity(endpoint, p256dh, auth, user);
            subscriptionRepository.save(entity);
            log.info("Сохранена подписка для пользователя {}", user.getUsername());
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subscriptionRepository.deleteByEndpoint(endpoint);
        }
        return ResponseEntity.ok().build();
    }
}