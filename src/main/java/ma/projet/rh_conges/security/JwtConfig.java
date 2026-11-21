package ma.projet.rh_conges.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** Jetons JWT signés en HMAC-SHA256 avec une clé fournie par configuration (JWT_SECRET en production). */
@Configuration
public class JwtConfig {

    private final SecretKey cle;

    public JwtConfig(@Value("${rh.jwt.secret}") String secret) {
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException("rh.jwt.secret doit faire au moins 32 octets (256 bits).");
        }
        this.cle = new SecretKeySpec(octets, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(cle).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
