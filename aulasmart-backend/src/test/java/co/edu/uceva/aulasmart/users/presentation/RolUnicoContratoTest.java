package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.presentation.dto.EditarUsuarioRequest;
import co.edu.uceva.aulasmart.users.presentation.dto.RegistroUsuarioRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RolUnicoContratoTest {
    private final ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void rechazaRolesMultiplesInclusoSiTambienVieneRolSingular() {
        assertThrows(Exception.class, () -> mapper.readValue("{\"rol\":\"DOCENTE\",\"roles\":[\"ADMINISTRADOR\"]}", RegistroUsuarioRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"roles\":[\"DOCENTE\"]}", EditarUsuarioRequest.class));
        assertThrows(Exception.class, () -> mapper.readValue("{\"rol\":[\"DOCENTE\",\"ESTUDIANTE\"]}", RegistroUsuarioRequest.class));
    }

    @Test
    void editarPermiteOmitirRolPeroNoAsignarNull() throws Exception {
        assertNull(mapper.readValue("{\"nombre\":\"Nuevo nombre\"}", EditarUsuarioRequest.class).rol());
        assertThrows(Exception.class, () -> mapper.readValue("{\"rol\":null}", EditarUsuarioRequest.class));
    }
}
