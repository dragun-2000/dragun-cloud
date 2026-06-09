package vn.co.cake.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.config.VietQrProperties;

@Service
public class VietQrPartnerTokenService {

    private final VietQrProperties vietQrProperties;

    public VietQrPartnerTokenService(VietQrProperties vietQrProperties) {
        this.vietQrProperties = vietQrProperties;
    }

    public boolean isValidBasicCredentials(String authorizationHeader) {
        if (StringUtils.isEmpty(authorizationHeader) || !authorizationHeader.startsWith("Basic ")) {
            return false;
        }
        String base64Credentials = authorizationHeader.substring("Basic ".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] values = credentials.split(":", 2);
        if (values.length != 2) {
            return false;
        }
        return vietQrProperties.getPartner().getUsername().equals(values[0])
                && vietQrProperties.getPartner().getPassword().equals(values[1]);
    }

    public String generateBearerToken() {
        Algorithm algorithm = Algorithm.HMAC512(vietQrProperties.getPartner().getJwtSecret().getBytes(StandardCharsets.UTF_8));
        Date expiresAt = new Date(System.currentTimeMillis() + PaymentConstants.VIETQR_TOKEN_EXPIRES_SECONDS * 1000L);
        return JWT.create()
                .withSubject(vietQrProperties.getPartner().getUsername())
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public boolean isValidBearerToken(String authorizationHeader) {
        if (StringUtils.isEmpty(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        try {
            Algorithm algorithm = Algorithm.HMAC512(vietQrProperties.getPartner().getJwtSecret().getBytes(StandardCharsets.UTF_8));
            JWT.require(algorithm).build().verify(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
