package app.inkwell.service;

import app.inkwell.model.Story;
import app.inkwell.repository.StoryRepository;
import app.inkwell.repository.CommentRepository;
import app.inkwell.repository.StoryRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TrendingService {
    private static final Logger logger = LoggerFactory.getLogger(TrendingService.class);
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private StoryRatingRepository storyRatingRepository;

    // Constants for trending algorithm
    private static final int RECENT_DAYS = 7; // Consider activity from last 7 days
    private static final double WEIGHT_READS = 1.0;
    private static final double WEIGHT_RATINGS = 5.0;
    private static final double WEIGHT_COMMENTS = 3.0;
    
    /**
     * Scheduled job to recalculate trending scores for all stories every 6 hours
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // Every 6 hours
    @Transactional
    public void updateAllTrendingScores() {
        logger.info("Starting scheduled trending score update");
        LocalDateTime start = LocalDateTime.now();
        
        List<Story> stories = storyRepository.findByPublishedTrue();
        int count = 0;
        
        for (Story story : stories) {
            updateTrendingScoreForStory(story);
            count++;
            
            // Log progress for large datasets
            if (count % 100 == 0) {
                logger.info("Updated trending scores for {} stories", count);
            }
        }
        
        logger.info("Completed trending score update for {} stories in {} seconds", 
                   count, ChronoUnit.SECONDS.between(start, LocalDateTime.now()));
    }
    
    /**
     * Update trending score for a single story
     */
    @Transactional
    public void updateTrendingScoreForStory(Story story) {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(RECENT_DAYS, ChronoUnit.DAYS);
        
        // Count recent engagement
        int recentReads = countRecentReads(story, cutoffDate);
        int recentRatings = countRecentRatings(story, cutoffDate);
        int recentComments = countRecentComments(story, cutoffDate);
        
        // Calculate base score from weighted engagement
        double baseScore = (recentReads * WEIGHT_READS) + 
                           (recentRatings * WEIGHT_RATINGS) + 
                           (recentComments * WEIGHT_COMMENTS);
        
        // Apply time decay - more recent stories get a boost
        double ageInDays = ChronoUnit.DAYS.between(story.getCreatedAt(), LocalDateTime.now());
        double timeDecayFactor = 1.0 / (1.0 + Math.log10(1 + ageInDays/10.0));
        
        // Calculate final score
        double trendingScore = baseScore * timeDecayFactor;
        
        // Update story with new score
        story.setTrendingScore(trendingScore);
        storyRepository.save(story);
    }
    
    /**
     * Count recent reads for a story
     */
    private int countRecentReads(Story story, LocalDateTime sinceDate) {
        // Use the repository method to count reads
        return storyRepository.countRecentReadsForStory(story, sinceDate);
    }
    
    /**
     * Count recent ratings for a story
     */
    private int countRecentRatings(Story story, LocalDateTime sinceDate) {
        // Assuming you have a StoryRatingRepository
        return storyRatingRepository.countByStoryAndCreatedAtAfter(story, sinceDate);
    }
    
    /**
     * Count recent comments for a story
     */
    private int countRecentComments(Story story, LocalDateTime sinceDate) {
        // Assuming you have a CommentRepository with a similar method
        return commentRepository.countByStoryAndCreatedAtAfter(story.getId(), sinceDate);
    }
    
    /**
     * Get trending stories with caching for improved performance
     */
    @Cacheable("trendingStoriesCache")
    public Page<Story> getTrendingStories(PageRequest pageRequest) {
        return storyRepository.findByPublishedTrueOrderByTrendingScoreDesc(pageRequest);
    }
}