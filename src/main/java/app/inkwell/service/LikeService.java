package app.inkwell.service;

import app.inkwell.model.Story;
import app.inkwell.model.StoryLike;
import app.inkwell.model.User;
import app.inkwell.repository.StoryLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LikeService {

    @Autowired
    private StoryLikeRepository storyLikeRepository;

    public boolean hasUserLikedStory(User user, Story story) {
        if (user == null || story == null) {
            return false;
        }
        return storyLikeRepository.findByUserAndStory(user, story).isPresent();
    }
    
    public long getStoryLikeCount(Story story) {
        if (story == null) {
            return 0;
        }
        return storyLikeRepository.countByStory(story);
    }
    
    public boolean toggleStoryLike(User user, Story story) {
        if (user == null || story == null) {
            return false;
        }
        
        Optional<StoryLike> existingLike = storyLikeRepository.findByUserAndStory(user, story);
        
        if (existingLike.isPresent()) {
            // Unlike
            storyLikeRepository.delete(existingLike.get());
            return false;
        } else {
            // Like
            StoryLike newLike = new StoryLike();
            newLike.setUser(user);
            newLike.setStory(story);
            newLike.setCreatedAt(LocalDateTime.now());
            storyLikeRepository.save(newLike);
            return true;
        }
    }
    
    // Add this new method to get user's liked stories
    public List<Story> getLikedStoriesByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return storyLikeRepository.findByUser(user).stream()
            .map(StoryLike::getStory)
            .collect(Collectors.toList());
    }
}