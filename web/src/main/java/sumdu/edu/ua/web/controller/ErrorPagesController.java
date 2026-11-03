package sumdu.edu.ua.web.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Targets of accessDeniedPage("/403") and of missing resources. Any HTTP method is forwarded. */
@Controller
public class ErrorPagesController {
  @RequestMapping("/403")
  public String forbidden(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    return "error/403";
  }

  @RequestMapping("/404")
  public String notFound(HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return "error/404";
  }

  /**
   * Lab 10: target of GlobalMvcExceptionHandler. The flash attributes are already in the model;
   * the original status is restored so the redirected page still reports 400/404/409.
   */
  @GetMapping("/error-page")
  public String errorPage(Model model, HttpServletResponse response) {
    if (model.getAttribute("status") instanceof Integer status) response.setStatus(status);
    else model.addAttribute("messageKey", "error.request");
    return "error";
  }
}
