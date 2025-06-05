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
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Post parent; // For nested replies
    
    private boolean isEdited = false;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.isEdited = true;
        this.updatedAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Post> replies = new ArrayList<>();
    
    // Convenience method to add a reply
    public void addReply(Post reply) {
        replies.add(reply);
        reply.setParent(this);
    }
}