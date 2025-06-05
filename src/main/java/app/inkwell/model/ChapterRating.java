package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "chapter_reviews")
public class ChapterRating {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference
    private Chapter chapter;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
    
    @Column(nullable = false)
    private Integer rating; // Rating from 1-5
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Flag for admin to hide inappropriate ratings
    @Column(nullable = false)
    private Boolean visible = true;
    
    // Helpful votes count
    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount = 0;

    // Getters and Setters
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        // Ensure rating is between 1 and 5
        if (rating < 1) rating = 1;
        if (rating > 5) rating = 5;
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getVisible() {
        return visible;
    }
    
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public Integer getHelpfulCount() {
        return helpfulCount;
    }
    
    public void setHelpfulCount(Integer helpfulCount) {
        this.helpfulCount = helpfulCount;
    }
    
    // Increment helpful count
    public void incrementHelpfulCount() {
        this.helpfulCount += 1;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private Double averageRating;

// Add getter and setter
public Double getAverageRating() {
    return averageRating;
}

public void setAverageRating(Double averageRating) {
    this.averageRating = averageRating;
}


@Column(name = "contains_spoilers", nullable = false)
private Boolean containsSpoilers = false;

public Boolean getContainsSpoilers() {
    return containsSpoilers;
}

public void setContainsSpoilers(Boolean containsSpoilers) {
    this.containsSpoilers = containsSpoilers;
}
}