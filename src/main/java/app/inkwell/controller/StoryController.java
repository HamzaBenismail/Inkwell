package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Controller
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

    // Define the directory where uploaded images will be stored
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    @PostMapping("/create")
public String createStory(
        @AuthenticationPrincipal User user,
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam(value = "genre", required = false) String genre,
        @RequestParam(value = "tags", required = false) String[] tags,
        @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
        RedirectAttributes redirectAttributes) {
    
    try {
        System.out.println("Creating story: " + title + " for user: " + user.getUsername());
        System.out.println("Genre: " + genre);
        System.out.println("Tags: " + (tags != null ? String.join(", ", tags) : "none"));
        
        // Convert tags array to list
        List<String> tagList = tags != null ? Arrays.asList(tags) : new ArrayList<>();
        
        // Handle cover image
        String coverImageUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            // Create uploads directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // Generate a unique filename to prevent overwriting
            String originalFilename = coverImage.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save the file
            Path filePath = Paths.get(UPLOAD_DIR + newFilename);
            Files.write(filePath, coverImage.getBytes());
            
            // Set the URL for the database
            coverImageUrl = "/uploads/" + newFilename;
            System.out.println("Saved cover image to: " + filePath.toAbsolutePath());
        }
        
        // Create the story
        Story story = storyService.createStory(title, description, user, tagList);
        
        // Set the genre if provided
        if (genre != null && !genre.isEmpty()) {
            story.setGenre(genre);
            storyService.saveStory(story);
        }
        
        // Set the cover image URL if one was uploaded
        if (coverImageUrl != null) {
            story.setCoverImage(coverImageUrl);
            storyService.saveStory(story);
        }
        
        // Set success message
        redirectAttributes.addFlashAttribute("successMessage", "Story created successfully!");
        
        // Redirect to the writing page with the new story ID
        return "redirect:/Writing?storyId=" + story.getId();
    } catch (Exception e) {
        // Log the error
        System.err.println("Error creating story: " + e.getMessage());
        e.printStackTrace();
        
        // Set error message
        redirectAttributes.addFlashAttribute("errorMessage", "Failed to create story: " + e.getMessage());
        return "redirect:/Dashboard";
    }
}

    @PostMapping("/{id}/publish")
    @ResponseBody
    public Map<String, Object> publishStory(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long storyId) {
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<Story> storyOpt = storyService.getStoryById(storyId);
        if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
            response.put("success", false);
            response.put("message", "Story not found or not owned by user");
            return response;
        }
        
        Story story = storyOpt.get();
        
        // Publish the story
        story.setPublished(true);
        storyService.saveStory(story);
        
        // Log the successful publish
        System.out.println("Story published successfully: " + story.getTitle() + " (ID: " + story.getId() + ")");
        
        response.put("success", true);
        response.put("message", "Story published successfully");
        
        return response;
    }
    
    // Add a debug endpoint to check stories
    @RequestMapping("/debug/list")
    @ResponseBody
    public List<Map<String, Object>> debugListStories(@AuthenticationPrincipal User user) {
        if (user == null) {
            return List.of(Map.of("error", "User not authenticated"));
        }
        
        List<Story> stories = storyService.getUserStories(user);
        return stories.stream().map(story -> {
            Map<String, Object> storyMap = new HashMap<>();
            storyMap.put("id", story.getId());
            storyMap.put("title", story.getTitle());
            storyMap.put("description", story.getDescription());
            storyMap.put("coverImage", story.getCoverImage());
            storyMap.put("published", story.getPublished());
            return storyMap;
        }).collect(Collectors.toList());
    }

    @GetMapping("/random")
    @ResponseBody
    public List<Map<String, Object>> getRandomStories(@RequestParam(defaultValue = "10") int limit) {
        try {
            System.out.println("Fetching " + limit + " random published stories");
            List<Story> randomStories = storyService.getRandomPublishedStories(limit);
            System.out.println("Found " + randomStories.size() + " random stories");
            
            List<Map<String, Object>> result = randomStories.stream().map(story -> {
                Map<String, Object> storyData = new HashMap<>();
                storyData.put("id", story.getId());
                storyData.put("title", story.getTitle());
                storyData.put("coverImage", story.getCoverImage() != null ? 
                    story.getCoverImage() : "/placeholder.svg?height=150&width=100");
                return storyData;
            }).collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            System.err.println("Error fetching random stories: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list instead of null
        }
    }
}

