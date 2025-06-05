package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "stories")
public class Story {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String coverImage;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonBackReference
    private User author;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    @Column(name = "read_count", nullable = false)
    private Integer readCount = 0;
    
    @Column(nullable = false)
    private Boolean published = false;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "story_tags",
        joinColumns = @JoinColumn(name = "story_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Chapter> chapters = new HashSet<>();


    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryLike> likes = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
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

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(Set<Chapter> chapters) {
        this.chapters = chapters;
    }
    
    // Helper methods
    public void addTag(Tag tag) {
        tags.add(tag);
    }
    
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
    
    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
        chapter.setStory(this);
    }
    
    public void removeChapter(Chapter chapter) {
        chapters.remove(chapter);
        chapter.setStory(null);
    }

    public Object getContent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContent'");
    }

    public void setContent(Object content) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setContent'");
    }

    public Object getComments() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    // Properly implement the getLikes method
    public List<StoryLike> getLikes() {
        return likes != null ? likes : new ArrayList<>();
    }

    // Add this field to your Story class
    @Column(name = "genre")
    private String genre;

    // Add getter and setter
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    @Column(name = "last_read")
private LocalDateTime lastRead;


public LocalDateTime getLastRead() {
    return lastRead;
}

public void setLastRead(LocalDateTime lastRead) {
    this.lastRead = lastRead;
}

// Add this field to the Story class

@Column(name = "average_rating")
private Float averageRating = 0.0f;

// Add getter and setter methods
public Float getAverageRating() {
    return averageRating;
}

public void setAverageRating(Float averageRating) {
    this.averageRating = averageRating;
}

// Add this field to your Story class
@Column(name = "trending_score")
private Double trendingScore = 0.0;

// Getter and setter
public Double getTrendingScore() {
    return trendingScore;
}

public void setTrendingScore(Double trendingScore) {
    this.trendingScore = trendingScore;
}
}

