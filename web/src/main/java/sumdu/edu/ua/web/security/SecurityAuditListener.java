package sumdu.edu.ua.web.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** Security audit: every login, failed login, logout and denied access goes to SECURITY_AUDIT. */
@Component
public class SecurityAuditListener {
  public static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

  @EventListener
  public void onLoginSuccess(AuthenticationSuccessEvent event) {
    Authentication auth = event.getAuthentication();
    AUDIT.info("LOGIN_SUCCESS user={} roles={} ip={}", auth.getName(), auth.getAuthorities(),
        ip(auth));
  }

  @EventListener
  public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
    Authentication auth = event.getAuthentication();
    AUDIT.warn("LOGIN_FAILURE user={} reason={} ip={}", auth.getName(),
        event.getException().getClass().getSimpleName(), ip(auth));
  }

  @EventListener
  public void onLogout(LogoutSuccessEvent event) {
    AUDIT.info("LOGOUT user={} ip={}", event.getAuthentication().getName(),
        ip(event.getAuthentication()));
  }

  @EventListener
  public void onAccessDenied(AuthorizationDeniedEvent<?> event) {
    Authentication auth = event.getAuthentication().get();
    Object target = event.getSource();
    String resource = target instanceof HttpServletRequest r
        ? r.getMethod() + " " + r.getRequestURI()
        : target instanceof MethodInvocation m
            ? m.getMethod().getDeclaringClass().getSimpleName() + "." + m.getMethod().getName()
            : String.valueOf(target);
    AUDIT.warn("ACCESS_DENIED user={} resource={}", auth.getName(), resource);
  }

  private static String ip(Authentication auth) {
    return auth != null && auth.getDetails() instanceof WebAuthenticationDetails d
        ? d.getRemoteAddress() : "-";
  }
}
