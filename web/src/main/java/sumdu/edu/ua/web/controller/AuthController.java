package sumdu.edu.ua.web.controller;

import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import sumdu.edu.ua.web.security.SecurityAuditListener;
import sumdu.edu.ua.core.dto.RegisterUserDto;
import sumdu.edu.ua.core.error.ConflictException;
import sumdu.edu.ua.core.error.ValidationException;
import sumdu.edu.ua.core.service.UserService;

/** Login page, sign-up with e-mail confirmation. POST /login and /logout are Spring Security. */
@Controller
public class AuthController {
  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/login")
  public String login(Authentication authentication) {
    // Anonymous visitors have no Principal here; signed-in users go straight to the catalog.
    return authentication != null ? "redirect:/books" : "auth/login";
  }

  @GetMapping("/register")
  public String registerForm(Model model) {
    model.addAttribute("user", new RegisterUserDto());
    return "auth/sign-up";
  }

  @PostMapping("/register")
  public String processRegister(
      @ModelAttribute("user") @Valid RegisterUserDto dto,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) return "auth/sign-up";
    try {
      userService.register(dto);
      SecurityAuditListener.AUDIT.info("REGISTER email={}", dto.getEmail().trim());
      model.addAttribute("email", dto.getEmail().trim().toLowerCase(Locale.ROOT));
      return "auth/sign-up-success";
    } catch (ConflictException | ValidationException ex) {
      model.addAttribute("error", ex.getMessage());
      return "auth/sign-up";
    } catch (IllegalStateException ex) { // SMTP is down: the account was rolled back.
      model.addAttribute("error", "Не вдалося надіслати лист підтвердження. Спробуйте пізніше.");
      return "auth/sign-up";
    }
  }

  /** Link from the e-mail; ?code= is accepted as an alias of ?token=. */
  @GetMapping("/confirm")
  public String confirm(
      @RequestParam(name = "token", required = false) String token,
      @RequestParam(name = "code", required = false) String code) {
    boolean ok = userService.verifyAccount(token != null ? token : code);
    SecurityAuditListener.AUDIT.info("CONFIRM_ACCOUNT success={}", ok);
    return ok ? "auth/confirm-success" : "auth/confirm-error";
  }
}
