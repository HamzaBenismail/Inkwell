package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.StoryLikeRepository;
import app.inkwell.repository.StoryRepository;
import app.inkwell.service.InteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private InteractionService interactionService;
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private StoryLikeRepository storyLikeRepository;
    
    @PostMapping("/story/{storyId}")
    public ResponseEntity<?> toggleStoryFavorite(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication required");
            return ResponseEntity.badRequest().body(response);
        }
        
        Optional<Story> storyOpt = storyRepository.findById(storyId);
        if (storyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Story story = storyOpt.get();
        boolean hasFavorited = interactionService.hasUserLikedStory(user, story);
        
        if (hasFavorited) {
            interactionService.unlikeStory(user, story);
        } else {
            interactionService.likeStory(user, story);
        }
        
        int favoriteCount = storyLikeRepository.countByStory(story);
        
        Map<String, Object> response = new HashMap<>();
        response.put("favorited", !hasFavorited);
        response.put("favoriteCount", favoriteCount);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/story/{storyId}")
    public ResponseEntity<?> getStoryFavoriteStatus(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
        Optional<Story> storyOpt = storyRepository.findById(storyId);
        if (storyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Story story = storyOpt.get();
        boolean hasFavorited = false;
        
        if (user != null) {
            hasFavorited = interactionService.hasUserLikedStory(user, story);
        }
        
        int favoriteCount = storyLikeRepository.countByStory(story);
        
        Map<String, Object> response = new HashMap<>();
        response.put("favorited", hasFavorited);
        response.put("favoriteCount", favoriteCount);
        
        return ResponseEntity.ok(response);
    }
    
    // For backwards compatibility, keep the original like endpoints that redirect to favorites
    @RequestMapping(value = "/likes/story/{storyId}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> legacyStoryLikesEndpoint(
            @PathVariable Long storyId, 
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        
        boolean isPost = "POST".equals(request.getMethod());
        if (isPost) {
            return toggleStoryFavorite(storyId, user);
        } else {
            return getStoryFavoriteStatus(storyId, user);
        }
    }
}

    
