package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.User;
import app.inkwell.service.ChapterService;
import app.inkwell.service.ViewTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tracking")
public class ViewTrackingController {

    @Autowired
    private ViewTrackingService viewTrackingService;
    
    @Autowired
    private ChapterService chapterService;
    
    @PostMapping("/chapter-view")
    public ResponseEntity<?> trackChapterView(
            @RequestParam Long chapterId,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        
        Optional<Chapter> chapterOpt = chapterService.getChapterById(chapterId);
        if (chapterOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Chapter chapter = chapterOpt.get();
        
        // Get client IP for anonymous users
        String ipAddress = viewTrackingService.getClientIpAddress(request);
        
        // Track the view
        boolean isNewView = viewTrackingService.trackChapterView(chapter, user, ipAddress);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("uniqueView", isNewView);
        response.put("views", chapter.getViews());
        
        return ResponseEntity.ok(response);
    }
}