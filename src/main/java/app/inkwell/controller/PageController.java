package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.Tag;
import app.inkwell.model.User;
import app.inkwell.service.StoryService;
import app.inkwell.service.TagService;
import app.inkwell.service.UserService;
import app.inkwell.repository.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
public class PageController {
  private static final Logger logger = LoggerFactory.getLogger(PageController.class);

  @Autowired
  private StoryService storyService;


  @Autowired
  private TagService tagService;

  @Autowired
private UserService userService;

    @Autowired
    private StoryRepository storyRepository;

  @GetMapping("/")
  public String index() {
      return "redirect:/Home";
  }

@GetMapping("/Home")
public String home(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String genre,
        @RequestParam(required = false, defaultValue = "recent") String sort,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "20") int size,
        Model model) {
    try {
        // Set up pagination and sorting
        Sort sortOption = getSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOption);

        // Get all tags for the filter dropdown, excluding Fan Fiction tag
        List<Tag> allTags = tagService.getAllTags().stream()
            .filter(tag -> !"Fan Fiction".equalsIgnoreCase(tag.getName()))
            .collect(Collectors.toList());
        model.addAttribute("allTags", allTags);

        // Store current filter values
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentSize", size);
        model.addAttribute("currentPage", page);

        // Get stories based on filters
        Page<Story> storiesPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search with fan fiction exclusion
            storiesPage = storyRepository.findByTitleContainingIgnoreCaseAndGenreNotOrDescriptionContainingIgnoreCaseAndGenreNot(
                search, "Fan Fiction", search, "Fan Fiction", pageable);
        } else if (genre != null && !genre.trim().isEmpty()) {
            // Genre filter with fan fiction exclusion
            storiesPage = storyRepository.findByGenreAndGenreNot(genre, "Fan Fiction", pageable);
        } else {
            // Default: get all non-fan fiction stories
            storiesPage = storyRepository.findByGenreNot("Fan Fiction", pageable);
        }

        // Add pagination data to model
        model.addAttribute("stories", storiesPage.getContent());
        model.addAttribute("totalPages", storiesPage.getTotalPages());
        model.addAttribute("totalItems", storiesPage.getTotalElements());
        model.addAttribute("hasNext", storiesPage.hasNext());
        model.addAttribute("hasPrevious", storiesPage.hasPrevious());

        return "Home";
    } catch (Exception e) {
        logger.error("Error loading Home page", e);
        model.addAttribute("errorMessage", "An error occurred while loading the home page");
        return "error/500";
    }
}

  private Sort getSort(String sortOption) {
      switch (sortOption) {
          case "popular":
              return Sort.by(Sort.Direction.DESC, "readCount");
          case "updated":
              return Sort.by(Sort.Direction.DESC, "lastUpdated");
          case "recent":
                return Sort.by(Sort.Direction.DESC, "createdAt");
          case "highest_rated":
              return Sort.by(Sort.Direction.DESC, "averageRating");
            case "lowest_rated":
              return Sort.by(Sort.Direction.ASC, "averageRating");
              case "trending":
                return Sort.by(Sort.Direction.DESC, "trendingScore");
          default:
              return Sort.by(Sort.Direction.DESC, "createdAt");
      }
  }
    

  @GetMapping("/fanfiction")
