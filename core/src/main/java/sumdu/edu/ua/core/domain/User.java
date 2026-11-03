package sumdu.edu.ua.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Account of the catalog. email is the login, password holds a BCrypt hash, never plain text. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "verificationToken"})
public class User {
  /** Registered, e-mail not confirmed yet: the account is disabled for login. */
  public static final String ROLE_REGISTERED = "REGISTERED";
  /** Reader: views books and comments, writes comments. */
  public static final String ROLE_USER = "USER";
  /** Full access: book CRUD, deleting comments, users and roles, API writes. */
  public static final String ROLE_ADMIN = "ADMIN";

  private long id;
  private String email;
  private String password;
  private String nickname;
  private String role;
  private boolean verified;
  private String verificationToken;
}
