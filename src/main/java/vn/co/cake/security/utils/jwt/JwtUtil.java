package vn.co.cake.security.utils.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(ignoreResourceNotFound = true, value = "classpath:application.properties")
public class JwtUtil {
    private static final String SECRET_KEY = "qaX1lZDjBe8KRi2Hr4iSbepomV4nbzuoEaUKipiceA0BsfCzl8zUgcfmmqEdt9jp";

    public static Boolean verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                    .build(); //Reusable verifier instance
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e){
            //Invalid signature/claims
            e.printStackTrace();
            return false;
        }
    }

    public static DecodedJWT decodedJWT(String token) {
        try {
            //String payload = jwt.getPayload();
            return JWT.decode(token);
        } catch (JWTDecodeException e){
            //Invalid token
            e.printStackTrace();
        }
        return null;
    }
}
