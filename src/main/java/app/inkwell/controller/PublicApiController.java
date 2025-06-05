package app.inkwell.controller;

import app.inkwell.model.Story;
import app.inkwell.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

    @Autowired
    private StoryService storyService;
    
    @GetMapping("/featured-stories")
    public List<Map<String, Object>> getFeaturedStories() {
        // Get a random selection of published stories
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "readCount"));
        List<Story> stories = storyService.getPublishedStories(pageable).getContent();
        
        // Shuffle the stories to get random selection
        Collections.shuffle(stories);
        
        // Convert to simplified format with just what we need
        return stories.stream()
            .map(story -> {
                Map<String, Object> storyMap = new HashMap<>();
                storyMap.put("id", story.getId());
                storyMap.put("title", story.getTitle());
                storyMap.put("coverImage", story.getCoverImage() != null ? story.getCoverImage() : "/uploads/default-cover.jpg");
                return storyMap;
            })
            .collect(Collectors.toList());
    }
}

