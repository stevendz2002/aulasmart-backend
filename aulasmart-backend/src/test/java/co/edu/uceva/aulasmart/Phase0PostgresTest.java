package co.edu.uceva.aulasmart;

import co.edu.uceva.aulasmart.solicitudes.application.service.RevisarSolicitudService;
import co.edu.uceva.aulasmart.solicitudes.infrastructure.persistence.repository.SolicitudAulaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ejecutar únicamente contra una base descartable, nunca contra datos de usuario. */
@EnabledIfEnvironmentVariable(named = "AULASMART_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        "aulasmart.siga.sincronizacion-inicial.enabled=false",
        "aulasmart.siga.programado.cron=-",
        "spring.config.import=",
        "spring.flyway.baseline-on-migrate=false"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class Phase0PostgresTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("AULASMART_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "audit");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SolicitudAulaJpaRepository repository;
    @Autowired RevisarSolicitudService revisar;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;

    @Test
    void actualizaDesdeV7SinEliminarUsuarios() throws Exception {
        String schema = "upgrade_" + java.util.UUID.randomUUID().toString().replace("-", "");
        org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .target("7").load().migrate();
        int antes = jdbc.queryForObject("SELECT count(*) FROM " + schema + ".usuario", Integer.class);
        org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                .load().migrate();
        assertEquals(antes, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".usuario", Integer.class));
        assertEquals(3, antes);
    }

    @Test
    void endpointsExigenAutenticacion() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ESTUDIANTE")
    void estudianteNoPuedeAdministrarNiSolicitar() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/usuarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/solicitudes")
                .contentType("application/json").content("{}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @BeforeEach
    void datos() {
        jdbc.update("DELETE FROM solicitud_aula WHERE id_aula = -100");
        jdbc.update("INSERT INTO edificio (id_edificio,codigo_siga,nombre) VALUES (-100,-100,'Auditoria') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO tipo_aula (id_tipo_aula,codigo_siga,nombre) VALUES (-100,-100,'Auditoria') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO dependencia (id_dependencia,codigo_siga,nombre) VALUES (-100,-100,'Auditoria') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO aula (id_aula,codigo_siga,nombre,id_edificio,capacidad,id_tipo_aula,id_dependencia) VALUES (-100,-100,'Auditoria',-100,30,-100,-100) ON CONFLICT DO NOTHING");
    }

    private void solicitud(long id) {
        jdbc.update("INSERT INTO solicitud_aula (id_solicitud,id_usuario,id_aula,fecha_inicio,fecha_fin,motivo) VALUES (?,9001,-100,'2030-01-10 13:00Z','2030-01-10 14:00Z','Auditoria')", id);
    }

    @Test
    void migracionesAplicadasYRevisionVersionada() {
        assertEquals("9", jdbc.queryForObject("SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1", String.class));
        solicitud(-101);
        revisar.revisar(-101L, 9002L, "RECHAZAR");
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM solicitud_aula WHERE id_solicitud=-101", Long.class));
    }

    @Test
    void dosRevisoresNoSobrescribenLaMismaSolicitud() throws Exception {
        solicitud(-101);
        comprobarCarrera(-101, -101, "APROBAR", "RECHAZAR");
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM solicitud_aula WHERE id_solicitud=-101", Long.class));
    }

    @Test
    void dosSolicitudesSolapadasNoSeApruebanJuntas() throws Exception {
        solicitud(-101);
        solicitud(-102);
        comprobarCarrera(-101, -102, "APROBAR", "APROBAR");
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM solicitud_aula WHERE id_aula=-100 AND estado='APROBADA'", Integer.class));
    }

    private void comprobarCarrera(long primera, long segunda, String decision1, String decision2) throws Exception {
        var barrera = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = executor.submit(() -> decidir(primera, decision1, barrera));
            Future<Boolean> b = executor.submit(() -> decidir(segunda, decision2, barrera));
            int exitosas = (a.get(20, TimeUnit.SECONDS) ? 1 : 0) + (b.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, exitosas);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean decidir(long id, String decision, CyclicBarrier barrera) {
        try {
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                // Ambas transacciones leen la misma versión antes de competir.
                repository.findById(id).orElseThrow();
                try { barrera.await(10, TimeUnit.SECONDS); }
                catch (Exception e) { throw new IllegalStateException(e); }
                revisar.revisar(id, 9002L, decision);
            });
            return true;
        } catch (org.springframework.dao.DataAccessException | co.edu.uceva.aulasmart.solicitudes.domain.exception.ReservaSolapadaException expected) {
            return false;
        }
    }
}
