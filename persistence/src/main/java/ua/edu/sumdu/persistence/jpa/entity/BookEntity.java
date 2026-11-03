package ua.edu.sumdu.persistence.jpa.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
public class BookEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 255)
  private String author;

  // YEAR is a reserved word in H2, so the column keeps the name pub_year.
  @Column(name = "pub_year", nullable = false)
  private Integer year;

  // Deleting a book deletes its comments; LAZY avoids loading them with every list page.
  @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.LAZY)
  @OrderBy("createdAt DESC")
  private List<CommentEntity> comments = new ArrayList<>();

  protected BookEntity() {} // Required by JPA.

  public BookEntity(String title, String author, Integer year) {
    this.title = title;
    this.author = author;
    this.year = year;
  }

  /** Keeps both sides of the bidirectional relation consistent. */
  public void addComment(CommentEntity comment) {
    comments.add(comment);
    comment.setBook(this);
  }

  public void removeComment(CommentEntity comment) {
    comments.remove(comment); // orphanRemoval issues DELETE on flush.
  }

  public Long getId() { return id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getAuthor() { return author; }
  public void setAuthor(String author) { this.author = author; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public List<CommentEntity> getComments() { return comments; }
}
