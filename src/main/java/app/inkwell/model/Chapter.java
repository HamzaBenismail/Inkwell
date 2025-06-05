package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "chapters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"story_id", "chapter_number"})
})
public class Chapter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    @JsonBackReference
    private Story story;
    
    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;
    
    @Column(name = "read_count", nullable = false)
    private Integer readCount = 0;
    
    @Column(nullable = false)
    private Boolean published = false;

    // New field for SoundCloud track URL
    @Column(name = "sound_cloud_track_url")
    private String soundCloudTrackUrl;

    @Column(name = "views", nullable = false)
    private Integer views = 0;


    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        // Calculate word count on content update
        this.wordCount = content != null ? content.split("\\s+").length : 0;
    }

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public Boolean getPublished() {
        return published;
    }

    public void setPublished(Boolean published) {
        this.published = published;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Add getter and setter
public String getSoundCloudTrackUrl() {
    return soundCloudTrackUrl;
}

public void setSoundCloudTrackUrl(String soundCloudTrackUrl) {
    this.soundCloudTrackUrl = soundCloudTrackUrl;
}




@Column(name = "last_read")
private LocalDateTime lastRead;


public LocalDateTime getLastRead() {
    return lastRead;
}

public void setLastRead(LocalDateTime lastRead) {
    this.lastRead = lastRead;
}

@Column(name = "average_rating")
private Double averageRating;

// Add getter and setter
public Double getAverageRating() {
    return averageRating;
}

public void setAverageRating(Double averageRating) {
    this.averageRating = averageRating;
}

public Integer getViews() {
    return views;
}

public void setViews(Integer views) {
    this.views = views;
}
}