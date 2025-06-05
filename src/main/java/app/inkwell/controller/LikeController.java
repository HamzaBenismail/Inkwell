package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.StoryRepository;
import app.inkwell.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    private static final Logger logger = LoggerFactory.getLogger(LikeController.class);

    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private LikeService likeService;

    @GetMapping("/story/{storyId}")
    public ResponseEntity<?> getStoryLikeStatus(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User user) {
        
        try {
            Optional<Story> storyOpt = storyRepository.findById(storyId);
            if (storyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Story story = storyOpt.get();
            boolean userLiked = false;
            
            // Check if user has liked the story if authenticated
            if (user != null) {
                userLiked = likeService.hasUserLikedStory(user, story);
            }
            
            // Get total like count
            long likeCount = likeService.getStoryLikeCount(story);
            
            Map<String, Object> response = new HashMap<>();
            response.put("liked", userLiked);
            response.put("count", likeCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting story like status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting like status");
        }
    }
    
    @PostMapping("/story/{storyId}")
    public ResponseEntity<?> toggleStoryLike(
            @PathVariable Long storyId,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        
        try {
            Optional<Story> storyOpt = storyRepository.findById(storyId);
            if (storyOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Story story = storyOpt.get();
            boolean liked = likeService.toggleStoryLike(user, story);
            long likeCount = likeService.getStoryLikeCount(story);
            
            Map<String, Object> response = new HashMap<>();
            response.put("liked", liked);
            response.put("count", likeCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error toggling story like", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing like request");
        }
    }
}