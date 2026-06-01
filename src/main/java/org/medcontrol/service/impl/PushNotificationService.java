package org.medcontrol.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.medcontrol.entity.PushSubscriptionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${vapid.public-key}")
    private String publicKeyBase64;

    @Value("${vapid.private-key}")
    private String privateKeyBase64;

    @Value("${vapid.public-key-raw}")
    private String publicKeyRaw;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        byte[] publicKeyBytes = Base64.getUrlDecoder().decode(publicKeyBase64);
        byte[] privateKeyBytes = Base64.getUrlDecoder().decode(privateKeyBase64);

        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        log.info("Push Service initialized. Public key starts with: {}",
                publicKeyBase64.substring(0, Math.min(20, publicKeyBase64.length())));
    }

    public void sendPush(PushSubscriptionEntity sub, String title, String body) {
        try {
            String payloadText = title + "|||" + body;

            Map<String, String> subscriptionKeys = new HashMap<>();
            subscriptionKeys.put("p256dh", sub.getP256dh());
            subscriptionKeys.put("auth", sub.getAuth());

            String jwt = createVapidJwt(sub.getEndpoint());
            sendPushMessage(sub.getEndpoint(), jwt, sub);

            log.info("Push sent successfully. Title: {}, Body: {}", title, body);
        } catch (Exception e) {
            log.error("Ошибка отправки push: {}", e.getMessage(), e);
        }
    }

    private String createVapidJwt(String endpoint) throws Exception {
        URI uri = new URI(endpoint);
        String origin = uri.getScheme() + "://" + uri.getHost();

        ObjectNode header = objectMapper.createObjectNode();
        header.put("typ", "JWT");
        header.put("alg", "ES256");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("aud", origin);
        payload.put("exp", Instant.now().plusSeconds(12 * 60 * 60).getEpochSecond());
        payload.put("sub", "mailto:admin@medcontrol.local");

        String headerB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(header));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(payload));

        String unsignedToken = headerB64 + "." + payloadB64;

        Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
        signature.initSign(privateKey);
        signature.update(unsignedToken.getBytes());
        byte[] derSignature = signature.sign();

        java.security.spec.ECParameterSpec params =
                ((ECPublicKey) publicKey).getParams();
        int orderBitLength = params.getOrder().bitLength();
        int curveLength = (orderBitLength + 7) / 8 * 2;
        byte[] rawSignature = derToRaw(derSignature, curveLength);

        String signatureB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawSignature);

        return unsignedToken + "." + signatureB64;
    }

    private byte[] derToRaw(byte[] derSignature, int curveLength) {
        if (derSignature[0] != 0x30) {
            return derSignature;
        }
        int rLength = derSignature[3];
        int rOffset = 4;
        if (derSignature[rOffset] == 0x00 && rLength > 32) {
            rOffset++;
            rLength--;
        }
        byte[] r = new byte[32];
        System.arraycopy(derSignature, rOffset, r, 32 - rLength, rLength);

        int sOffset = rOffset + rLength + 1;
        int sLength = derSignature[rOffset + rLength + 1];
        sOffset++;
        if (derSignature[sOffset] == 0x00 && sLength > 32) {
            sOffset++;
            sLength--;
        }
        byte[] s = new byte[32];
        System.arraycopy(derSignature, sOffset, s, 32 - sLength, sLength);

        byte[] raw = new byte[curveLength];
        System.arraycopy(r, 0, raw, 0, 32);
        System.arraycopy(s, 0, raw, 32, 32);
        return raw;
    }

    private void sendPushMessage(String endpoint, String jwt, PushSubscriptionEntity sub) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URI(endpoint).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "vapid t=" + jwt + ", k=" + publicKeyRaw);
        conn.setRequestProperty("TTL", "60");

        conn.getOutputStream().close();

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            log.info("Empty push sent successfully");
        } else {
            log.warn("Push failed: {} - {}", responseCode, new String(conn.getErrorStream().readAllBytes()));
        }
    }
}