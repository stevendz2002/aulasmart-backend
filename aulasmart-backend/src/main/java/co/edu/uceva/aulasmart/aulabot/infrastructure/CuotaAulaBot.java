package co.edu.uceva.aulasmart.aulabot.infrastructure;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import co.edu.uceva.aulasmart.aulabot.domain.model.AulaBotNoDisponibleException;

/** Límite conservador de instancia. El límite real de la cuenta también aplica en Groq. */
@Component
public class CuotaAulaBot {
    private final Clock clock;
    private final int minuteLimit;
    private final int userMinuteLimit;
    private final int dayLimit;
    private final Deque<Instant> recent=new ArrayDeque<>();
    private LocalDate day;
    private int used;
    private final Map<String,Deque<Instant>> users=new HashMap<>();
    public CuotaAulaBot(Clock clock) { this(clock,20,6,50); }
    @org.springframework.beans.factory.annotation.Autowired
    public CuotaAulaBot(Clock clock,
            @org.springframework.beans.factory.annotation.Value("${aulasmart.aulabot.minute-limit:20}") int minuteLimit,
            @org.springframework.beans.factory.annotation.Value("${aulasmart.aulabot.user-minute-limit:6}") int userMinuteLimit,
            @org.springframework.beans.factory.annotation.Value("${aulasmart.aulabot.day-limit:50}") int dayLimit) {
        if(minuteLimit<1 || minuteLimit>30 || userMinuteLimit<1 || userMinuteLimit>10 || dayLimit<1 || dayLimit>1000)
            throw new IllegalArgumentException("Cuota AulaBot fuera de los límites permitidos.");
        this.clock=clock;this.minuteLimit=minuteLimit;this.userMinuteLimit=userMinuteLimit;this.dayLimit=dayLimit;
    }
    public synchronized void consumir(String user) {
        Instant now=clock.instant(); LocalDate today=LocalDate.now(clock);
        if(!today.equals(day)) { day=today; used=0; users.clear(); }
        while(!recent.isEmpty() && !recent.peekFirst().isAfter(now.minusSeconds(60))) recent.removeFirst();
        if(recent.size()>=minuteLimit || used>=dayLimit) throw new AulaBotNoDisponibleException();
        var userRecent=users.computeIfAbsent(user,k -> new ArrayDeque<>());
        while(!userRecent.isEmpty() && !userRecent.peekFirst().isAfter(now.minusSeconds(60))) userRecent.removeFirst();
        if(userRecent.size()>=userMinuteLimit) throw new AulaBotNoDisponibleException();
        userRecent.addLast(now);
        recent.addLast(now); used++;
    }
}
