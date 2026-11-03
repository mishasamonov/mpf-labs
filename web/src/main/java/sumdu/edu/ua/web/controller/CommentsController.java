package sumdu.edu.ua.web.controller;

import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.error.InvalidCommentTextException;
import sumdu.edu.ua.core.service.CommentService;

@Controller
@RequestMapping("/comments")
public class CommentsController {
  private final CatalogFacade services;
  private final CommentService commentService;

  public CommentsController(CatalogFacade services, CommentService commentService) {
    this.services = services;
    this.commentService = commentService;
  }

  /** Only the text is submitted: the author is the signed-in account. */
  public record CommentForm(String text) {}

  @InitBinder("commentForm")
  void constructorBinding(WebDataBinder binder) { binder.setDeclarativeBinding(true); }

  @GetMapping
  public String list(@RequestParam long bookId,
      @RequestParam(defaultValue = "0") int page, Model model) {
    populate(bookId, page, model);
    model.addAttribute("commentForm", new CommentForm(""));
    return "book-comments";
  }

  @PostMapping(consumes = "application/x-www-form-urlencoded")
  public String add(@RequestParam long bookId,
      @ModelAttribute("commentForm") CommentForm form, BindingResult errors,
      Authentication authentication, Model model, RedirectAttributes redirect) {
    BooksController.checkText(form.text(), 1000, "text", errors);
    if (errors.hasErrors()) {
      populate(bookId, 0, model);
      return "book-comments";
    }
    long userId = services.users().findByEmailOrThrow(authentication.getName()).getId();
    try {
      services.comments().add(bookId, userId, form.text());
    } catch (InvalidCommentTextException e) { // Lab 10: forbidden words etc. from core.
      errors.rejectValue("text", "comment.text.rejected", e.getMessage());
      populate(bookId, 0, model);
      return "book-comments";
    }
    redirect.addFlashAttribute("commentAdded", true);
    return "redirect:/comments?bookId=" + bookId;
  }

  /**
   * Lab 10: all checks are in CommentService.delete. Its exceptions become JSON 400/404
   * in GlobalApiExceptionHandler; createdAt is optional here so that the service reports it.
   */
  @PostMapping("/delete")
  public String delete(@RequestParam long bookId, @RequestParam long commentId,
      @RequestParam(required = false) Instant createdAt, RedirectAttributes redirect) {
    commentService.delete(bookId, commentId, createdAt);
    redirect.addFlashAttribute("commentDeleted", true);
    return "redirect:/comments?bookId=" + bookId;
  }

  private void populate(long bookId, int page, Model model) {
    model.addAttribute("book", services.catalog().get(bookId));
    var result = services.comments().list(bookId, null, null,
        new PageRequest(page, 10, "createdAt,desc"));
    model.addAttribute("comments", result.items());
    model.addAttribute("commentPage", result);
  }
}
