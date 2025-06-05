package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subforum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private String slug; // URL-friendly version of the name
    
    private String iconClass; // Font Awesome icon class
    private String iconBgClass; // Background color class for the icon
    
    @ManyToOne
    @JoinColumn(name = "forum_id")
    private Forum forum;
    
    @ManyToOne
    @JoinColumn(name = "story_id")
    private Story story;
    
    @OneToMany(mappedBy = "subforum", cascade = CascadeType.ALL)
    private List<Topic> topics = new ArrayList<>();
    
    private Integer topicCount = 0;
    private Integer postCount = 0;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}