public String fanFictionPage(
    @RequestParam(required = false) String search,
    @RequestParam(required = false, defaultValue = "recent") String sort,
    @RequestParam(required = false, defaultValue = "0") int page,
    @RequestParam(required = false, defaultValue = "20") int size,
    Model model) {
    try {
        // Set up pagination and sorting
        Sort sortOption = getSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOption);

        // Store current filter values
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentSize", size);
        model.addAttribute("currentPage", page);

        // Get stories based on filters
        Page<Story> storiesPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search within fan fiction stories
            storiesPage = storyRepository.findByTitleContainingIgnoreCaseAndGenreOrDescriptionContainingIgnoreCaseAndGenre(
                search, "Fan Fiction", search, "Fan Fiction", pageable);
        } else {
            // Default: get all fan fiction stories
            storiesPage = storyRepository.findByGenre("Fan Fiction", pageable);
        }

        // Add pagination data to model
        model.addAttribute("stories", storiesPage.getContent());
        model.addAttribute("totalPages", storiesPage.getTotalPages());
        model.addAttribute("totalItems", storiesPage.getTotalElements());
        model.addAttribute("hasNext", storiesPage.hasNext());
        model.addAttribute("hasPrevious", storiesPage.hasPrevious());

        return "FanFiction";
    } catch (Exception e) {
        logger.error("Error loading FanFiction page", e);
        model.addAttribute("errorMessage", "An error occurred while loading the fan fiction page");
        return "error/500";
    }
}

    @GetMapping("/Settings")
    public String settingsPage(Model model, @AuthenticationPrincipal User user) {
        if (user != null) {
            model.addAttribute("user", user);
            model.addAttribute("isEmailVerified", user.isEmailVerified());
            model.addAttribute("isWriter", user.isWriter());
        }
        return "Settings";
    }

   @PostMapping("settings/security/update")
   public String updateSecuritySettings(
           @RequestParam("currentPassword") String currentPassword,
           @RequestParam("newPassword") String newPassword,
           @RequestParam("confirmPassword") String confirmPassword,
           Model model) {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
           model.addAttribute("notAuthenticated", true);
           return "Settings";
       }
       
       User user = (User) auth.getPrincipal();
       
       // Basic check to ensure new password and confirm password match
       if (!newPassword.equals(confirmPassword)) {
           model.addAttribute("errorMessage", "New password and confirm password do not match.");
           return "Settings";
       }
       
       try {
           userService.updatePassword(user.getUsername(), currentPassword, newPassword);
           model.addAttribute("successMessage", "Password updated successfully.");
       } catch (Exception e) {
           model.addAttribute("errorMessage", e.getMessage());
       }
       
       // Re-load user object to persist changes and populate model
       model.addAttribute("user", userService.getUserByUsername(user.getUsername()).orElse(null));
       return "Settings";
   }

   @GetMapping("settings/email/verify")
    public String verifyEmail(@RequestParam("token") String token) {
        try {
            boolean verified = userService.verifyEmail(token);
            if (verified) {
                // Get current authentication
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof User) {
                    // Reload user with fresh data 
                    User currentUser = userService.getUserByUsername(((User) auth.getPrincipal()).getUsername())
                        .orElse(null);
                    
                    if (currentUser != null) {
                        // Create new authentication with updated user and same authorities
                        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                            currentUser, auth.getCredentials(), auth.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(newAuth);
                    }
                }
                return "redirect:/Settings?emailVerified=true&tab=writers";
            } else {
                return "redirect:/Settings?emailVerified=false&tab=writers";
            }
        } catch (Exception e) {
            logger.error("Error verifying email", e);
            return "redirect:/Settings?emailVerified=false&tab=writers";
        }
    }


   @GetMapping("/genres")
public String genresPage(Model model) {
    try {
        // Get all available genres
        List<String> genres = storyService.getAllGenres();
        
        // Create a map to store stories by genre
        Map<String, List<Story>> storiesByGenre = new HashMap<>();
        
        // Get a few stories for each genre as preview
        for (String genre : genres) {
            List<Story> stories = storyService.getStoriesByGenre(genre, PageRequest.of(0, 4)).getContent();
            storiesByGenre.put(genre, stories);
        }
        
        model.addAttribute("genres", genres);
        model.addAttribute("storiesByGenre", storiesByGenre);
        
        return "genres";
    } catch (Exception e) {
        logger.error("Error loading genres page", e);
        model.addAttribute("error", "Failed to load genres: " + e.getMessage());
        return "error";
    }
}

@GetMapping("/genre/{genreName}")
public String genrePage(
        @PathVariable String genreName,
        @RequestParam(required = false, defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "12") int size,
        Model model) {
    try {
        // Get stories by genre with pagination
        Page<Story> stories = storyService.getStoriesByGenre(
            genreName, 
            PageRequest.of(page, size, Sort.by("lastUpdated").descending())
        );
        
        // Get popular tags for this genre
        // This would require adding a method to your TagService
        // List<Tag> popularTags = tagService.getPopularTagsForGenre(genreName, 10);
        
        model.addAttribute("genre", genreName);
        model.addAttribute("stories", stories.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", stories.getTotalPages());
        model.addAttribute("totalItems", stories.getTotalElements());
        // model.addAttribute("popularTags", popularTags);
        
        return "genre";
    } catch (Exception e) {
        logger.error("Error loading genre page for: " + genreName, e);
        model.addAttribute("error", "Failed to load genre: " + e.getMessage());
        return "error";
    }
}
}

