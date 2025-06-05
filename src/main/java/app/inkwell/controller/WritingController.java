package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.Character;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.model.WorldBuildingElement;
import app.inkwell.service.StoryService;
import app.inkwell.service.WritingPlanningService;
import app.inkwell.model.StoryPlanningElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import app.inkwell.model.Version;
import app.inkwell.service.VersionService;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/writing")
public class WritingController {

  @Autowired
  private StoryService storyService;

  // Add this field to the class
    @Autowired
    private VersionService versionService;
  
  @Autowired
  private WritingPlanningService writingPlanningService;
  
  // List endpoints for each content type
  @GetMapping("/chapters/list")
  public ResponseEntity<?> listChapters(
          @AuthenticationPrincipal User user,
          @RequestParam("storyId") Long storyId) {
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      List<Chapter> chapters = storyService.getStoryChapters(storyOpt.get());
      
      // Convert to simplified DTOs to avoid circular references
      List<Map<String, Object>> chapterDtos = chapters.stream()
          .map(chapter -> {
              Map<String, Object> dto = new HashMap<>();
              dto.put("id", chapter.getId());
              dto.put("title", chapter.getTitle());
              dto.put("content", chapter.getContent());
              dto.put("chapterNumber", chapter.getChapterNumber());
              dto.put("published", chapter.getPublished());
              dto.put("soundCloudTrackUrl", chapter.getSoundCloudTrackUrl());
              return dto;
          })
          .collect(Collectors.toList());
      
      return ResponseEntity.ok(chapterDtos);
  }
  
  @GetMapping("/characters/list")
  public ResponseEntity<?> listCharacters(
          @AuthenticationPrincipal User user,
          @RequestParam("storyId") Long storyId) {
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      List<Character> characters = writingPlanningService.getStoryCharacters(storyOpt.get());
      
      // Convert to simplified DTOs to avoid circular references
      List<Map<String, Object>> characterDtos = characters.stream()
          .map(character -> {
              Map<String, Object> dto = new HashMap<>();
              dto.put("id", character.getId());
              dto.put("name", character.getName());
              dto.put("description", character.getDescription());
              return dto;
          })
          .collect(Collectors.toList());
      
      return ResponseEntity.ok(characterDtos);
  }
  
  @GetMapping("/worldbuilding/list")
  public ResponseEntity<?> listWorldElements(
          @AuthenticationPrincipal User user,
          @RequestParam("storyId") Long storyId) {
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      List<WorldBuildingElement> elements = writingPlanningService.getStoryWorldElements(storyOpt.get());
      
      // Convert to simplified DTOs to avoid circular references
      List<Map<String, Object>> elementDtos = elements.stream()
          .map(element -> {
              Map<String, Object> dto = new HashMap<>();
              dto.put("id", element.getId());
              dto.put("title", element.getTitle());
              dto.put("content", element.getContent());
              return dto;
          })
          .collect(Collectors.toList());
      
      return ResponseEntity.ok(elementDtos);
  }
  
  // Save or update a chapter
  @PostMapping("/chapters/new")
public ResponseEntity<?> createChapter(
        @AuthenticationPrincipal User user,
        @RequestBody Map<String, Object> payload) {
    
    Long storyId = null;
    try {
        storyId = Long.parseLong(payload.get("storyId").toString());
    } catch (NumberFormatException | NullPointerException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid story ID"));
    }
    
    Optional<Story> storyOpt = storyService.getStoryById(storyId);
    if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
        return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
    }
    
    Story story = storyOpt.get();
    String title = (String) payload.get("title");
    String content = (String) payload.get("content");
    
    // Get the next chapter number
    int chapterNumber = story.getChapters().size() + 1;
    
    // Create the new chapter
    Chapter chapter = storyService.addChapter(story, title, content, chapterNumber);
    
    // Set SoundCloud track URL if provided
    if (payload.containsKey("soundCloudTrackUrl")) {
        chapter.setSoundCloudTrackUrl((String) payload.get("soundCloudTrackUrl"));
        chapter = storyService.saveChapter(chapter);
    }
    
    // Create the initial version for this chapter
    Version version = versionService.createChapterVersion(chapter);
    
    Map<String, Object> response = new HashMap<>();
    response.put("id", chapter.getId());
    response.put("title", chapter.getTitle());
    response.put("soundCloudTrackUrl", chapter.getSoundCloudTrackUrl());
    response.put("versionNumber", version.getVersionNumber());
    response.put("message", "Chapter created successfully");
    
