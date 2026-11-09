package co.edu.uceva.aulasmart;

import co.edu.uceva.aulasmart.users.application.service.GestionUsuariosService;
import co.edu.uceva.aulasmart.users.domain.exception.RegistroInvalidoException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@EnabledIfEnvironmentVariable(named = "AULASMART_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {"aulasmart.security.allow-test-accounts=false", "aulasmart.siga.sincronizacion-inicial.enabled=false", "aulasmart.siga.programado.cron=-", "spring.config.import=", "spring.flyway.baseline-on-migrate=false"})
@AutoConfigureMockMvc
class Phase2PostgresTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("AULASMART_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "audit");
        registry.add("spring.datasource.password", () -> "");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired GestionUsuariosService usuarios;
    @Autowired MockMvc mvc;

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM usuario WHERE correo LIKE '%@phase2.example'");
    }

    @Test
    void migracionAmbiguaSeDetieneSinAlterarUsuariosNiAsociaciones() {
        for (boolean multiples : new boolean[]{false, true}) {
            String schema = "roles_" + UUID.randomUUID().toString().replace("-", "");
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("8").load().migrate();
            if (multiples) jdbc.update("INSERT INTO " + schema + ".usuario_rol VALUES (9001,4)");
            else jdbc.update("DELETE FROM " + schema + ".usuario_rol WHERE id_usuario=9001");
            assertThrows(org.flywaydb.core.api.FlywayException.class,
                    () -> Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate());
            assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".usuario", Integer.class));
            assertEquals(multiples ? 2 : 0, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".usuario_rol WHERE id_usuario=9001", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema=? AND table_name='usuario' AND column_name='id_rol'", Integer.class, schema));
        }
    }

    @Test
    void mapeoExplicitoResuelveAmbiguedadYConservaAsociaciones() {
        String schema = "resuelto_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("8").load().migrate();
        jdbc.update("INSERT INTO " + schema + ".usuario_rol VALUES (9001,4)");
        jdbc.execute("CREATE TABLE " + schema + ".usuario_rol_resolucion_v9 (id_usuario BIGINT PRIMARY KEY REFERENCES " + schema + ".usuario(id_usuario), id_rol SMALLINT NOT NULL REFERENCES " + schema + ".rol(id_rol), motivo TEXT NOT NULL CHECK(length(btrim(motivo))>0))");
        jdbc.update("INSERT INTO " + schema + ".usuario_rol_resolucion_v9 VALUES (9001,4,'Asignacion explicita de prueba')");
        Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
        assertEquals(4, jdbc.queryForObject("SELECT id_rol FROM " + schema + ".usuario WHERE id_usuario=9001", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".usuario_rol_historial_v9 WHERE id_usuario=9001", Integer.class));
    }

    @Test
    void baseExigeRolValidoYConservaHistorial() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE usuario SET id_rol=NULL WHERE id_usuario=9001"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE usuario SET id_rol=32000 WHERE id_usuario=9001"));
        assertEquals(3, jdbc.queryForObject("SELECT count(*) FROM usuario_rol_historial_v9", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT id_rol FROM usuario WHERE id_usuario=9001", Integer.class));
    }

    @Test
    void apiCreaEditaYRechazaContratoMultiple() throws Exception {
        var admin = user("admin@prueba.aulasmart.local").roles("ADMINISTRADOR");
        mvc.perform(post("/api/usuarios").with(admin).contentType("application/json")
                .content("{\"nombre\":\"Prueba\",\"correo\":\"api@phase2.example\",\"clave\":\"claveSegura1\",\"rol\":\"DOCENTE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("rol").value("DOCENTE"))
                .andExpect(jsonPath("roles").doesNotExist());
        Long id = jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE correo='api@phase2.example'", Long.class);
        mvc.perform(patch("/api/usuarios/" + id).with(admin).contentType("application/json").content("{\"nombre\":\"Editado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("rol").value("DOCENTE"));
        mvc.perform(patch("/api/usuarios/" + id).with(admin).contentType("application/json").content("{\"rol\":\"ESTUDIANTE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("rol").value("ESTUDIANTE"));
        for (String body : new String[]{"{\"roles\":[\"DOCENTE\"]}", "{\"rol\":null}", "{\"rol\":[]}", "{\"rol\":\"\"}"}) {
            mvc.perform(patch("/api/usuarios/" + id).with(admin).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void soloAdministradorAccedeAlDirectorio() throws Exception {
        for (String rol : new String[]{"DOCENTE", "ESTUDIANTE", "ADMINISTRATIVO"}) {
            mvc.perform(get("/api/usuarios").with(user("prueba").roles(rol))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/usuarios").with(user("prueba").roles("ADMINISTRADOR"))).andExpect(status().isOk());
    }

    @Test
    void concurrenciaConservaUnAdministradorHabilitado() throws Exception {
        var primero = usuarios.crear("Admin Uno", "uno@phase2.example", "claveSegura1", "ADMINISTRADOR");
        var segundo = usuarios.crear("Admin Dos", "dos@phase2.example", "claveSegura1", "ADMINISTRADOR");
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> degradar(primero.id(), inicio));
            var b = executor.submit(() -> degradar(segundo.id(), inicio));
            inicio.countDown();
            assertEquals(1, a.get(20, TimeUnit.SECONDS) + b.get(20, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuario u JOIN rol r ON r.id_rol=u.id_rol WHERE u.correo LIKE '%@phase2.example' AND u.activo AND r.nombre='ADMINISTRADOR'", Integer.class));
        } finally { executor.shutdownNow(); }
    }

    private int degradar(Long id, CountDownLatch inicio) throws Exception {
        inicio.await(10, TimeUnit.SECONDS);
        try { usuarios.editar(id, null, "DOCENTE", null, 9003L); return 1; }
        catch (RegistroInvalidoException expected) { return 0; }
    }
}
