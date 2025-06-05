package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Character {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(nullable = false)
  private String name;
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  private String imageUrl;
  
  @ManyToOne
  @JoinColumn(name = "story_id", nullable = false)
  private Story story;
  
  // Custom equals and hashCode methods to prevent infinite recursion
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Character character = (Character) o;
      return id != null && Objects.equals(id, character.id);
  }

  @Override
  public int hashCode() {
      return getClass().hashCode();
  }
}

