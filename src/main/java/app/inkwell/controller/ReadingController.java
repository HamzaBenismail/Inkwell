package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.ReadingStatus;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.ChapterRatingRepository;
import app.inkwell.service.ChapterService;
import app.inkwell.service.StoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ReadingController {

    private static final Logger logger = LoggerFactory.getLogger(ReadingController.class);

  @Autowired
  private StoryService storyService;
    
    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ChapterRatingRepository chapterRatingRepository;

  @GetMapping("/story/{id}")
  public String viewStory(@PathVariable("id") Long storyId, Model model, @AuthenticationPrincipal User user) {
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      
      if (storyOpt.isEmpty() || !storyOpt.get().getPublished()) {
          return "redirect:/Home";
      }
      
      Story story = storyOpt.get();
      List<Chapter> chapters = storyService.getPublishedChapters(story);
      
      model.addAttribute("story", story);
      model.addAttribute("chapters", chapters);
      model.addAttribute("isAuthenticated", SecurityContextHolder.getContext().getAuthentication() != null && 
                                           SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                                           !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
      
      // Add reading status if user is authenticated
      if (user != null) {
          storyService.getUserReadingProgressForStory(user, story).ifPresent(progress -> {
              model.addAttribute("readingStatus", progress.getStatus());
          });
      }
      
      return "StoryView";
  }
  
  @GetMapping("/story/{storyId}/chapter/{chapterId}")
  public String viewChapter(
          @PathVariable("storyId") Long storyId,
          @PathVariable("chapterId") Long chapterId,
          Model model,
          @AuthenticationPrincipal User user) {
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      Optional<Chapter> chapterOpt = storyService.getChapterById(chapterId);
      
      if (storyOpt.isEmpty() || chapterOpt.isEmpty() || 
          !storyOpt.get().getPublished() || !chapterOpt.get().getPublished() ||
          !chapterOpt.get().getStory().getId().equals(storyId)) {
          return "redirect:/Home";
      }
      
      Story story = storyOpt.get();
      Chapter chapter = chapterOpt.get();
      
      // Get previous and next chapters
      List<Chapter> chapters = storyService.getPublishedChapters(story);
      int currentIndex = chapters.indexOf(chapter);
      
      Chapter prevChapter = currentIndex > 0 ? chapters.get(currentIndex - 1) : null;
      Chapter nextChapter = currentIndex < chapters.size() - 1 ? chapters.get(currentIndex + 1) : null;
      

      // Get chapter's average rating
        Double chapterRating = chapter.getAverageRating();
        if (chapterRating == null) {
            // If not stored on chapter, calculate it
            chapterRating = chapterRatingRepository.calculateAverageRating(chapter);
            // Update chapter with calculated rating
            chapter.setAverageRating(chapterRating != null ? chapterRating : 0.0);
            chapterService.saveChapter(chapter);
        }

      model.addAttribute("story", story);
      model.addAttribute("chapter", chapter);
      model.addAttribute("prevChapter", prevChapter);
      model.addAttribute("nextChapter", nextChapter);
      model.addAttribute("isAuthenticated", SecurityContextHolder.getContext().getAuthentication() != null && 
                                           SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                                           !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
      
      // Add reading status if user is authenticated
      if (user != null) {
          storyService.getUserReadingProgressForStory(user, story).ifPresent(progress -> {
              model.addAttribute("readingStatus", progress.getStatus());
          });
      }
      
      return "ChapterView";
  }
  
  @PostMapping("/api/reading/track")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> trackReading(
            @RequestParam("storyId") Long storyId,
            @RequestParam(value = "chapterId", required = false) Long chapterId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Story> storyOpt = storyService.getStoryById(storyId);
            
            if (storyOpt.isEmpty()) {
                logger.warn("Story not found: {}", storyId);
                response.put("success", false);
                response.put("message", "Story not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Story story = storyOpt.get();
            
            if (chapterId != null) {
                // Track chapter read
                Optional<Chapter> chapterOpt = storyService.getChapterById(chapterId);
                
                if (chapterOpt.isEmpty()) {
                    logger.warn("Chapter not found: {}", chapterId);
                    response.put("success", false);
                    response.put("message", "Chapter not found");
                    return ResponseEntity.badRequest().body(response);
                }
                
                Chapter chapter = chapterOpt.get();
                
                // Only track if user is authenticated
                if (user != null) {
                    logger.debug("Recording chapter read for user: {} and chapter: {}", 
                            user.getUsername(), chapter.getId());
                    chapterService.recordChapterRead(chapter, user);
                    response.put("success", true);
                } else {
                    // For anonymous users, we don't track individual reads
                    response.put("success", true);
                }
            } else {
                // Track story view - but only for user library management, not for counts
                if (user != null) {
                    logger.debug("Recording story view for user: {} and story: {}", 
                            user.getUsername(), story.getId());
                    storyService.recordStoryView(user, story);
                    response.put("success", true);
                } else {
                    response.put("success", true);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error tracking reading progress: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

  @PostMapping("/api/reading/status")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> updateReadingStatus(
            @RequestParam("storyId") Long storyId,
            @RequestParam("status") String statusStr,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "User must be authenticated");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Optional<Story> storyOpt = storyService.getStoryById(storyId);
            
            if (storyOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Story not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            Story story = storyOpt.get();
            ReadingStatus status = ReadingStatus.valueOf(statusStr);
            
            storyService.updateReadingStatus(user, story, status);
            
            response.put("success", true);
            response.put("status", status);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid reading status: {}", statusStr);
            response.put("success", false);
            response.put("message", "Invalid reading status");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating reading status: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
  
  private int calculateProgressPercentage(Story story, Chapter chapter) {
      List<Chapter> chapters = storyService.getPublishedChapters(story);
      int totalChapters = chapters.size();
      if (totalChapters == 0) return 0;
      
      int currentChapterIndex = chapters.indexOf(chapter);
      return (currentChapterIndex + 1) * 100 / totalChapters;
  }

  
}

