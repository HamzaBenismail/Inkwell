package app.inkwell.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    private String slug;
    
    private boolean isPinned = false;
    private boolean isLocked = false;
    
    @ManyToOne
    @JoinColumn(name = "subforum_id")
    private Subforum subforum;
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    
    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL)
    private List<Post> posts = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "topic_tags", joinColumns = @JoinColumn(name = "topic_id"))
    private Set<String> tags = new HashSet<>();
    
    private Integer viewCount = 0;
    private Integer replyCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void incrementReplyCount() {
        this.replyCount++;
    }
}