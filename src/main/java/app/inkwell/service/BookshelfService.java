package app.inkwell.service;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.StoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookshelfService {

    @Autowired
    private StoryRepository storyRepository;
    
    /**
     * Gets the stories a user is currently reading
     */
    public List<BookshelfEntry> getCurrentlyReading(User user) {
        // This is a placeholder for the actual implementation
        // In a real implementation, you would fetch this data from the database
        return new ArrayList<>();
    }
    
    /**
     * Gets the stories a user plans to read
     */
    public List<BookshelfEntry> getPlanToRead(User user) {
        // Placeholder implementation
        return new ArrayList<>();
    }
    
    /**
     * Gets the stories a user has put on hold
     */
    public List<BookshelfEntry> getOnHold(User user) {
        // Placeholder implementation
        return new ArrayList<>();
    }
    
    /**
     * Gets the stories a user has completed
     */
    public List<BookshelfEntry> getCompleted(User user) {
        // Placeholder implementation
        return new ArrayList<>();
    }
    
    /**
     * Gets the most liked story by a user
     */
    public Story getMostLikedStory(User user) {
        // Placeholder implementation
        return null;
    }
    
    /**
     * Gets the recently read stories for a user
     */
    public List<RecentReadEntry> getRecentlyReadStories(User user) {
        // Placeholder implementation
        return new ArrayList<>();
    }
    
    /**
     * Gets the reading streak for a user
     */
    public int getReadingStreak(User user) {
        // Placeholder implementation
        return 0;
    }
    
    /**
     * Represents an entry in a user's bookshelf
     */
    public static class BookshelfEntry {
        private Story story;
        private int readProgress; // percentage completed
        private String status; // "reading", "plan", "hold", "completed"
        private LocalDateTime lastRead;
        private LocalDateTime completedDate;
        
        public BookshelfEntry(Story story, int readProgress, String status) {
            this.story = story;
            this.readProgress = readProgress;
            this.status = status;
            this.lastRead = LocalDateTime.now().minusDays((long)(Math.random() * 7));
        }
        
        // Getters and setters
        public Story getStory() { return story; }
        public void setStory(Story story) { this.story = story; }
        
        public int getReadProgress() { return readProgress; }
        public void setReadProgress(int readProgress) { this.readProgress = readProgress; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public LocalDateTime getLastRead() { return lastRead; }
        public void setLastRead(LocalDateTime lastRead) { this.lastRead = lastRead; }
        
        public LocalDateTime getCompletedDate() { return completedDate; }
        public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    }
    
    /**
     * Represents a recently read story entry
     */
    public static class RecentReadEntry {
        private Story story;
        private String lastRead; // e.g. "2d ago"
        
        public RecentReadEntry(Story story, String lastRead) {
            this.story = story;
            this.lastRead = lastRead;
        }
        
        // Getters and setters
        public Story getStory() { return story; }
        public void setStory(Story story) { this.story = story; }
        
        public String getLastRead() { return lastRead; }
        public void setLastRead(String lastRead) { this.lastRead = lastRead; }
    }
}