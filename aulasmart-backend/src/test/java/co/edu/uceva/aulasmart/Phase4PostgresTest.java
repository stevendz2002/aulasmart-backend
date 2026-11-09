package co.edu.uceva.aulasmart;

import co.edu.uceva.aulasmart.users.application.service.GestionUsuariosService;
import co.edu.uceva.aulasmart.users.infrastructure.security.SessionTokenService;
import co.edu.uceva.aulasmart.users.presentation.AuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="AULASMART_TEST_DB_URL",matches=".+")
@SpringBootTest(properties={"aulasmart.security.allow-test-accounts=false", "aulasmart.siga.sincronizacion-inicial.enabled=false",
        "aulasmart.siga.programado.cron=-", "spring.config.import=", "spring.flyway.baseline-on-migrate=false"})
@AutoConfigureMockMvc
class Phase4PostgresTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("AULASMART_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "audit");
        registry.add("spring.datasource.password", () -> "");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired GestionUsuariosService users;
    @Autowired ObjectMapper json;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired SessionTokenService sessions;
    private static final String EMAIL="jwt@phase4.example";
    private static final String PASSWORD="ClaveDePrueba123";

    @BeforeEach void setup() { users.crear("JWT prueba",EMAIL,PASSWORD,"DOCENTE"); }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM sesion_autenticacion WHERE correo=?",EMAIL);
        jdbc.update("DELETE FROM usuario WHERE correo=?",EMAIL);
    }
    private MockHttpServletRequestBuilder auth(String path) {
        return post("/api/auth/"+path).header("Origin","http://localhost:4200").header("X-AulaSmart-CSRF","1");
    }
    private MvcResult login() throws Exception {
        return mvc.perform(auth("login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("correo",EMAIL,"password",PASSWORD))))
                .andExpect(status().isOk()).andReturn();
    }
    private String access(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText(); }
    private Cookie cookie(MvcResult result) {
        String header=result.getResponse().getHeader("Set-Cookie");
        return new Cookie(AuthController.COOKIE,header.substring(header.indexOf('=')+1,header.indexOf(';')));
    }
    private ResultActions whoami(String token) throws Exception { return mvc.perform(get("/api/usuarios/whoami").header("Authorization","Bearer "+token)); }

    @Test void loginEmiteJwtYCookieProtegidaSinExponerRefreshEnJson() throws Exception {
        var login=login();
        String header=login.getResponse().getHeader("Set-Cookie");
        assertTrue(header.contains("HttpOnly")); assertTrue(header.contains("SameSite=Strict")); assertTrue(header.contains("Path=/api/auth"));
        assertEquals("no-store",login.getResponse().getHeader("Cache-Control"));
        assertFalse(json.readTree(login.getResponse().getContentAsString()).has("refreshToken"));
        assertEquals("DOCENTE",decoder.decode(access(login)).getClaimAsString("rol"));
        whoami(access(login)).andExpect(status().isOk()).andExpect(jsonPath("correo").value(EMAIL));
        String stored=jdbc.queryForObject("SELECT hash FROM token_renovacion",String.class);
        assertEquals(64,stored.length()); assertNotEquals(cookie(login).getValue(),stored);
    }

    @Test void credencialesIncorrectasCuentasInactivasYBasicSeRechazan() throws Exception {
        mvc.perform(auth("login").contentType("application/json").content(json.writeValueAsString(Map.of("correo",EMAIL,"password","incorrecta"))))
                .andExpect(status().isUnauthorized());
        jdbc.update("UPDATE usuario SET activo=false WHERE correo=?",EMAIL);
        mvc.perform(auth("login").contentType("application/json").content(json.writeValueAsString(Map.of("correo",EMAIL,"password",PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/whoami").header("Authorization","Basic "+Base64.getEncoder().encodeToString((EMAIL+":"+PASSWORD).getBytes())))
                .andExpect(status().isUnauthorized());
    }

    @Test void protegeLoginRefreshYLogoutContraCsrf() throws Exception {
        for(String path:List.of("login","refresh","logout")) {
            mvc.perform(post("/api/auth/"+path)).andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/"+path).header("Origin","http://localhost:4200")).andExpect(status().isForbidden());
            mvc.perform(post("/api/auth/"+path).header("Origin","https://evil.example").header("X-AulaSmart-CSRF","1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void rotaRefreshYReutilizacionRevocaTodaLaFamilia() throws Exception {
        var first=login(); var old=cookie(first);
        var refreshed=mvc.perform(auth("refresh").cookie(old)).andExpect(status().isOk()).andReturn();
        assertNotEquals(old.getValue(),cookie(refreshed).getValue());
        whoami(access(refreshed)).andExpect(status().isOk());
        mvc.perform(auth("refresh").cookie(old)).andExpect(status().isUnauthorized());
        mvc.perform(auth("refresh").cookie(cookie(refreshed))).andExpect(status().isUnauthorized());
        whoami(access(first)).andExpect(status().isUnauthorized());
        whoami(access(refreshed)).andExpect(status().isUnauthorized());
    }

    @Test void logoutRevocaAccessRefreshYLimpiaCookie() throws Exception {
        var login=login();
        mvc.perform(auth("logout").cookie(cookie(login))).andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));
        whoami(access(login)).andExpect(status().isUnauthorized());
        mvc.perform(auth("refresh").cookie(cookie(login))).andExpect(status().isUnauthorized());
        mvc.perform(auth("logout").cookie(cookie(login))).andExpect(status().isNoContent());
    }

    @Test void cambioDeRolYDesactivacionSeAplicanInmediatamente() throws Exception {
        var login=login();
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+access(login))).andExpect(status().isForbidden());
        jdbc.update("UPDATE usuario SET id_rol=(SELECT id_rol FROM rol WHERE nombre='ESTUDIANTE') WHERE correo=?",EMAIL);
        whoami(access(login)).andExpect(status().isOk()).andExpect(jsonPath("rol").value("ESTUDIANTE"));
        mvc.perform(get("/api/solicitudes").header("Authorization","Bearer "+access(login))).andExpect(status().isForbidden());
        jdbc.update("UPDATE usuario SET activo=false WHERE correo=?",EMAIL);
        whoami(access(login)).andExpect(status().isUnauthorized());
        mvc.perform(auth("refresh").cookie(cookie(login))).andExpect(status().isUnauthorized());
    }

    @Test void firmaExpiracionIssuerAudienceYSesionSeValidan() throws Exception {
        var login=login(); String valid=access(login);
        String[] parts=valid.split("\\.");
        String altered=parts[0]+"."+parts[1]+"."+(parts[2].charAt(0)=='A'?"B":"A")+parts[2].substring(1);
        whoami(altered).andExpect(status().isUnauthorized());
        var original=decoder.decode(valid);
        for(String invalid:List.of("exp","iss","aud","sid")) {
            var claims=new HashMap<>(original.getClaims());
            if (invalid.equals("exp")) claims.put("iat",Instant.now().minusSeconds(600));
            claims.put(invalid,switch(invalid) { case "exp" -> Instant.now().minusSeconds(120); case "aud" -> List.of("other"); case "sid" -> UUID.randomUUID().toString(); default -> "other"; });
            String token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),JwtClaimsSet.builder().claims(c -> c.putAll(claims)).build())).getTokenValue();
            whoami(token).andExpect(status().isUnauthorized());
        }
    }

    @Test void renovacionExpiradaOMalformadaNoSeAcepta() throws Exception {
        var login=login();
        jdbc.update("UPDATE sesion_autenticacion SET expira_en=now()-interval '1 second' WHERE correo=?",EMAIL);
        mvc.perform(auth("refresh").cookie(cookie(login))).andExpect(status().isUnauthorized());
        mvc.perform(auth("refresh").cookie(new Cookie(AuthController.COOKIE,"invalida"))).andExpect(status().isUnauthorized());
        mvc.perform(auth("refresh")).andExpect(status().isUnauthorized());
    }

    @Test void dosRenovacionesConcurrentesNoProducenDosSesionesValidas() throws Exception {
        String refresh=cookie(login()).getValue();
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> attempt=() -> { start.await(); try { sessions.refresh(refresh); return true; } catch(BadCredentialsException e) { return false; } };
        try {
            var a=pool.submit(attempt); var b=pool.submit(attempt); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertThrows(BadCredentialsException.class,() -> sessions.refresh(refresh));
            assertTrue(jdbc.queryForObject("SELECT revocada FROM sesion_autenticacion WHERE correo=?",Boolean.class,EMAIL));
        } finally { pool.shutdownNow(); }
    }
}
