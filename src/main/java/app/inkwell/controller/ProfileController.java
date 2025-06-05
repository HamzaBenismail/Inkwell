package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.SubscriptionRepository;
import app.inkwell.service.BookshelfService;
import app.inkwell.service.StatisticsService;
import app.inkwell.service.UserService;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private BookshelfService bookshelfService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    
    // Define the upload directory
    private static final String UPLOAD_DIR = "src/main/resources/static/images/profile/";

    private static final String IMAGES_DIR = "src/main/resources/static/images/";

    /**
     * Initialize necessary directories and default files
     */
    @PostConstruct
    public void init() {
        try {
            // Create profile images directory if it doesn't exist
            File profileDir = new File(UPLOAD_DIR);
            if (!profileDir.exists()) {
                logger.info("Creating profile images directory: {}", UPLOAD_DIR);
                profileDir.mkdirs();
            }
            
            // Create images directory if it doesn't exist
            File imagesDir = new File(IMAGES_DIR);
            if (!imagesDir.exists()) {
                logger.info("Creating images directory: {}", IMAGES_DIR);
                imagesDir.mkdirs();
            }
            
            // Check if default profile image exists, create a simple one if not
            File defaultProfileImage = new File(IMAGES_DIR + "default-avatar.png");
            if (!defaultProfileImage.exists()) {
                logger.info("Default profile image not found, creating a placeholder");
                // You could copy a default image from classpath here
                defaultProfileImage.createNewFile();
            }
        } catch (Exception e) {
            logger.error("Error initializing profile resources", e);
        }
    }

    @GetMapping({"/profile", "/Profile"})
    public String currentUserProfile(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                model.addAttribute("notAuthenticated", true);
                return "ReaderProfile"; // Default to reader profile for unauthenticated
            }
            
            logger.info("Loading profile for authenticated user");
            
            Object principal = auth.getPrincipal();
            String username;
            
            // Handle different types of principal objects
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof User) {
                username = ((User) principal).getUsername();
            } else {
                username = principal.toString();
            }
            
            logger.info("Username from authentication: {}", username);
            
            // Get user from database
            Optional<User> userOpt = userService.getUserByUsername(username);
            
            if (!userOpt.isPresent()) {
                logger.warn("User not found in database: {}", username);
                model.addAttribute("profileNotFound", true);
                return "ReaderProfile";
            }
            
            User profileUser = userOpt.get();
            logger.info("User found: {}, is writer: {}", profileUser.getUsername(), profileUser.isWriter());
            
            // Determine which profile to show based on user type
            if (profileUser.isWriter()) {
                return redirectToWriterProfile(profileUser, model, true);
            } else {
                return redirectToReaderProfile(profileUser, model, true);
            }
        } catch (Exception e) {
            logger.error("Error loading profile: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading your profile: " + e.getMessage());
            return "ReaderProfile";
        }
    }

    @GetMapping({"/profile/{username}", "/Profile/{username}"})
    public String userProfile(@PathVariable String username, Model model) {
        try {
            logger.info("Loading profile for user: {}", username);
            
            // Get profile user
            Optional<User> userOpt = userService.getUserByUsername(username);
            
            if (!userOpt.isPresent()) {
                logger.warn("Profile not found for username: {}", username);
                model.addAttribute("profileNotFound", true);
                return "ReaderProfile";
            }
            
            User profileUser = userOpt.get();
            
            // Check if this is the current user's own profile
            boolean isOwnProfile = false;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String currentUsername = auth.getName();
                isOwnProfile = currentUsername.equals(username);
            }
            
            logger.info("User found: {}, is writer: {}, isOwnProfile: {}", 
                profileUser.getUsername(), profileUser.isWriter(), isOwnProfile);
            
            // Determine which profile to show based on user type
            if (profileUser.isWriter()) {
                return redirectToWriterProfile(profileUser, model, isOwnProfile);
            } else {
                return redirectToReaderProfile(profileUser, model, isOwnProfile);
            }
        } catch (Exception e) {
            logger.error("Error loading profile: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the profile: " + e.getMessage());
            return "ReaderProfile";
        }
    }

    // Helper method for writer profile
    private String redirectToWriterProfile(User profileUser, Model model, boolean isOwnProfile) {
        model.addAttribute("user", profileUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        
        // Get writer statistics
        Map<String, Integer> userStats = statisticsService.getUserStatistics(profileUser);
        model.addAttribute("userStats", userStats);
        
        // Get user stories - ensure null safety
        List<Story> userStories = new ArrayList<>();
        List<Story> ongoingStories = new ArrayList<>();
        Story mostPopularStory = null;
        
        if (profileUser.getStories() != null && !profileUser.getStories().isEmpty()) {
            try {
                // Create a new list to avoid ConcurrentModificationException
                userStories = new ArrayList<>(profileUser.getStories());
                
                // Find ongoing stories and most popular story
                int maxReads = -1;
                for (Story story : userStories) {
                    // Check if most popular
                    int reads = story.getReadCount();
                    if (reads > maxReads) {
                        maxReads = reads;
                        mostPopularStory = story;
                    }
                    
                    // Check if ongoing
                    if (!story.getPublished()) {
                        ongoingStories.add(story);
                    }
                }
                
                // Sort stories by last updated date (newest first)
                userStories.sort(Comparator.comparing(Story::getLastUpdated, Comparator.nullsLast(Comparator.reverseOrder())));
            } catch (Exception e) {
                logger.error("Error processing user stories: {}", e.getMessage());
            }
        }
        
        model.addAttribute("userStories", userStories);
        model.addAttribute("ongoingStories", ongoingStories);
        model.addAttribute("mostPopularStory", mostPopularStory);
        model.addAttribute("recentQuestions", new ArrayList<>());
        
        if (!isOwnProfile) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    User subscriber = userService.getUserByUsername(auth.getName()).orElseThrow();
                    boolean isSubscribed = subscriptionRepository.existsBySubscriberAndWriterAndStatus(
                            subscriber, profileUser, "active");
                    model.addAttribute("isSubscribed", isSubscribed);
                } else {
                    model.addAttribute("isSubscribed", false);
                }
            } catch (Exception e) {
                logger.warn("Error checking subscription status: {}", e.getMessage());
                model.addAttribute("isSubscribed", false);
            }
        } else {
            model.addAttribute("isSubscribed", false);
        }
        
        return "WriterProfile";
    }

    // Helper method for reader profile
    private String redirectToReaderProfile(User profileUser, Model model, boolean isOwnProfile) {
        model.addAttribute("user", profileUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        
        // Reader stats
        Map<String, Integer> readerStats = new HashMap<>();
        readerStats.put("storiesRead", 0);
        readerStats.put("storiesLiked", 0);
        readerStats.put("reviewsWritten", 0);
        model.addAttribute("readerStats", readerStats);
        
        // Get bookshelf data
        model.addAttribute("currentlyReading", bookshelfService.getCurrentlyReading(profileUser));
        model.addAttribute("planToRead", bookshelfService.getPlanToRead(profileUser));
        model.addAttribute("onHold", bookshelfService.getOnHold(profileUser));
        model.addAttribute("completed", bookshelfService.getCompleted(profileUser));
        
        // Get reading activity
        model.addAttribute("readingStreak", bookshelfService.getReadingStreak(profileUser));
        model.addAttribute("recentlyReadStories", bookshelfService.getRecentlyReadStories(profileUser));
        model.addAttribute("mostLikedStory", bookshelfService.getMostLikedStory(profileUser));
        
        return "ReaderProfile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) String bio,
                             @RequestParam(required = false) MultipartFile profileImage,
                             RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return "redirect:/login";
            }
            
            logger.info("Processing profile update");
            
            // Get current user
            Object principal = auth.getPrincipal();
            String currentUsername;
            
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                currentUsername = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof User) {
                currentUsername = ((User) principal).getUsername();
            } else {
                currentUsername = principal.toString();
            }
            
            Optional<User> userOpt = userService.getUserByUsername(currentUsername);
            if (!userOpt.isPresent()) {
                logger.warn("User not found for profile update: {}", currentUsername);
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/Profile";
            }
            
            User user = userOpt.get();
            logger.info("Updating profile for user: {}", user.getUsername());
            
            // Update bio if provided
            if (bio != null) {
                logger.info("Updating bio");
                user.setBio(bio);
            }
            
            // Update username if provided and not taken
            if (username != null && !username.isEmpty() && !username.equals(user.getUsername())) {
                logger.info("Attempting to update username from {} to {}", user.getUsername(), username);
                // Check if username is available
                if (userService.existsByUsername(username)) {
                    logger.warn("Username already taken: {}", username);
                    redirectAttributes.addFlashAttribute("errorMessage", "Username already taken");
                    return "redirect:/Profile";
                }
                user.setUsername(username);
            }
            
            // Handle profile image upload
            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    logger.info("Processing profile image upload: {}, size: {}", 
                        profileImage.getOriginalFilename(), profileImage.getSize());
                    
                    // Create directory if it doesn't exist
                    File directory = new File(UPLOAD_DIR);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    
                    // Generate a unique filename
                    String fileExtension = getFileExtension(profileImage.getOriginalFilename());
                    String fileName = UUID.randomUUID().toString() + "." + fileExtension;
                    Path filePath = Paths.get(UPLOAD_DIR, fileName);
                    
                    // Save the file
                    Files.copy(profileImage.getInputStream(), filePath);
                    
                    // Update user profile image path
                    String imageUrl = "/images/profile/" + fileName;
                    logger.info("Setting profile image URL: {}", imageUrl);
                    user.setProfileImage(imageUrl);
                } catch (IOException e) {
                    logger.error("Error saving profile image", e);
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload profile image: " + e.getMessage());
                    return "redirect:/Profile";
                }
            }
            
            // Save the updated user
            userService.save(user);
            
            logger.info("Profile updated successfully for user: {}", user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully");
            return "redirect:/Profile";
        } catch (Exception e) {
            logger.error("Error updating profile", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());
            return "redirect:/Profile";
        }
    }
    
    /**
     * Helper method to get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
    
    @GetMapping("/become-writer")
    public String becomeWriter(RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return "redirect:/login";
            }
            
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/Profile";
            }
            
            if (currentUser.isWriter()) {
                redirectAttributes.addFlashAttribute("errorMessage", "You are already a writer");
                return "redirect:/Profile";
            }
            
            // Check if email is verified
            if (!currentUser.isEmailVerified()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please verify your email first");
                return "redirect:/Profile";
            }
            
            // Promote user to writer
            User updatedUser = userService.promoteToWriter(currentUser);
            if (updatedUser.isWriter()) {
                redirectAttributes.addFlashAttribute("successMessage", "Congratulations! You are now a writer.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to become a writer. Please try again.");
            }
            
            return "redirect:/Profile";
        } catch (Exception e) {
            logger.error("Error becoming writer", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error becoming a writer: " + e.getMessage());
            return "redirect:/Profile";
        }
    }
}
