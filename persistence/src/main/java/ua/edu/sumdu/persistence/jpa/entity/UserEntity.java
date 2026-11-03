package ua.edu.sumdu.persistence.jpa.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Account for Spring Security: email login, BCrypt password, role, e-mail confirmation. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA.
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 128)
  private String email;

  @Column(name = "password", length = 255)
  private String password; // BCrypt hash.

  @Column(name = "nickname", length = 128)
  private String nickname;

  @Column(name = "role", nullable = false, length = 32)
  private String role;

  @Column(name = "verified", nullable = false)
  private boolean verified;

  @Column(name = "verification_token", length = 255)
  private String verificationToken;

  // No REMOVE cascade: comments outlive the account (FK ON DELETE SET NULL).
  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  @Setter(AccessLevel.NONE)
  private List<CommentEntity> comments = new ArrayList<>();

  public UserEntity(String email, String password, String nickname, String role) {
    this.email = email;
    this.password = password;
    this.nickname = nickname;
    this.role = role;
  }

  public void addComment(CommentEntity comment) {
    comments.add(comment);
    comment.setUser(this);
  }

  /** Mirrors ON DELETE SET NULL for comments already loaded into the persistence context. */
  @PreRemove
  void detachComments() {
    comments.forEach(comment -> comment.setUser(null));
  }
}
