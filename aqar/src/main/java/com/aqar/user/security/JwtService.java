package com.aqar.user.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JwtService {

    private final byte[] secret;
    private final long accessTokenSeconds;
    private final ObjectMapper mapper = new ObjectMapper();

    public static class Claims {
        public Long sub;
        public String role;
        public long exp;
    }

    public JwtService(String secret, long accessTokenSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String createAccessToken(Long userId, String role) {
        long exp = Instant.now().getEpochSecond() + accessTokenSeconds;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(String.format("{\"sub\":%d,\"role\":\"%s\",\"exp\":%d}", userId, role, exp));
        String unsigned = header + "." + payload;
        String sig = base64UrlBytes(sign(unsigned));
        return unsigned + "." + sig;
    }

    public Claims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("invalid_token_format");
            String unsigned = parts[0] + "." + parts[1];
            byte[] expectedSig = sign(unsigned);
            byte[] provided = base64UrlDecode(parts[2]);
            if (!MessageDigest.isEqual(expectedSig, provided)) {
                throw new IllegalArgumentException("invalid_signature");
            }
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> map = mapper.readValue(payloadJson, Map.class);
            Number expN = (Number) map.get("exp");
            long exp = expN.longValue();
            long now = Instant.now().getEpochSecond();
            if (exp < now) throw new IllegalArgumentException("token_expired");
            Claims c = new Claims();
            c.sub = Long.valueOf(((Number)map.get("sub")).longValue());
            c.role = (String) map.get("role");
            c.exp = exp;
            return c;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlBytes(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
