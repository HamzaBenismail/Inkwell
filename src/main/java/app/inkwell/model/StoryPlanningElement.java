package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "story_planning_elements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoryPlanningElement {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(nullable = false)
  private String title;
  
  @Column(columnDefinition = "TEXT")
  private String content;
  
  private String color;
  
  @Column(name = "position_x")
  private int positionX;
  
  @Column(name = "position_y")
  private int positionY;
  
  private int width;
  
  private int height;
  
  @ManyToOne
  @JoinColumn(name = "story_id", nullable = false)
  private Story story;
  
  // Custom equals and hashCode methods to prevent infinite recursion
  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      StoryPlanningElement that = (StoryPlanningElement) o;
      return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
      return getClass().hashCode();
  }
}

