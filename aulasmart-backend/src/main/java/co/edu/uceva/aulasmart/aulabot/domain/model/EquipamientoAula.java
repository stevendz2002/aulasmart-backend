package co.edu.uceva.aulasmart.aulabot.domain.model;

import co.edu.uceva.aulasmart.aulas.domain.model.Aula;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Equipamiento detectable en el catálogo real. No existe tabla de equipos:
 * SIGA registra esas dotaciones en el nombre de las aulas (por ejemplo
 * "B101-PANTALLA"). Cada grupo reúne las palabras con las que los usuarios
 * piden un equipo y los apodos con los que aparecen en el catálogo. Todo
 * vínculo se resuelve contra los datos actuales, sin migraciones ni catálogo
 * inventado; si el término no existe se pide aclarar con lo que sí aparece.
 */
public enum EquipamientoAula {
    /**paging Videobeam y proyección: en SIGA las aulas con proyector se marcan "-PANTALLA". */
    VIDEOBEAM("videobeam", "video beam", "proyector", "pantalla", "proyeccion"),
    COMPUTO("computador", "computo", "portatil", "pc"),
    AUDIO("audio", "sonido", "bocina", "parlante"),
    INTERACTIVA("interactiva", "interactivo");

    private final List<String> apodos;

    EquipamientoAula(String... apodos) {
        this.apodos = List.of(apodos);
    }

    /** El aula evidencia este grupo cuando su nombre normalizado contiene algún apodo. */
    public boolean presente(String nombreNormalizado) {
        return apodos.stream().anyMatch(nombreNormalizado::contains);
    }

    /** Traduce la palabra del usuario al grupo del catálogo; vacío si es desconocida. */
    public static Optional<EquipamientoAula> desde(String palabra) {
        var normalizada = normalizar(palabra);
        if (normalizada.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(grupo -> grupo.apodos.stream()
                        .anyMatch(apodo -> normalizada.contains(apodo) || apodo.contains(normalizada)))
                .findFirst();
    }

    /**
     * Palabras del vocabulario que realmente aparecen hoy en el catálogo, tal
     * como están escritas en los nombres de las aulas. Nunca inventa términos.
     */
    public static Set<String> observados(List<Aula> aulas) {
        var presentes = new LinkedHashSet<String>();
        for (var aula : aulas) {
            var texto = normalizar(aula.nombre());
            for (var grupo : values()) {
                grupo.apodos.stream().filter(texto::contains).findFirst().ifPresent(presentes::add);
            }
        }
        return presentes;
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
