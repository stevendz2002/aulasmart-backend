package co.edu.uceva.aulasmart.aulabot.domain.model;

/**
 * Filtros comprensibles por la conversación, todavía independientes de los
 * identificadores del catálogo. La resolución contra datos reales se hará en
 * la integración con aulas, nunca consultando al proveedor de lenguaje.
 * El equipamiento se guarda tal como lo escribió el usuario y se traduce
 * después contra los nombres reales del catálogo.
 */
public record FiltroAulaBot(
        Integer capacidadMinima,
        String edificio,
        String tipoAula,
        String dependencia,
        String nombre,
        String equipamiento
) {
    public FiltroAulaBot(Integer capacidadMinima, String edificio, String tipoAula) {
        this(capacidadMinima, edificio, tipoAula, null, null, null);
    }
    public FiltroAulaBot(Integer capacidadMinima, String edificio, String tipoAula, String dependencia, String nombre) {
        this(capacidadMinima, edificio, tipoAula, dependencia, nombre, null);
    }
    public FiltroAulaBot {
        if (capacidadMinima != null && capacidadMinima < 1) {
            throw new IllegalArgumentException("La capacidad mínima debe ser un entero positivo.");
        }
        edificio = limpiar(edificio);
        tipoAula = limpiar(tipoAula);
        dependencia = limpiar(dependencia);
        nombre = limpiar(nombre);
        equipamiento = limpiar(equipamiento);
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
