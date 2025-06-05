package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;
  
  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne
  @JoinColumn(name = "story_id", nullable = false)
  private Story story;
  
  @ManyToOne
  @JoinColumn(name = "chapter_id")
  private Chapter chapter;
  
  @ManyToOne
  @JoinColumn(name = "parent_id")
  private Comment parentComment;
  
  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  private int likeCount = 0;
  
  // Custom equals and hashCode methods to prevent infinite recursion
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Comment comment = (Comment) o;
      return id != null && Objects.equals(id, comment.id);
  }

  @Override
  public int hashCode() {
      return getClass().hashCode();
  }
}

