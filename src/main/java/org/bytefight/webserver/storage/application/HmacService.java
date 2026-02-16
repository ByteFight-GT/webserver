package org.bytefight.webserver.storage.application;

import org.bytefight.webserver.storage.infra.StorageProperties;
import org.bytefight.webserver.storage.domain.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class HmacService {
    private final StorageProperties props;

    public String sign(StoredObject storedObject, long exp) {
        String data = "GET\n" + storedObject.getUuid() + "\n" + exp;

        return base64Url(hmacSha256(props.hmacSecret(), data));
    }

    public boolean verify(StoredObject storedObject, String expStr, String sig) {
        if(expStr == null || sig == null) return false;

        long exp;
        try {
            exp = Long.parseLong(expStr);
        } catch(NumberFormatException e) {
            return false;
        }

        // check expiry
        long now = Instant.now().getEpochSecond();
        if(now > exp) return false;

        // make sure the signature is actually legit
        String data = "GET\n" + storedObject.getUuid() + "\n" + exp;
        String expected = base64Url(hmacSha256(props.hmacSecret(), data));

        return constantTimeEquals(sig, expected);
    }

    private static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.US_ASCII);
        byte[] y = b.getBytes(StandardCharsets.US_ASCII);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}
