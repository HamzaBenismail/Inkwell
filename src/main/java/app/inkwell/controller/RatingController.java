package app.inkwell.controller;

import app.inkwell.model.*;
import app.inkwell.repository.*;
import app.inkwell.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private StoryRatingRepository storyRatingRepository;
    
    @Autowired
    private ChapterRatingRepository chapterRatingRepository;
    
    @Autowired
    private RatingService ratingService;

    // Story Ratings
    @GetMapping("/story/{storyId}")
    public ResponseEntity<?> getStoryRatings(
            @PathVariable Long storyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Optional<Story> storyOpt = storyRepository.findById(storyId);
            if (storyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Story story = storyOpt.get();
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<StoryRating> ratings = storyRatingRepository.findByStoryAndVisibleTrueOrderByCreatedAtDesc(story, pageable);
            Double avgRating = storyRatingRepository.calculateAverageRating(story);
            long ratingCount = storyRatingRepository.countByStoryAndVisibleTrue(story);
            
            Map<String, Object> response = new HashMap<>();
            response.put("ratings", ratings.getContent());
            response.put("currentPage", ratings.getNumber());
            response.put("totalItems", ratings.getTotalElements());
            response.put("totalPages", ratings.getTotalPages());
            response.put("averageRating", avgRating != null ? avgRating : 0.0);
            response.put("ratingCount", ratingCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting story ratings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting ratings");
        }
    }
    
    // Replace or update the existing createStoryRating method
@PostMapping("/story/{storyId}")
public ResponseEntity<?> createStoryRating(
            @PathVariable Long storyId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("error", "Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            // Validate the rating value
            if (payload == null || !payload.containsKey("rating")) {
                response.put("success", false);
                response.put("error", "Rating value is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            int rating;
            try {
                Object ratingObj = payload.get("rating");
                if (ratingObj instanceof Integer) {
                    rating = (Integer) ratingObj;
                } else if (ratingObj instanceof Double) {
                    rating = ((Double) ratingObj).intValue();
                } else {
                    rating = Integer.parseInt(ratingObj.toString());
                }
                
                if (rating < 1 || rating > 5) {
                    response.put("success", false);
                    response.put("error", "Rating must be between 1 and 5");
                    return ResponseEntity.badRequest().body(response);
                }
            } catch (Exception e) {
                response.put("success", false);
                response.put("error", "Invalid rating value");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<Story> storyOpt = storyRepository.findById(storyId);
            if (storyOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Story not found");
                return ResponseEntity.notFound().build();
            }
            
            Story story = storyOpt.get();
            
            // Check if user has already rated
            Optional<StoryRating> existingRatingOpt = storyRatingRepository.findByStoryAndUser(story, user);
            StoryRating storyRating;
            
            if (existingRatingOpt.isPresent()) {
                // Update existing rating
                storyRating = existingRatingOpt.get();
                storyRating.setRating(rating);
                storyRating.setUpdatedAt(LocalDateTime.now());
            } else {
                // Create new rating
                storyRating = new StoryRating();
                storyRating.setStory(story);
                storyRating.setUser(user);
                storyRating.setRating(rating);
                storyRating.setCreatedAt(LocalDateTime.now());
                storyRating.setUpdatedAt(LocalDateTime.now());
                storyRating.setVisible(true);
            }
            
            storyRatingRepository.save(storyRating);
            
            // Update average rating
            Double avgRating = storyRatingRepository.calculateAverageRating(story);
            if (avgRating != null) {
                story.setAverageRating(avgRating.floatValue());
                storyRepository.save(story);
            }
            
            response.put("success", true);
            response.put("averageRating", avgRating != null ? avgRating : 0.0);
            response.put("isNew", !existingRatingOpt.isPresent());
            
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("id", storyRating.getId());
            ratingData.put("rating", storyRating.getRating());
            response.put("rating", ratingData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating story rating", e);
            response.put("success", false);
            response.put("error", "Error processing rating");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @DeleteMapping("/story/{ratingId}")
    public ResponseEntity<?> deleteStoryRating(
            @PathVariable Long ratingId,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
    Map.of("success", false, "message", "Authentication required")
);
        }
        
        try {
            Optional<StoryRating> ratingOpt = storyRatingRepository.findById(ratingId);
            if (ratingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            StoryRating rating = ratingOpt.get();
            Story story = rating.getStory();
            
            // Check if the user is the author of the rating or an admin
            if (!rating.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own ratings");
            }
            
            storyRatingRepository.delete(rating);
            
            // Update story average rating
            Double avgRating = storyRatingRepository.calculateAverageRating(story);
            if (avgRating != null) {
                story.setAverageRating(avgRating.floatValue());
            } else {
                story.setAverageRating(0f); // No ratings left
            }
            storyRepository.save(story);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating deleted successfully");
            response.put("averageRating", avgRating);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error deleting story rating", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting rating");
        }
    }

    @GetMapping("/story/{storyId}/user")
public ResponseEntity<?> getUserStoryRating(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("hasRated", false);
            return ResponseEntity.ok(response);
        }
        
        try {
            Optional<Story> storyOpt = storyRepository.findById(storyId);
            if (storyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Story story = storyOpt.get();
            Optional<StoryRating> ratingOpt = storyRatingRepository.findByStoryAndUser(story, user);
            
            response.put("hasRated", ratingOpt.isPresent());
            
            if (ratingOpt.isPresent()) {
                StoryRating rating = ratingOpt.get();
                Map<String, Object> ratingData = new HashMap<>();
                ratingData.put("id", rating.getId());
                ratingData.put("rating", rating.getRating());
                
                response.put("rating", ratingData);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking user story rating", e);
            response.put("error", "Error checking user rating");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    
    
    // Chapter Ratings
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<?> getChapterRatings(
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
            if (chapterOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Chapter chapter = chapterOpt.get();
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            
            Page<ChapterRating> ratingPage = chapterRatingRepository.findByChapterAndVisibleTrueOrderByCreatedAtDesc(chapter, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("ratings", ratingPage.getContent());
            response.put("currentPage", ratingPage.getNumber());
            response.put("totalItems", ratingPage.getTotalElements());
            response.put("totalPages", ratingPage.getTotalPages());
            response.put("averageRating", chapterRatingRepository.calculateAverageRating(chapter));
            response.put("ratingCount", chapterRatingRepository.countByChapterAndVisibleTrue(chapter));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error [operation description]: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/chapter/{chapterId}/user")
    public ResponseEntity<?> getUserChapterRating(
    @PathVariable Long chapterId,
    @AuthenticationPrincipal User user) {

    if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            Map.of("success", false, "message", "Authentication required")
        );
    }
    
    try {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chapter chapter = chapterOpt.get();
        Optional<ChapterRating> ratingOpt = chapterRatingRepository.findByChapterAndUser(chapter, user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hasRated", ratingOpt.isPresent()); // Changed from hasRating to hasRated for consistency
        
        if (ratingOpt.isPresent()) {
            ChapterRating rating = ratingOpt.get();
            Map<String, Object> ratingData = new HashMap<>();
            ratingData.put("id", rating.getId());
            ratingData.put("rating", rating.getRating());
            
            response.put("rating", ratingData); // Nest rating in a ratingData object for consistency
        }
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        logger.error("Error fetching user rating: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Error fetching user rating: " + e.getMessage()
        ));
    }
}
    
    @PostMapping("/chapter/{chapterId}")
    public ResponseEntity<?> createChapterRating(
        @PathVariable Long chapterId,
        @RequestBody Map<String, Object> payload,
        @AuthenticationPrincipal User user) {
    
    if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            Map.of("success", false, "message", "Authentication required")
        );
    }
    
    try {
        // Extract data from request
        Integer rating = payload.get("rating") instanceof Number ? 
            ((Number) payload.get("rating")).intValue() : null;
        
        Boolean containsSpoilers = payload.get("containsSpoilers") instanceof Boolean ?
            (Boolean) payload.get("containsSpoilers") : false;
        
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(
                Map.of("success", false, "message", "Rating must be between 1 and 5")
            );
        }
        
        // Create rating directly using IDs to avoid entity recursion issues
        Double averageRating = ratingService.saveChapterRatingSimple(
            chapterId, user.getId(), rating, containsSpoilers);
        
        // Return a simple Map response with just what we need
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "averageRating", averageRating
        ));
    } catch (Exception e) {
        logger.error("Error creating chapter rating: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Error creating chapter rating: " + e.getMessage()
        ));
    }
}
    
    @DeleteMapping("/chapter/{ratingId}")
    public ResponseEntity<?> deleteChapterRating(
            @PathVariable Long ratingId,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
    Map.of("success", false, "message", "Authentication required")
);
        }
        
        try {
            Optional<ChapterRating> ratingOpt = chapterRatingRepository.findById(ratingId);
            if (ratingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            ChapterRating rating = ratingOpt.get();
            
            // Check if the user is the author of the rating or an admin
            if (!rating.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own ratings");
            }
            
            Chapter chapter = rating.getChapter();
            chapterRatingRepository.delete(rating);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating deleted successfully");
            response.put("averageRating", chapterRatingRepository.calculateAverageRating(chapter));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error [operation description]: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    // Mark rating as helpful
    @PostMapping("/helpful/{ratingType}/{ratingId}")
    public ResponseEntity<?> markRatingAsHelpful(
            @PathVariable String ratingType,
            @PathVariable Long ratingId,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
    Map.of("success", false, "message", "Authentication required")
);
        }
        
        try {
            if ("story".equals(ratingType)) {
                Optional<StoryRating> ratingOpt = storyRatingRepository.findById(ratingId);
                if (ratingOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                
                StoryRating rating = ratingOpt.get();
                rating.incrementHelpfulCount();
                storyRatingRepository.save(rating);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("helpfulCount", rating.getHelpfulCount());
                return ResponseEntity.ok(response);
                
            } else if ("chapter".equals(ratingType)) {
                Optional<ChapterRating> ratingOpt = chapterRatingRepository.findById(ratingId);
                if (ratingOpt.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                
                ChapterRating rating = ratingOpt.get();
                rating.incrementHelpfulCount();
                chapterRatingRepository.save(rating);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("helpfulCount", rating.getHelpfulCount());
                return ResponseEntity.ok(response);
                
            } else {
                return ResponseEntity.badRequest().body("Invalid rating type");
            }
        } catch (Exception e) {
            logger.error("Error marking rating as helpful", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error marking rating as helpful");
        }
    }
}