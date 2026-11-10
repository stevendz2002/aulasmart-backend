package co.edu.uceva.aulasmart.aulabot.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Franja parcial extraída de la conversación: cada componente puede faltar aún. */
public record FranjaAulaBot(LocalDate fecha, LocalTime inicio, LocalTime fin) {
    public boolean completa() {
        return fecha != null && inicio != null && fin != null;
    }

    public boolean vacia() {
        return fecha == null && inicio == null && fin == null;
    }
}
