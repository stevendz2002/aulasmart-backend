package co.edu.uceva.aulasmart.shared.presentation;

import co.edu.uceva.aulasmart.aulas.domain.exception.AulaNoEncontradaException;
import co.edu.uceva.aulasmart.aulas.domain.exception.DisponibilidadNoVerificableException;
import co.edu.uceva.aulasmart.aulas.domain.exception.IntervaloInvalidoException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudNoEncontradaException;
import co.edu.uceva.aulasmart.solicitudes.domain.exception.SolicitudYaRevisadaException;
import co.edu.uceva.aulasmart.users.domain.exception.CorreoYaRegistradoException;
import co.edu.uceva.aulasmart.users.domain.exception.RegistroInvalidoException;
import co.edu.uceva.aulasmart.users.domain.exception.RolInvalidoException;
import co.edu.uceva.aulasmart.users.domain.exception.UsuarioNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException.class)
    ResponseEntity<ApiError> manejarAulaBot(co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> manejarRevisionConcurrente(RuntimeException exception, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La solicitud cambió mientras la revisabas. Actualiza la lista antes de continuar.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> manejarIntegridad(DataIntegrityViolationException exception, HttpServletRequest request) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof SQLException sql && "23P01".equals(sql.getSQLState())) {
                return respuesta(HttpStatus.CONFLICT, "El aula ya tiene una solicitud aprobada en ese horario.", request);
            }
            causa = causa.getCause();
        }
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con los datos existentes.", request);
    }
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

    @ExceptionHandler(CorreoYaRegistradoException.class)
    ResponseEntity<ApiError> manejarCorreoRegistrado(
            CorreoYaRegistradoException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({RegistroInvalidoException.class, RolInvalidoException.class})
    ResponseEntity<ApiError> manejarRegistroInvalido(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ResponseEntity<ApiError> manejarUsuarioNoEncontrado(
            UsuarioNoEncontradoException exception,
            HttpServletRequest request
    ) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage(), request);
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