    return ResponseEntity.ok(response);
}
  
  @PutMapping("/chapters/{id}")
public ResponseEntity<?> updateChapter(
        @AuthenticationPrincipal User user,
        @PathVariable("id") Long chapterId,
        @RequestBody Map<String, Object> payload) {
    
    Optional<Chapter> chapterOpt = storyService.getChapterById(chapterId);
    if (chapterOpt.isEmpty() || !chapterOpt.get().getStory().getAuthor().getId().equals(user.getId())) {
        return ResponseEntity.badRequest().body(Map.of("error", "Chapter not found or not owned by user"));
    }
    
    Chapter chapter = chapterOpt.get();
    String title = (String) payload.get("title");
    String content = (String) payload.get("content");
    
    // Update the chapter
    chapter.setTitle(title);
    chapter.setContent(content);
    chapter.setWordCount(storyService.countWords(content));
    
    // Update SoundCloud track URL
    if (payload.containsKey("soundCloudTrackUrl")) {
        chapter.setSoundCloudTrackUrl((String) payload.get("soundCloudTrackUrl"));
    }
    
    // Save the updated chapter
    chapter = storyService.saveChapter(chapter);
    
    // Create a new version for this chapter
    Version version = versionService.createChapterVersion(chapter);
    
    Map<String, Object> response = new HashMap<>();
    response.put("id", chapter.getId());
    response.put("title", chapter.getTitle());
    response.put("soundCloudTrackUrl", chapter.getSoundCloudTrackUrl());
    response.put("versionNumber", version.getVersionNumber());
    response.put("message", "Chapter updated successfully");
    
    return ResponseEntity.ok(response);
}
  
  @PostMapping("/chapters/{id}/publish")
  public ResponseEntity<?> publishChapter(
          @AuthenticationPrincipal User user,
          @PathVariable("id") Long chapterId) {
      
      Optional<Chapter> chapterOpt = storyService.getChapterById(chapterId);
      if (chapterOpt.isEmpty() || !chapterOpt.get().getStory().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Chapter not found or not owned by user"));
      }
      
      Chapter chapter = chapterOpt.get();
      
      // Publish the chapter
      chapter.setPublished(true);
      storyService.saveChapter(chapter);
      
      // If this is the first published chapter, also publish the story
      Story story = chapter.getStory();
      if (!story.getPublished()) {
          story.setPublished(true);
          storyService.saveStory(story);
      }
      
      return ResponseEntity.ok(Map.of("message", "Chapter published successfully"));
  }
  
  // Characters endpoints
  @PostMapping("/characters/new")
  public ResponseEntity<?> createCharacter(
          @AuthenticationPrincipal User user,
          @RequestBody Map<String, String> payload) {
      
      Long storyId = null;
      try {
          storyId = Long.parseLong(payload.get("storyId"));
      } catch (NumberFormatException | NullPointerException e) {
          return ResponseEntity.badRequest().body(Map.of("error", "Invalid story ID"));
      }
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      Story story = storyOpt.get();
      String name = payload.get("title"); // Using "title" as the character name
      String description = payload.get("content");
      
      Character character = writingPlanningService.createCharacter(story, name, description);
      
      // Create initial version
    Version version = versionService.createCharacterVersion(character);

      Map<String, Object> response = new HashMap<>();
      response.put("id", character.getId());
      response.put("name", character.getName());
      response.put("versionNumber", version.getVersionNumber());
      response.put("message", "Character created successfully");
      
      return ResponseEntity.ok(response);
  }
  
  @PutMapping("/characters/{id}")
  public ResponseEntity<?> updateCharacter(
          @AuthenticationPrincipal User user,
          @PathVariable("id") Long characterId,
          @RequestBody Map<String, String> payload) {
      
      Optional<Character> characterOpt = writingPlanningService.getCharacterById(characterId);
      if (characterOpt.isEmpty() || !characterOpt.get().getStory().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Character not found or not owned by user"));
      }
      
      Character character = characterOpt.get();
      String name = payload.get("title");
      String description = payload.get("content");
      
      character.setName(name);
      character.setDescription(description);

      // Create a new version
      Version version = versionService.createCharacterVersion(character);
      
      character = writingPlanningService.saveCharacter(character);
      
      Map<String, Object> response = new HashMap<>();
      response.put("id", character.getId());
      response.put("name", character.getName());
      response.put("versionNumber", version.getVersionNumber());
      response.put("message", "Character updated successfully");
      
      return ResponseEntity.ok(response);
  }
  
  // World building endpoints
  @PostMapping("/worldbuilding/new")
  public ResponseEntity<?> createWorldElement(
          @AuthenticationPrincipal User user,
          @RequestBody Map<String, String> payload) {
      
      Long storyId = null;
      try {
          storyId = Long.parseLong(payload.get("storyId"));
      } catch (NumberFormatException | NullPointerException e) {
          return ResponseEntity.badRequest().body(Map.of("error", "Invalid story ID"));
      }
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      Story story = storyOpt.get();
      String title = payload.get("title");
      String content = payload.get("content");
      
      WorldBuildingElement element = writingPlanningService.createWorldElement(story, title, content);
      
      // Create initial version
    Version version = versionService.createWorldBuildingVersion(element);

      Map<String, Object> response = new HashMap<>();
      response.put("id", element.getId());
      response.put("title", element.getTitle());
      response.put("versionNumber", version.getVersionNumber());
      response.put("message", "World building element created successfully");
      
      return ResponseEntity.ok(response);
  }
  
  @PutMapping("/worldbuilding/{id}")
  public ResponseEntity<?> updateWorldElement(
          @AuthenticationPrincipal User user,
          @PathVariable("id") Long elementId,
          @RequestBody Map<String, String> payload) {
      
      Optional<WorldBuildingElement> elementOpt = writingPlanningService.getWorldElementById(elementId);
      if (elementOpt.isEmpty() || !elementOpt.get().getStory().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "World building element not found or not owned by user"));
      }
      
      WorldBuildingElement element = elementOpt.get();
      String title = payload.get("title");
      String content = payload.get("content");
      
      element.setTitle(title);
      element.setContent(content);
      
      element = writingPlanningService.saveWorldElement(element);
      
      // Create a new version
    Version version = versionService.createWorldBuildingVersion(element);

      Map<String, Object> response = new HashMap<>();
      response.put("id", element.getId());
      response.put("title", element.getTitle());
      response.put("versionNumber", version.getVersionNumber());
      response.put("message", "World building element updated successfully");
      
      return ResponseEntity.ok(response);
  }

  // Add a new endpoint to handle saving planning elements
  @PostMapping("/planning/save")
  public ResponseEntity<?> savePlanningElements(
          @AuthenticationPrincipal User user,
          @RequestBody Map<String, Object> payload) {
      
      try {
          Long storyId = Long.parseLong(payload.get("storyId").toString());
          
          Optional<Story> storyOpt = storyService.getStoryById(storyId);
          if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
              return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
          }
          
          Story story = storyOpt.get();
          
          // Get the planning elements from the payload
          List<Map<String, Object>> elements = (List<Map<String, Object>>) payload.get("elements");
          
          // Clear existing planning elements for this story
          // This is a simplified approach - in a real app, you might want to update existing elements
          // rather than deleting and recreating them
          writingPlanningService.deleteAllPlanningElements(story);
          
          // Create new planning elements
          for (Map<String, Object> element : elements) {
              String title = (String) element.get("title");
              String content = (String) element.get("content");
              String color = (String) element.get("color");
              int posX = ((Number) element.get("positionX")).intValue();
              int posY = ((Number) element.get("positionY")).intValue();
              int width = ((Number) element.get("width")).intValue();
              int height = ((Number) element.get("height")).intValue();
              
              writingPlanningService.createPlanningElement(story, title, content, color, posX, posY, width, height);
          }
          
          return ResponseEntity.ok(Map.of("message", "Planning elements saved successfully"));
      } catch (Exception e) {
          return ResponseEntity.badRequest().body(Map.of("error", "Failed to save planning elements: " + e.getMessage()));
      }
  }

  // Add a new endpoint to fetch planning elements
  @GetMapping("/planning/list")
  public ResponseEntity<?> listPlanningElements(
          @AuthenticationPrincipal User user,
          @RequestParam("storyId") Long storyId) {
      
      Optional<Story> storyOpt = storyService.getStoryById(storyId);
      if (storyOpt.isEmpty() || !storyOpt.get().getAuthor().getId().equals(user.getId())) {
          return ResponseEntity.badRequest().body(Map.of("error", "Story not found or not owned by user"));
      }
      
      List<StoryPlanningElement> elements = writingPlanningService.getStoryPlanningElements(storyOpt.get());
      
      // Convert to simplified DTOs to avoid circular references
      List<Map<String, Object>> elementDtos = elements.stream()
          .map(element -> {
              Map<String, Object> dto = new HashMap<>();
              dto.put("id", element.getId());
              dto.put("title", element.getTitle());
              dto.put("content", element.getContent());
              dto.put("color", element.getColor());
              dto.put("positionX", element.getPositionX());
              dto.put("positionY", element.getPositionY());
              dto.put("width", element.getWidth());
              dto.put("height", element.getHeight());
              return dto;
          })
          .collect(Collectors.toList());
      
      return ResponseEntity.ok(elementDtos);
  }

  // Endpoint to get all versions for a specific content item
