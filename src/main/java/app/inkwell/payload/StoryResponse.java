package app.inkwell.payload;

import app.inkwell.model.Story;
import app.inkwell.model.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

public class StoryResponse {
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private String authorName;
    private String createdAt;
    private String lastUpdated;
    private int readCount;
    private boolean published;
    private Set<String> tags;
    private int chapterCount;
    
    public StoryResponse(Story story, int chapterCount) {
        this.id = story.getId();
        this.title = story.getTitle();
        this.description = story.getDescription();
        this.coverImage = story.getCoverImage();
        this.authorName = story.getAuthor().getUsername();
        this.createdAt = formatDateTime(story.getCreatedAt());
        this.lastUpdated = formatDateTime(story.getLastUpdated());
        this.readCount = story.getReadCount();
        this.published = story.getPublished();
        this.tags = story.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        this.chapterCount = chapterCount;
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateTime, now);
        
        if (daysBetween == 0) {
            return "Today";
        } else if (daysBetween == 1) {
            return "Yesterday";
        } else if (daysBetween < 7) {
            return daysBetween + " days ago";
        } else if (daysBetween < 30) {
            return (daysBetween / 7) + " weeks ago";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public int getReadCount() {
        return readCount;
    }

    public boolean isPublished() {
        return published;
    }

    public Set<String> getTags() {
        return tags;
    }

    public int getChapterCount() {
        return chapterCount;
    }
}