package sumdu.edu.ua.web.service;

import org.springframework.stereotype.Service;
import sumdu.edu.ua.core.service.CatalogService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;

@Service
public class CatalogFacade {
  private final CatalogService catalog;
  private final CommentService comments;
  private final UserService users;

  public CatalogFacade(CatalogService catalog, CommentService comments, UserService users) {
    this.catalog = catalog;
    this.comments = comments;
    this.users = users;
  }

  public CatalogService catalog() {
    return catalog;
  }

  public CommentService comments() {
    return comments;
  }

  public UserService users() {
    return users;
  }
}
