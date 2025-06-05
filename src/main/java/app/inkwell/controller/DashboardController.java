package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.service.StoryService;
import app.inkwell.service.StatisticsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    @Autowired
    private StoryService storyService;

    @Autowired
    private StatisticsService statisticsService;

    @PreAuthorize("hasAuthority('WRITER')")
    @GetMapping("/Dashboard")
  public String dashboard(Model model) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      boolean isAuthenticated = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()));
      model.addAttribute("isAuthenticated", isAuthenticated);
      
      if (!isAuthenticated) {
          return "redirect:/SignIn?redirect=/Dashboard";
      }

      User user = (User) auth.getPrincipal();
      if (!user.isWriter()) {
          return "redirect:/Settings?tab=writers";
      }

      try {
          // Add user stories
          List<Story> userStories = storyService.getUserStories(user);
          if (userStories == null) {
              userStories = new ArrayList<>();
          }
          model.addAttribute("userStories", userStories);
          
          // Add simplified stories for JavaScript
          try {
              String simplifiedStories = storyService.getSimplifiedUserStoriesJson(user);
              if (simplifiedStories == null) {
                  simplifiedStories = "[]";
              }
              model.addAttribute("simplifiedStories", simplifiedStories);
          } catch (Exception e) {
              e.printStackTrace();
              model.addAttribute("simplifiedStories", "[]");
          }
          
          // Add user stats
          Map<String, Integer> userStats = statisticsService.getUserStatistics(user);

          try {
                // Try to get actual stats if possible
                if (auth.getPrincipal() instanceof User) {
                    user = (User) auth.getPrincipal();
                    if (statisticsService != null) {
                        Map<String, Integer> actualStats = statisticsService.getUserStatistics(user);
                        if (actualStats != null && !actualStats.isEmpty()) {
                            userStats = actualStats;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Continue with default stats
            }

          model.addAttribute("userStats", userStats);
          
          return "Dashboard";
      } catch (Exception e) {;
          model.addAttribute("errorMessage", "An error occurred while loading the dashboard");
          return "error/500";
      }
  }



    @GetMapping("/Writing")
    @PreAuthorize("hasAuthority('WRITER')")
    public String writingPage(@AuthenticationPrincipal User user, Model model, @RequestParam(required = false) Long storyId) {
      // Add authentication status
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      boolean isAuthenticated = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()));
      model.addAttribute("isAuthenticated", isAuthenticated);
      
      if (user == null) {;
          return "redirect:/SignIn?redirect=/Writing";
      }

      if (!user.isWriter()) {
        return "redirect:/Settings?tab=writers";
    }
      
      List<Story> userStories = storyService.getUserStories(user);
      if (userStories == null) {
          userStories = new ArrayList<>();
      }
      model.addAttribute("userStories", userStories);
  
      // Get story ID from request parameter if available
      if (storyId != null) {
          Optional<Story> story = storyService.getStoryById(storyId);
          if (story.isPresent() && story.get().getAuthor().getId().equals(user.getId())) {
              model.addAttribute("currentStory", story.get());
          }
      }

      return "Writing";
  }
    
    @GetMapping("/Statistics")
    @PreAuthorize("hasAuthority('WRITER')")
    public String statisticsPage(@AuthenticationPrincipal User user, Model model) {
    if (user != null) {
        if (!user.isWriter()) {
            return "redirect:/Settings?tab=writers";
        }
        
        try {
            // Get user statistics with error handling
            Map<String, Integer> userStats = new HashMap<>();
            userStats.put("totalStories", 0);
            userStats.put("totalReads", 0);
            userStats.put("totalStoryLikes", 0);
            userStats.put("totalChapterLikes", 0);
            userStats.put("totalLikes", 0);
            userStats.put("totalComments", 0);
            
            try {
                if (statisticsService != null) {
                    Map<String, Integer> actualStats = statisticsService.getUserStatistics(user);
                    if (actualStats != null && !actualStats.isEmpty()) {
                        userStats = actualStats;
                        // Ensure totalLikes is set if not already present
                        if (!userStats.containsKey("totalLikes")) {
                            int storyLikes = userStats.getOrDefault("totalStoryLikes", 0);
                            int chapterLikes = userStats.getOrDefault("totalChapterLikes", 0);
                            userStats.put("totalLikes", storyLikes + chapterLikes);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Continue with default stats
            }
            model.addAttribute("userStats", userStats);
            
            // Get user stories with error handling
            List<Story> userStories = new ArrayList<>();
            try {
                userStories = storyService.getUserStories(user);
            } catch (Exception e) {
                e.printStackTrace();
                // Continue with empty list
            }
            model.addAttribute("userStories", userStories);
            
            // If user has stories, get statistics for the first one as an example
            if (!userStories.isEmpty()) {
                try {
                    Map<String, Object> storyStats = statisticsService.getStoryStatistics(userStories.get(0));
                    model.addAttribute("storyStats", storyStats);
                    model.addAttribute("selectedStory", userStories.get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continue without story stats
                }
            }
            
            return "Statistics";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred while loading statistics");
            return "error/500";
        }
    }
    return "redirect:/SignIn?redirect=/Statistics";
}
    
    @GetMapping({"/writing-guide", "/seo-tips", "/analytics"})
    @PreAuthorize("hasAuthority('WRITER')")
    public String writerResources(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("user", user);
        return "WriterResources";
    }
}