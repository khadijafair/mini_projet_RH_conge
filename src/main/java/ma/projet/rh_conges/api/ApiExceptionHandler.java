package ma.projet.rh_conges.api;

import ma.projet.rh_conges.exception.DemandeCongeException;
import ma.projet.rh_conges.exception.QuotaDepasseException;
import ma.projet.rh_conges.exception.RessourceIntrouvableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Erreurs de l'API au format standard RFC 9457 (application/problem+json). */
@RestControllerAdvice(basePackages = "ma.projet.rh_conges.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final HttpStatusCode REGLE_METIER = HttpStatusCode.valueOf(422);

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail introuvable(RessourceIntrouvableException e) {
        return probleme(HttpStatus.NOT_FOUND, "Ressource introuvable", e.getMessage());
    }

    @ExceptionHandler(QuotaDepasseException.class)
    public ProblemDetail quota(QuotaDepasseException e) {
        ProblemDetail pd = probleme(REGLE_METIER, "Quota dépassé", e.getMessage());
        pd.setProperty("joursDemandes", e.getJoursDemandes());
        pd.setProperty("joursDisponibles", e.getJoursDisponibles());
        return pd;
    }

    @ExceptionHandler(DemandeCongeException.class)
    public ProblemDetail regleMetier(DemandeCongeException e) {
        return probleme(REGLE_METIER, "Règle métier non respectée", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail parametre(IllegalArgumentException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Paramètre invalide", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail interdit(AccessDeniedException e) {
        return probleme(HttpStatus.FORBIDDEN, "Accès refusé", e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail nonAuthentifie(AuthenticationException e) {
        return probleme(HttpStatus.UNAUTHORIZED, "Authentification refusée", "Identifiants invalides.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail pd = probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Certains champs sont invalides.");
        Map<String, String> erreurs = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            erreurs.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("erreurs", erreurs);
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail probleme(HttpStatusCode statut, String titre, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, detail);
        pd.setTitle(titre);
        return pd;
    }
}
