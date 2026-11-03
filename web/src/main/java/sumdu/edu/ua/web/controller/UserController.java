package sumdu.edu.ua.web.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;

@Controller
public class UserController {
  private final CommentService commentService;
  private final UserService userService;

  public UserController(CatalogFacade services) {
    this.commentService = services.comments();
    this.userService = services.users();
  }

  /** ADMIN only (SecurityConfig and @PreAuthorize on UserService.findAll). */
  @GetMapping("/users")
  public String users(Model model) {
    model.addAttribute("users", userService.findAll());
    model.addAttribute("roles", List.of(User.ROLE_REGISTERED, User.ROLE_USER, User.ROLE_ADMIN));
    return "users";
  }

  @PostMapping("/users/{id}/role")
  public String changeRole(@PathVariable long id, @RequestParam String role,
      Authentication authentication, RedirectAttributes redirect) {
    if (userService.findById(id).getEmail().equals(authentication.getName())) {
      redirect.addFlashAttribute("ownRole", true); // An admin cannot lock himself out.
      return "redirect:/users";
    }
    userService.changeRole(id, role);
    redirect.addFlashAttribute("roleChanged", true);
    return "redirect:/users";
  }

  @GetMapping("/users/{id}/comments")
  public String userComments(@PathVariable Long id, Model model) {
    model.addAttribute("comments", commentService.getCommentsByUser(id));
    model.addAttribute("username", userService.findById(id).getNickname());
    return "user-comments";
  }

  /** Alias from the lab 8 guide: /user/{email} resolves the login to the id route. */
  @GetMapping("/user/{email}")
  public String byEmail(@PathVariable String email) {
    long id = userService.findByEmailOrThrow(email).getId();
    return "redirect:/users/" + id + "/comments";
  }
}
