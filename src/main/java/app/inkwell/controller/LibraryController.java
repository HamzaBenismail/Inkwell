package app.inkwell.controller;

import app.inkwell.model.ReadingProgress;
import app.inkwell.model.ReadingStatus;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.service.StoryService;
import app.inkwell.service.LikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class LibraryController {
    private static final Logger logger = LoggerFactory.getLogger(LibraryController.class);

    @Autowired
    private StoryService storyService;

    @Autowired
    private LikeService likeService;

    @GetMapping("/Library")
    public String library(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false, defaultValue = "continue") String tab,
            @RequestParam(required = false, defaultValue = "all") String filter,
            Model model) {
        
        if (user == null) {
            logger.debug("User not authenticated, redirecting to SignIn");
            return "redirect:/SignIn?redirect=/Library";
        }
        
        try {
            logger.info("Loading Library page for user: {} with tab: {} and filter: {}", user.getUsername(), tab, filter);

            // Add this code to fetch the user's liked stories
            if (user!= null) {
                List<Story> likedStories = likeService.getLikedStoriesByUser(user);
                model.addAttribute("favorites", likedStories);
            }
            
            // Set active tab and filter
            model.addAttribute("activeTab", tab);
            model.addAttribute("activeFilter", filter);
            
            // Get reading progress based on status
            List<ReadingProgress> continueReading = storyService.getUserReadingProgressByStatus(user, ReadingStatus.READING);
            model.addAttribute("continueReading", continueReading);
            logger.debug("Found {} stories in 'continue reading'", continueReading.size());
            
            // Only load these if we're on the readlist tab
            if ("readlist".equals(tab)) {
                List<ReadingProgress> planToRead = storyService.getUserReadingProgressByStatus(user, ReadingStatus.PLAN_TO_READ);
                model.addAttribute("planToRead", planToRead);
                logger.debug("Found {} stories in 'plan to read'", planToRead.size());
                
                List<ReadingProgress> onHold = storyService.getUserReadingProgressByStatus(user, ReadingStatus.ON_HOLD);
                model.addAttribute("onHold", onHold);
                logger.debug("Found {} stories in 'on hold'", onHold.size());
                
                List<ReadingProgress> completed = storyService.getUserReadingProgressByStatus(user, ReadingStatus.COMPLETED);
                model.addAttribute("completed", completed);
                logger.debug("Found {} stories in 'completed'", completed.size());
                
                List<ReadingProgress> dropped = storyService.getUserReadingProgressByStatus(user, ReadingStatus.DROPPED);
                model.addAttribute("dropped", dropped);
                logger.debug("Found {} stories in 'dropped'", dropped.size());
            }
            
            return "Library";
        } catch (Exception e) {
            logger.error("Error loading Library page", e);
            model.addAttribute("errorMessage", "An error occurred while loading your library");
            return "error/500";
        }
    }
}

