package app.inkwell.service;

import app.inkwell.model.*;
import app.inkwell.repository.ChapterRatingRepository;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.StoryRatingRepository;
import app.inkwell.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RatingService {
    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    private StoryRatingRepository storyRatingRepository;
    
    @Autowired
    private ChapterRatingRepository chapterRatingRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserRepository userRepository;

    // Story Rating Methods
    public Optional<StoryRating> getStoryRatingById(Long id) {
        return storyRatingRepository.findById(id);
    }
    
    public List<StoryRating> getStoryRatings(Story story) {
        return storyRatingRepository.findByStoryOrderByCreatedAtDesc(story);
    }
    
    public Optional<StoryRating> getUserStoryRating(User user, Story story) {
        return storyRatingRepository.findByStoryAndUser(story, user);
    }
    
    public StoryRating createStoryRating(Story story, User user, int rating) {
        StoryRating storyRating = new StoryRating();
        storyRating.setStory(story);
        storyRating.setUser(user);
        storyRating.setRating(rating);
        storyRating.setCreatedAt(LocalDateTime.now());
        storyRating.setUpdatedAt(LocalDateTime.now());
        storyRating.setVisible(true);
        
        return storyRatingRepository.save(storyRating);
    }
    
    public Double calculateStoryAverageRating(Story story) {
        return storyRatingRepository.calculateAverageRating(story);
    }
    
    public void markStoryRatingAsHelpful(StoryRating rating) {
        rating.incrementHelpfulCount();
        storyRatingRepository.save(rating);
    }
    
    // Chapter Rating Methods
    public Optional<ChapterRating> getChapterRatingById(Long id) {
        return chapterRatingRepository.findById(id);
    }
    
    public List<ChapterRating> getChapterRatings(Chapter chapter) {
        return chapterRatingRepository.findByChapterOrderByCreatedAtDesc(chapter);
    }
    
    public Optional<ChapterRating> getUserChapterRating(User user, Chapter chapter) {
        return chapterRatingRepository.findByChapterAndUser(chapter, user);
    }
    
    
    public Double saveChapterRating(Long chapterId, Long userId, Integer rating, Boolean containsSpoilers) {
    // Get the actual Chapter and User entities
    Chapter chapter = chapterRepository.findById(chapterId)
        .orElseThrow(() -> new RuntimeException("Chapter not found"));
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Find existing rating or create new one
    ChapterRating chapterRating = chapterRatingRepository.findByChapterAndUser(chapter, user)
        .orElse(new ChapterRating());
    
    // Set all required fields
    chapterRating.setChapter(chapter);
    chapterRating.setUser(user);
    chapterRating.setRating(rating);
    chapterRating.setContainsSpoilers(containsSpoilers);
    chapterRating.setVisible(true);
    
    // Save and return average
    chapterRatingRepository.save(chapterRating);
    return calculateChapterAverageRating(chapter);
}

// Add a Long overload for calculateChapterAverageRating
public Double calculateChapterAverageRating(Chapter chapter2) {
    Chapter chapter = chapterRepository.findById(chapter2.getId())
        .orElseThrow(() -> new RuntimeException("Chapter not found"));
    return calculateChapterAverageRating(chapter);
}
    
    public void markChapterRatingAsHelpful(ChapterRating rating) {
        rating.incrementHelpfulCount();
        chapterRatingRepository.save(rating);
    }

    public Double saveChapterRatingSimple(Long chapterId, Long userId, Integer rating, Boolean containsSpoilers) {
    Chapter chapter = chapterRepository.findById(chapterId)
        .orElseThrow(() -> new EntityNotFoundException("Chapter not found"));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
    
    // Find existing rating or create new one
    ChapterRating chapterRating = chapterRatingRepository.findByChapterAndUser(chapter, user)
        .orElse(new ChapterRating());
    
    // Set rating properties
    chapterRating.setChapter(chapter);
    chapterRating.setUser(user);
    chapterRating.setRating(rating);
    chapterRating.setContainsSpoilers(containsSpoilers);
    chapterRating.setUpdatedAt(LocalDateTime.now());
    
    if (chapterRating.getId() == null) {
        chapterRating.setCreatedAt(LocalDateTime.now());
        chapterRating.setVisible(true);
        chapterRating.setHelpfulCount(0);
    }
    
    // Save rating
    chapterRatingRepository.save(chapterRating);
    
    // Calculate and update average rating on chapter
    Double averageRating = chapterRatingRepository.calculateAverageRating(chapter);
    if (averageRating == null) {
        averageRating = 0.0;
    }
    
    // Important: Update the chapter with the average rating
    chapter.setAverageRating(averageRating);
    chapterRepository.save(chapter);
    
    return averageRating;
}
    
}