package sumdu.edu.ua.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Data of the sign-up form. Validated in the web layer, re-checked by UserService. */
@Getter
@Setter
@NoArgsConstructor
public class RegisterUserDto {
  @Email(message = "Некоректний email")
  @NotBlank(message = "Email обов'язковий")
  @Size(max = 128, message = "Email: не більше 128 символів")
  private String email;

  @NotBlank(message = "Пароль обов'язковий")
  @Size(min = 6, max = 72, message = "Пароль має бути не менше 6 символів")
  private String password;

  @Size(max = 128, message = "Нікнейм: не більше 128 символів")
  private String nickname;
}
