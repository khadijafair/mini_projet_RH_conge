package ma.projet.rh_conges.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.projet.rh_conges.api.dto.JetonReponse;
import ma.projet.rh_conges.api.dto.JetonRequete;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    @Value("${rh.jwt.duree-secondes:3600}")
    private long dureeSecondes;

    @PostMapping("/token")
    @SecurityRequirements
    @Operation(summary = "Obtenir un jeton JWT", description = "Identifiants du compte ; jeton valable 1 heure.")
    public JetonReponse token(@Valid @RequestBody JetonRequete requete) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(requete.username(), requete.password()));
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        Instant maintenant = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("rh-conges")
                .issuedAt(maintenant)
                .expiresAt(maintenant.plusSeconds(dureeSecondes))
                .subject(auth.getName())
                .claim("roles", roles)
                .build();
        String jeton = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new JetonReponse(jeton, "Bearer", dureeSecondes);
    }
}
