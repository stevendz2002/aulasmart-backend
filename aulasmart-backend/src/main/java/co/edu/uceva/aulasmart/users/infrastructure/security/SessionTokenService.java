package co.edu.uceva.aulasmart.users.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import co.edu.uceva.aulasmart.users.application.port.ConsultaUsuariosPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Infraestructura de sesión: los secretos de renovación solo se persisten como SHA-256. */
@Service
public class SessionTokenService {
    private final JdbcTemplate jdbc;
    private final JpaUserDetailsService users;
    private final ConsultaUsuariosPort profiles;
    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;
    private final SecureRandom random = new SecureRandom();

    public SessionTokenService(JdbcTemplate jdbc, JpaUserDetailsService users, ConsultaUsuariosPort profiles,
            JwtEncoder encoder, Clock clock, @Value("${aulasmart.auth.access-ttl:PT15M}") Duration accessTtl,
            @Value("${aulasmart.auth.refresh-ttl:P7D}") Duration refreshTtl,
            @Value("${aulasmart.auth.issuer}") String issuer) {
        this.jdbc=jdbc; this.users=users; this.profiles=profiles; this.encoder=encoder; this.clock=clock;
        this.accessTtl=accessTtl; this.refreshTtl=refreshTtl; this.issuer=issuer;
        if (accessTtl.isNegative() || accessTtl.isZero() || accessTtl.compareTo(Duration.ofHours(1)) > 0
                || refreshTtl.compareTo(accessTtl) <= 0 || refreshTtl.compareTo(Duration.ofDays(30)) > 0)
            throw new IllegalStateException("Duración de sesión inválida (access <= 1 hora, refresh <= 30 días).");
    }

    public record Tokens(String accessToken, String correo, String nombre, String rol, long expiresIn,
                         String refreshToken, Instant refreshExpiresAt) {}
    private record Session(UUID id, String correo, Instant expires, boolean revoked) {}

    @Transactional
    public Tokens login(UserDetails user, String previousCookie) {
        logout(previousCookie);
        UUID id=UUID.randomUUID();
        Instant expires=clock.instant().plus(refreshTtl);
        jdbc.update("INSERT INTO sesion_autenticacion(id,correo,expira_en) VALUES (?,?,?)", id, user.getUsername(), Timestamp.from(expires));
        return issue(new Session(id,user.getUsername(),expires,false), user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Tokens refresh(String cookie) {
        String[] parts = parts(cookie);
        Session session = find(UUID.fromString(parts[0]), true);
        if (session.revoked() || !session.expires().isAfter(clock.instant())) throw invalid();
        var used = jdbc.query("SELECT usado FROM token_renovacion WHERE hash=? AND sesion_id=?",
                (rs,n) -> rs.getBoolean(1), hash(cookie), session.id());
        if (used.isEmpty()) throw invalid();
        if (used.get(0)) {
            revoke(session.id()); // Reutilización: invalidar la familia y los access ya emitidos.
            throw invalid();
        }
        UserDetails user;
        try { user=users.loadUserByUsername(session.correo()); }
        catch (org.springframework.security.core.AuthenticationException e) { revoke(session.id()); throw invalid(); }
        jdbc.update("UPDATE token_renovacion SET usado=TRUE WHERE hash=?", hash(cookie));
        return issue(session,user);
    }

    @Transactional
    public void logout(String cookie) {
        if (cookie==null || cookie.isBlank()) return;
        try {
            var p=parts(cookie);
            var session=find(UUID.fromString(p[0]),true);
            Integer count=jdbc.queryForObject("SELECT count(*) FROM token_renovacion WHERE hash=? AND sesion_id=?", Integer.class, hash(cookie),session.id());
            if (count!=null && count>0) revoke(session.id());
        } catch (BadCredentialsException ignored) { /* Cierre idempotente con cookie inválida. */ }
    }

    @Transactional(readOnly=true)
    public UserDetails authenticate(Jwt jwt) {
        try {
            Session session=find(UUID.fromString(jwt.getClaimAsString("sid")),false);
            if (session.revoked() || !session.expires().isAfter(clock.instant()) || !session.correo().equals(jwt.getSubject())) throw invalid();
            return users.loadUserByUsername(session.correo()); // Permisos actuales, sin roles obsoletos.
        } catch (IllegalArgumentException | NullPointerException | org.springframework.security.core.userdetails.UsernameNotFoundException e) { throw invalid(); }
    }

    private Tokens issue(Session session, UserDetails user) {
        byte[] secret=new byte[32]; random.nextBytes(secret);
        String refresh=session.id()+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        jdbc.update("INSERT INTO token_renovacion(hash,sesion_id) VALUES (?,?)",hash(refresh),session.id());
        var profile=profiles.buscarPorCorreo(user.getUsername()).orElseThrow(SessionTokenService::invalid);
        String role=user.getAuthorities().iterator().next().getAuthority().replaceFirst("^ROLE_", "");
        Instant now=clock.instant();
        var claims=JwtClaimsSet.builder().issuer(issuer).subject(user.getUsername()).audience(List.of("aulasmart-api"))
                .issuedAt(now).expiresAt(now.plus(accessTtl)).id(UUID.randomUUID().toString())
                .claim("sid",session.id().toString()).claim("rol",role).build();
        String access=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Tokens(access,user.getUsername(),profile.nombre(),role,accessTtl.toSeconds(),refresh,session.expires());
    }

    private Session find(UUID id, boolean lock) {
        var rows=jdbc.query("SELECT id,correo,expira_en,revocada FROM sesion_autenticacion WHERE id=?"+(lock?" FOR UPDATE":""),
                (rs,n) -> new Session(rs.getObject(1,UUID.class),rs.getString(2),rs.getTimestamp(3).toInstant(),rs.getBoolean(4)),id);
        if(rows.isEmpty()) throw invalid();
        return rows.get(0);
    }
    private void revoke(UUID id) { jdbc.update("UPDATE sesion_autenticacion SET revocada=TRUE WHERE id=?",id); }
    private static String[] parts(String cookie) {
        if(cookie==null || !cookie.matches("[a-f0-9-]{36}\\.[A-Za-z0-9_-]{43}")) throw invalid();
        String[] parts=cookie.split("\\.",2);
        try { UUID.fromString(parts[0]); } catch (IllegalArgumentException e) { throw invalid(); }
        return parts;
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static BadCredentialsException invalid() { return new BadCredentialsException("Sesión inválida o vencida."); }

    @Scheduled(cron="0 0 3 * * *",zone="America/Bogota")
    @Transactional
    public void cleanup() { jdbc.update("DELETE FROM sesion_autenticacion WHERE expira_en < ?",Timestamp.from(clock.instant())); }
}
