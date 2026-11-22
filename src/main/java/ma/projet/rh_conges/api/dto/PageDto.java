package ma.projet.rh_conges.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Page de résultats au format stable (indépendant de l'implémentation Spring Data). */
public record PageDto<T>(List<T> contenu, int page, int taille, long totalElements, int totalPages) {

    public static <E, T> PageDto<T> of(Page<E> page, Function<E, T> conversion) {
        return new PageDto<>(page.getContent().stream().map(conversion).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
