package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "read_records")
public class ReadRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Can be null for anonymous reads
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Optional: IP address or other identifier for anonymous readers
    private String readerIdentifier;
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Story getStory() {
        return story;
    }
    
    public void setStory(Story story) {
        this.story = story;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getReaderIdentifier() {
        return readerIdentifier;
    }
    
    public void setReaderIdentifier(String readerIdentifier) {
        this.readerIdentifier = readerIdentifier;
    }
}