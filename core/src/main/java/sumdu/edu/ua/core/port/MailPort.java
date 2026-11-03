package sumdu.edu.ua.core.port;

/** Outgoing mail; the web module implements it with JavaMailSender and FreeMarker. */
public interface MailPort {
  void sendVerificationEmail(String email, String token);
}