@GetMapping("/{contentType}/versions/{contentId}")
public ResponseEntity<?> getVersions(
        @AuthenticationPrincipal User user,
        @PathVariable("contentType") String contentType,
        @PathVariable("contentId") Long contentId) {
    
    // Validate content ownership based on content type
    boolean userOwnsContent = false;
    
    if ("chapters".equals(contentType)) {
        Optional<Chapter> content = storyService.getChapterById(contentId);
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    } else if ("characters".equals(contentType)) {
        Optional<Character> content = writingPlanningService.getCharacterById(contentId);
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    } else if ("worldbuilding".equals(contentType)) {
        Optional<WorldBuildingElement> content = writingPlanningService.getWorldElementById(contentId);
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    }
    
    if (!userOwnsContent) {
        return ResponseEntity.badRequest().body(Map.of("error", "Content not found or not owned by user"));
    }
    
    // Map API contentType to entity contentType
    String entityContentType = mapContentTypeToEntityType(contentType);
    
    // Get versions
    List<Version> versions = versionService.getVersions(entityContentType, contentId);
    
    // Map to DTOs to avoid circular references
    List<Map<String, Object>> versionDtos = versions.stream()
        .map(version -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", version.getId());
            dto.put("content", version.getContent());
            dto.put("soundCloudTrackUrl", version.getSoundCloudTrackUrl());
            dto.put("createdAt", version.getCreatedAt());
            dto.put("versionNumber", version.getVersionNumber());
            return dto;
        })
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(versionDtos);
}

