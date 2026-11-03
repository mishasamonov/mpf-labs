package sumdu.edu.ua.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sumdu.edu.ua.web.service.CatalogFacade;
import sumdu.edu.ua.web.service.MailService;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.error.ValidationException;

@Controller
@RequestMapping("/books")
public class BooksController {
  private final CatalogFacade services;

  private final MailService mailService;
  public BooksController(CatalogFacade services, MailService mailService) {
    this.services = services; this.mailService = mailService;
  }

  @InitBinder("bookForm")
  void constructorBinding(WebDataBinder binder) {
    binder.setDeclarativeBinding(true); // Book is an immutable core record.
  }

  @GetMapping
  public String getBooks(@RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page, Model model) {
    var result = services.catalog().search(q, new PageRequest(page, 10, "id,asc"));
    model.addAttribute("books", result.items());
    model.addAttribute("catalogPage", result);
    model.addAttribute("q", q == null ? "" : q);
    return "books";
  }

  @GetMapping({"/add", "/new"})
  public String showAddForm(Model model) {
    model.addAttribute("bookForm", new Book(0, "", "", 2026));
    return "book-form";
  }

  @PostMapping(value = {"", "/add"}, consumes = "application/x-www-form-urlencoded")
  public String addBook(@ModelAttribute("bookForm") Book book, BindingResult errors,
      RedirectAttributes redirect) {
    if (errors.hasErrors()) return "book-form";
    checkText(book.title(), 255, "title", errors);
    checkText(book.author(), 255, "author", errors);
    if (book.pubYear() < 1 || book.pubYear() > 9999)
      errors.rejectValue("pubYear", "validation.year");
    if (errors.hasErrors()) return "book-form";
    Book saved;
    try {
      // Never trust a submitted id. The existing service and database create it.
      saved = services.catalog().add(book.title(), book.author(), book.pubYear());
    } catch (ValidationException e) {
      errors.reject("validation.book");
      return "book-form";
    }
    try {
      mailService.sendNewBookEmail(saved);
    } catch (IllegalStateException e) {
      // The book is already persisted: report delivery failure without duplicating it.
      redirect.addFlashAttribute("mailFailed", true);
    }
    redirect.addFlashAttribute("bookAdded", true);
    return "redirect:/books"; // Post/Redirect/Get avoids duplicate form submission.
  }

  /** Link target of the user comments page: the book with its comments. */
  @GetMapping("/{id:\\d+}")
  public String showBook(@PathVariable long id) {
    services.catalog().get(id); // 404 for a missing book.
    return "redirect:/comments?bookId=" + id;
  }

  @GetMapping("/{id:\\d+}/edit")
  public String showEditForm(@PathVariable long id, Model model) {
    model.addAttribute("bookForm", services.catalog().get(id));
    model.addAttribute("editId", id);
    return "book-form";
  }

  @PostMapping(value = "/{id:\\d+}/edit", consumes = "application/x-www-form-urlencoded")
  public String editBook(@PathVariable long id, @ModelAttribute("bookForm") Book book,
      BindingResult errors, Model model, RedirectAttributes redirect) {
    model.addAttribute("editId", id); // Keeps edit mode even when binding failed.
    if (errors.hasErrors()) return "book-form";
    checkText(book.title(), 255, "title", errors);
    checkText(book.author(), 255, "author", errors);
    if (book.pubYear() < 1 || book.pubYear() > 9999)
      errors.rejectValue("pubYear", "validation.year");
    if (errors.hasErrors()) return "book-form";
    // The path id wins over the hidden field; save() updates the existing row.
    services.catalog().save(new Book(id, book.title(), book.author(), book.pubYear()));
    redirect.addFlashAttribute("bookUpdated", true);
    return "redirect:/books";
  }

  @PostMapping("/{id:\\d+}/delete")
  public String deleteBook(@PathVariable long id, RedirectAttributes redirect) {
    services.catalog().delete(id); // Comments are removed by JPA cascade.
    redirect.addFlashAttribute("bookDeleted", true);
    return "redirect:/books";
  }

  static void checkText(String value, int max, String field, BindingResult errors) {
    if (value == null || value.isBlank() || value.trim().length() > max)
      errors.rejectValue(field, "validation.text", new Object[]{max}, null);
  }
}
