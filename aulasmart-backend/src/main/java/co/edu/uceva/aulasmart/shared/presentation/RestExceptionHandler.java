package co.edu.uceva.aulasmart.shared.presentation;

import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.exception.IntervaloInvalidoException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudNoEncontradaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudYaRevisadaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(AulaNoEncontradaException.class)
    ResponseEntity<ApiError> manejarNoEncontrada(
            AulaNoEncontradaException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    ResponseEntity<ApiError> manejarSolicitudNoEncontrada(
            SolicitudNoEncontradaException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SolicitudYaRevisadaException.class)
    ResponseEntity<ApiError> manejarSolicitudYaRevisada(
            SolicitudYaRevisadaException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ReservaSolapadaException.class)
    ResponseEntity<ApiError> manejarReservaSolapada(
            ReservaSolapadaException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DisponibilidadNoVerificableException.class)
    ResponseEntity<ApiError> manejarNoVerificable(
            DisponibilidadNoVerificableException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler({IntervaloInvalidoException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> manejarArgumentoInvalido(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> manejarSolicitudInvalida(
            Exception exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, "Los parámetros de la consulta son inválidos o incompletos.", request);
    }

    private ResponseEntity<ApiError> respuesta(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        var error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
