package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_views", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private LocalDateTime viewedAt;
    
    // Constructor for convenience
    public TopicView(Topic topic, User user) {
        this.topic = topic;
        this.user = user;
        this.viewedAt = LocalDateTime.now();
    }
}