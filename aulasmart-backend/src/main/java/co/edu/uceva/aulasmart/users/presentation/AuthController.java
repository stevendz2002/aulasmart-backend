package co.edu.uceva.aulasmart.users.presentation;

import co.edu.uceva.aulasmart.users.infrastructure.security.SessionTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String COOKIE = "aulasmart_refresh";
    private final AuthenticationManager authentication;
    private final SessionTokenService sessions;
    private final boolean secureCookie;
    private final Clock clock;

    public AuthController(AuthenticationManager authentication, SessionTokenService sessions, Clock clock,
            @Value("${aulasmart.auth.secure-cookie:true}") boolean secureCookie) {
        this.authentication=authentication; this.sessions=sessions; this.clock=clock; this.secureCookie=secureCookie;
    }

    public record Login(@NotBlank @Email @Size(max=254) String correo, @NotBlank @Size(max=200) String password) {}
    public record Access(String accessToken, String tokenType, long expiresIn, String correo, String nombre, String rol) {}

    @PostMapping("/login")
    public ResponseEntity<Access> login(@Valid @RequestBody Login request,
            @CookieValue(name=COOKIE, required=false) String previous) {
        var principal=authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.correo().trim(), request.password()));
        return response(sessions.login((UserDetails)principal.getPrincipal(), previous));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Access> refresh(@CookieValue(name=COOKIE, required=false) String cookie) {
        return response(sessions.refresh(cookie));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name=COOKIE, required=false) String cookie) {
        sessions.logout(cookie);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("",Duration.ZERO).toString())
                .header(HttpHeaders.CACHE_CONTROL,"no-store").build();
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String,String>> invalid(AuthenticationException ignored) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .body(Map.of("message","Credenciales incorrectas o sesión vencida."));
    }

    private ResponseEntity<Access> response(SessionTokenService.Tokens tokens) {
        var body=new Access(tokens.accessToken(),"Bearer",tokens.expiresIn(),tokens.correo(),tokens.nombre(),tokens.rol());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE,cookie(tokens.refreshToken(),Duration.between(clock.instant(),tokens.refreshExpiresAt())).toString()).body(body);
    }
    private ResponseCookie cookie(String value,Duration age) {
        return ResponseCookie.from(COOKIE,value).httpOnly(true).secure(secureCookie).sameSite("Strict")
                .path("/api/auth").maxAge(age).build();
    }
}
