package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.User;
import app.inkwell.model.Story;
import app.inkwell.service.ChapterService;
import app.inkwell.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private StoryService storyService;

    @GetMapping("/{id}")
    public ResponseEntity<Chapter> getChapter(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Optional<Chapter> chapterOpt = chapterService.getChapterById(id);
        if (chapterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chapter chapter = chapterOpt.get();
            
        // Check if user has permission to view this chapter
        if (!chapter.getStory().getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(chapter);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<?> saveChapter(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal User user) {
        
        Optional<Chapter> chapterOpt = chapterService.getChapterById(id);
        if (chapterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chapter chapter = chapterOpt.get();
            
        // Check if user has permission to edit this chapter
        if (!chapter.getStory().getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        chapter.setContent(payload.get("content"));
        chapterService.saveChapter(chapter);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishChapter(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        
        Optional<Chapter> chapterOpt = chapterService.getChapterById(id);
        if (chapterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chapter chapter = chapterOpt.get();
            
        // Check if user has permission to publish this chapter
        if (!chapter.getStory().getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        chapter.setPublished(true);
        chapterService.saveChapter(chapter);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create")
    public ResponseEntity<Chapter> createChapter(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal User user) {
    
        Long storyId = Long.parseLong(payload.get("storyId").toString());
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        int chapterNumber = Integer.parseInt(payload.get("chapterNumber").toString());
    
        // Get the story
        Optional<Story> storyOpt = storyService.getStoryById(storyId);
        if (storyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Story story = storyOpt.get();
        
        // Check if user has permission to add a chapter to this story
        if (!story.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
    
        // Create the chapter
        Chapter chapter = chapterService.createChapter(story, title, content, chapterNumber);
    
        return ResponseEntity.ok(chapter);
    }
}

