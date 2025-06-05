package app.inkwell.service;

import app.inkwell.model.Chapter;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.repository.ChapterLikeRepository;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.CommentRepository;
import app.inkwell.repository.StoryLikeRepository;
import app.inkwell.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {
    
    @Autowired
    private StoryRepository storyRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private StoryLikeRepository storylikeRepository;

    @Autowired
    private ChapterLikeRepository chapterLikeRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    public Map<String, Integer> getUserStatistics(User user) {
        Map<String, Integer> stats = new HashMap<>();
        
        try {
            List<Story> userStories = storyRepository.findByAuthor(user);
            
            int totalReads = 0;
            int totalStoryLikes = 0;
            int totalChapterLikes = 0;
            int totalComments = 0;
            
            for (Story story : userStories) {
                totalReads += story.getReadCount();
                totalStoryLikes += storylikeRepository.countByStory(story);
                totalComments += commentRepository.findByStory(story, null).getTotalElements();
                
                // Add chapter likes
                List<Chapter> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(story);
                for (Chapter chapter : chapters) {
                    totalChapterLikes += chapterLikeRepository.countByChapter(chapter);
                }
            }
            
            stats.put("totalStories", userStories.size());
            stats.put("totalReads", totalReads);
            stats.put("totalStoryLikes", totalStoryLikes);
            stats.put("totalChapterLikes", totalChapterLikes);
            stats.put("totalLikes", totalStoryLikes + totalChapterLikes);
            stats.put("totalComments", totalComments);
        } catch (Exception e) {
            e.printStackTrace();
            // Provide defaults in case of error
            stats.put("totalStories", 0);
            stats.put("totalReads", 0);
            stats.put("totalStoryLikes", 0);
            stats.put("totalChapterLikes", 0);
            stats.put("totalLikes", 0);
            stats.put("totalComments", 0);
        }
        
        return stats;
      }
    
    public Map<String, Object> getStoryStatistics(Story story) {
        int totalstoryLikes = storylikeRepository.countByStory(story);
        long totalComments = commentRepository.findByStory(story, null).getTotalElements();
        List<Chapter> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(story);
      int totalChapters = chapters.size();
      
        // Calculate chapter likes
        int totalChapterLikes = 0;
        for (Chapter chapter : chapters) {
            totalChapterLikes += chapterLikeRepository.countByChapter(chapter);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("reads", story.getReadCount());
        stats.put("likes", totalstoryLikes);
        stats.put("chapterLikes", totalChapterLikes);
        stats.put("comments", totalComments);
        stats.put("chapters", totalChapters);
        
        return stats;
    }

    public Map<String, Object> getChapterStatistics(Chapter chapter) {
        int likes = chapterLikeRepository.countByChapter(chapter);
        long comments = commentRepository.findByChapter(chapter, null).getTotalElements();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("reads", chapter.getReadCount());
        stats.put("likes", likes);
        stats.put("comments", comments);
        
        return stats;
    }
    
}

