package ua.edu.sumdu.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class CommentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private BookEntity book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;

  // Name at the moment of writing; stays readable after the account is deleted.
  @Column(nullable = false, length = 64)
  private String author;

  @Column(nullable = false, length = 1000)
  private String text;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected CommentEntity() {}

  /** Book and user are attached through BookEntity.addComment / UserEntity.addComment. */
  public CommentEntity(String author, String text, Instant createdAt) {
    this.author = author;
    this.text = text;
    this.createdAt = createdAt;
  }

  public Long getId() { return id; }
  public BookEntity getBook() { return book; }
  void setBook(BookEntity book) { this.book = book; }
  public UserEntity getUser() { return user; }
  void setUser(UserEntity user) { this.user = user; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
  public String getText() { return text; }
  public void setText(String text) { this.text = text; }
  public Instant getCreatedAt() { return createdAt; }
}
