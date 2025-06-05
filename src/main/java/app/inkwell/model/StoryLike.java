package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "story_likes", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"user_id", "story_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoryLike {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne
  @JoinColumn(name = "story_id", nullable = false)
  private Story story;
  
  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  // Custom equals and hashCode methods to prevent infinite recursion
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StoryLike storyLike = (StoryLike) o;
      return id != null && Objects.equals(id, storyLike.id);
  }

  @Override
  public int hashCode() {
      return getClass().hashCode();
  }
}