// Endpoint to get a specific version
@GetMapping("/{contentType}/version/{versionId}")
public ResponseEntity<?> getVersion(
        @AuthenticationPrincipal User user,
        @PathVariable("contentType") String contentType,
        @PathVariable("versionId") Long versionId) {
    
    Optional<Version> versionOpt = versionService.getVersionById(versionId);
    
    if (versionOpt.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    
    Version version = versionOpt.get();
    
    // Validate ownership based on content type
    boolean userOwnsContent = false;
    String entityContentType = mapContentTypeToEntityType(contentType);
    
    if (!entityContentType.equals(version.getContentType())) {
        return ResponseEntity.badRequest().body(Map.of("error", "Version does not match content type"));
    }
    
    if ("chapter".equals(version.getContentType())) {
        Optional<Chapter> content = storyService.getChapterById(version.getContentId());
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    } else if ("character".equals(version.getContentType())) {
        Optional<Character> content = writingPlanningService.getCharacterById(version.getContentId());
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    } else if ("worldbuilding".equals(version.getContentType())) {
        Optional<WorldBuildingElement> content = writingPlanningService.getWorldElementById(version.getContentId());
        userOwnsContent = content.isPresent() && content.get().getStory().getAuthor().getId().equals(user.getId());
    }
    
    if (!userOwnsContent) {
        return ResponseEntity.badRequest().body(Map.of("error", "Content not found or not owned by user"));
    }
    
    // Create DTO for response
    Map<String, Object> versionDto = new HashMap<>();
    versionDto.put("id", version.getId());
    versionDto.put("content", version.getContent());
    versionDto.put("soundCloudTrackUrl", version.getSoundCloudTrackUrl());
    versionDto.put("createdAt", version.getCreatedAt());
    versionDto.put("versionNumber", version.getVersionNumber());
    
    return ResponseEntity.ok(versionDto);
}

// Helper method to map API content type to entity content type
private String mapContentTypeToEntityType(String apiContentType) {
    switch (apiContentType) {
        case "chapters":
            return "chapter";
        case "characters":
            return "character"; 
        case "worldbuilding":
            return "worldbuilding";
        default:
            return apiContentType;
    }
}
}

