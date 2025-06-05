package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_views", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chapter_id", "user_id"}),
    @UniqueConstraint(columnNames = {"chapter_id", "ip_address"})
})
public class ChapterView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();
    
    // Default constructor
    public ChapterView() {
    }
    
    // Constructor for registered users
    public ChapterView(Chapter chapter, User user) {
        this.chapter = chapter;
        this.user = user;
    }
    
    // Constructor for anonymous users
    public ChapterView(Chapter chapter, String ipAddress) {
        this.chapter = chapter;
        this.ipAddress = ipAddress;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}