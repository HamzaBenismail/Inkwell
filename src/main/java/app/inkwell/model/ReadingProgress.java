package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reading_progress", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"user_id", "story_id"})
})
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgress {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne
  @JoinColumn(name = "story_id", nullable = false)
  private Story story;
  
  @ManyToOne
  @JoinColumn(name = "current_chapter_id")
  private Chapter currentChapter;
  
  private int progressPercentage;
  
  @Enumerated(EnumType.STRING)
  private ReadingStatus status = ReadingStatus.READING;
  
  @Column(nullable = false)
  private LocalDateTime lastReadAt = LocalDateTime.now();
  
  // Custom equals and hashCode methods to prevent infinite recursion
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ReadingProgress that = (ReadingProgress) o;
      return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
      return getClass().hashCode();
  }
}

