package app.inkwell.dto;

import java.time.LocalDateTime;

import app.inkwell.model.ChapterRating;

public class ChapterRatingDTO {

    // Why do I have one dto file? I don't know. I just do.
    // I couldn't find a way to implement dto in other files the same way I did for the REST OF MY FUNCTIONS. FUCK THIS SHIT.

    private Long id;
    private Long chapterId;
    private Long userId;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean visible;
    private Integer helpfulCount;
    private Boolean containsSpoilers;
    private String username; // Optional - include user's name
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
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

    public Boolean getContainsSpoilers() {
        return containsSpoilers;
    }

    public void setContainsSpoilers(Boolean containsSpoilers) {
        this.containsSpoilers = containsSpoilers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    
    
    // Constructor from entity
    public static ChapterRatingDTO fromEntity(ChapterRating rating) {
        ChapterRatingDTO dto = new ChapterRatingDTO();
        dto.setId(rating.getId());
        dto.setChapterId(rating.getChapter().getId());
        dto.setUserId(rating.getUser().getId());
        dto.setRating(rating.getRating());
        dto.setCreatedAt(rating.getCreatedAt());
        dto.setUpdatedAt(rating.getUpdatedAt());
        dto.setVisible(rating.getVisible());
        dto.setHelpfulCount(rating.getHelpfulCount());
        dto.setContainsSpoilers(rating.getContainsSpoilers());
        dto.setUsername(rating.getUser().getUsername());
        return dto;
    }
}