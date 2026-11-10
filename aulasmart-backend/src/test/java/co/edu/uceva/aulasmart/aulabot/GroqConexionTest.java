package co.edu.uceva.aulasmart.aulabot;

import co.edu.uceva.aulasmart.aulabot.domain.model.SolicitudInterpretacion;
import co.edu.uceva.aulasmart.aulabot.infrastructure.GroqInterpretador;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: tres consultas sintéticas e independientes, sin catálogo ni datos personales. No necesita base de datos. */
@EnabledIfEnvironmentVariable(named="AULASMART_GROQ_SMOKE",matches="true")
class GroqConexionTest {
    @Test void interpretaConsultaRealConCuentaFree() {
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer()).run(context -> {
            var env=context.getEnvironment();
            var adapter=new GroqInterpretador(env.getRequiredProperty("aulasmart.aulabot.groq-key"),true,new ObjectMapper(),Clock.systemUTC());
            var zona=ZoneId.of("America/Bogota");
            var result=adapter.interpretar(new SolicitudInterpretacion(
                    "Necesito un aula para 40 personas mañana de 08:00 a 10:00 en el bloque B",null,null,null));
            assertTrue(result.estaCompleta());
            assertEquals(40,result.consulta().filtro().capacidadMinima());
            assertEquals(LocalDate.now(zona).plusDays(1),result.consulta().inicio().atZone(zona).toLocalDate());
            // Segundo turno: el servidor entrega la consulta vigente; el modelo conserva lo no modificado.
            var followup=adapter.interpretar(new SolicitudInterpretacion("Mejor para 50 personas",result.consulta(),null,null));
            assertTrue(followup.estaCompleta(),followup.aclaracion());
            assertEquals(50,followup.consulta().filtro().capacidadMinima());
            assertEquals(result.consulta().inicio(),followup.consulta().inicio());
            assertEquals(result.consulta().fin(),followup.consulta().fin());
            // Cambio de horario sin fin explícito exige aclarar, no inventa franja.
            var ambiguous=adapter.interpretar(new SolicitudInterpretacion("Mejor a las 10",result.consulta(),null,null));
            assertFalse(ambiguous.estaCompleta());
        });
    }
}
