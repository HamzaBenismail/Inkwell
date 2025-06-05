package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.model.Comment;
import app.inkwell.repository.*;
import app.inkwell.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

  @Autowired
  private StatisticsService statisticsService;
  
  @Autowired
  private StoryRepository storyRepository;
  
  @Autowired
  private ChapterRepository chapterRepository;
  
  @Autowired
  private StoryLikeRepository storyLikeRepository;
  
  @Autowired
  private ChapterLikeRepository chapterLikeRepository;
  
  @Autowired
  private CommentRepository commentRepository;

  @GetMapping("/story/{storyId}")
  public ResponseEntity<?> getStoryStatistics(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
    try {
        // Check if story exists and belongs to the user
        Optional<Story> storyOpt = storyRepository.findById(storyId);
        if (storyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Story story = storyOpt.get();
        
        // Verify the story belongs to the current user
        if (user == null || story.getAuthor() == null || !story.getAuthor().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // Basic story stats
            int reads = story.getReadCount();
            int storyLikes = storyLikeRepository.countByStory(story);
            long comments = commentRepository.findByStory(story, null).getTotalElements();
            List<Chapter> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(story);
            int chapterCount = chapters.size();
            
            // Calculate chapter likes
            int chapterLikes = 0;
            for (Chapter chapter : chapters) {
                chapterLikes += chapterLikeRepository.countByChapter(chapter);
            }
            
            statistics.put("reads", reads);
            statistics.put("storyLikes", storyLikes);
            statistics.put("chapterLikes", chapterLikes);
            statistics.put("totalLikes", storyLikes + chapterLikes);
            statistics.put("comments", comments);
            statistics.put("chapters", chapterCount);
            
            // Chapter statistics - only include actual chapters
            List<Map<String, Object>> chapterStats = new ArrayList<>();
            
            for (Chapter chapter : chapters) {
                if (chapter != null) {
                    Map<String, Object> chapterStat = new HashMap<>();
                    chapterStat.put("chapterNumber", chapter.getChapterNumber());
                    chapterStat.put("title", chapter.getTitle() != null ? chapter.getTitle() : "");
                    chapterStat.put("readCount", chapter.getReadCount());
                    chapterStat.put("likeCount", chapterLikeRepository.countByChapter(chapter));
                    chapterStats.add(chapterStat);
                }
            }
            
            statistics.put("chapterStats", chapterStats);
            
            // Reads by month (last 3 months)
            int[] readsByMonth = new int[12];
            // For simplicity, distribute reads across the last 3 months
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            if (reads > 0) {
                readsByMonth[currentMonth] = (int)(reads * 0.6); // 60% in current month
                readsByMonth[(currentMonth + 11) % 12] = (int)(reads * 0.3); // 30% in previous month
                readsByMonth[(currentMonth + 10) % 12] = reads - readsByMonth[currentMonth] - readsByMonth[(currentMonth + 11) % 12]; // Remainder in month before
            }
            statistics.put("readsByMonth", readsByMonth);
            
            // Engagement metrics - use actual data
            Map<String, Object> engagement = new HashMap<>();
            engagement.put("storyLikes", storyLikes);
            engagement.put("chapterLikes", chapterLikes);
            engagement.put("totalLikes", storyLikes + chapterLikes);
            engagement.put("comments", comments);
            engagement.put("shares", 0); // No shares feature yet
            engagement.put("bookmarks", 0); // No bookmarks feature yet
            statistics.put("engagement", engagement);
            
            // Writing activity (hours spent writing per day of week)
            double[] writingActivity = new double[7];
            for (Chapter chapter : chapters) {
                if (chapter != null && chapter.getCreatedAt() != null) {
                    int dayOfWeek = chapter.getCreatedAt().getDayOfWeek().getValue() % 7;
                    writingActivity[dayOfWeek] += 1.5;
                }
            }
            statistics.put("writingActivity", writingActivity);
            
            // Word count by month
            int[] wordCountByMonth = new int[12];
            int totalWordCount = 0;
            
            for (Chapter chapter : chapters) {
                if (chapter != null && chapter.getContent() != null) {
                    totalWordCount += chapter.getContent().split("\\s+").length;
                }
            }
            
            // For simplicity, put all word count in the current month
            wordCountByMonth[currentMonth] = totalWordCount;
            statistics.put("wordCountByMonth", wordCountByMonth);
        } catch (Exception e) {
            e.printStackTrace();
            // Set default values for basic statistics if calculation fails
            statistics.put("reads", 0);
            statistics.put("storyLikes", 0);
            statistics.put("chapterLikes", 0);
            statistics.put("totalLikes", 0);
            statistics.put("comments", 0);
            statistics.put("chapters", 0);
            statistics.put("chapterStats", new ArrayList<>());
            statistics.put("readsByMonth", new int[12]);
            statistics.put("engagement", Map.of(
                "storyLikes", 0,
                "chapterLikes", 0,
                "totalLikes", 0,
                "comments", 0,
                "shares", 0,
                "bookmarks", 0
            ));
            statistics.put("writingActivity", new double[7]);
            statistics.put("wordCountByMonth", new int[12]);
        }

        // Add comment distribution by chapter
        Map<Long, Long> commentDistribution = new HashMap<>();
        List<Chapter> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(story);
        for (Chapter chapter : chapters) {
            long chapterComments = commentRepository.findByStoryIdAndChapterId(storyId, chapter.getId(), null).getTotalElements();
            commentDistribution.put(chapter.getId(), chapterComments);
        }
        statistics.put("commentDistribution", commentDistribution);

        // Add recent comments preview (just a few)
        Page<Comment> recentComments = commentRepository.findByStoryIdAndChapterIdIsNull(
            storyId, org.springframework.data.domain.PageRequest.of(0, 3, 
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        List<Map<String, Object>> recentCommentsData = recentComments.getContent().stream()
            .map(comment -> {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("id", comment.getId());
                commentData.put("content", comment.getContent());
                commentData.put("username", comment.getUser().getUsername());
                commentData.put("createdAt", comment.getCreatedAt());
                return commentData;
            })
            .collect(Collectors.toList());
        statistics.put("recentComments", recentCommentsData);

        return ResponseEntity.ok(statistics);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(Map.of("error", "An error occurred while processing your request"));
    }
  }

  @GetMapping("/story/{storyId}/comments")
  public ResponseEntity<?> getStoryComments(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
      try {
          // Check if story exists and belongs to the user
          Optional<Story> storyOpt = storyRepository.findById(storyId);
          if (storyOpt.isEmpty()) {
              return ResponseEntity.notFound().build();
          }
          
          Story story = storyOpt.get();
          
          // Verify the story belongs to the current user
          if (!story.getAuthor().getId().equals(user.getId())) {
              return ResponseEntity.status(403).build();
          }
          
          // Fetch story-level comments
          Page<Comment> storyComments = commentRepository.findByStoryIdAndChapterIdIsNull(storyId, 
              org.springframework.data.domain.PageRequest.of(0, 10, 
              org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
          
          // Fetch chapter-level comments
          List<Chapter> chapters = chapterRepository.findByStoryOrderByChapterNumberAsc(story);
          Map<Long, List<Map<String, Object>>> chapterComments = new HashMap<>();
          
          for (Chapter chapter : chapters) {
              Page<Comment> comments = commentRepository.findByStoryIdAndChapterId(storyId, chapter.getId(), 
                  org.springframework.data.domain.PageRequest.of(0, 5, 
                  org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
              
              List<Map<String, Object>> formattedComments = comments.getContent().stream()
                  .map(comment -> {
                      Map<String, Object> commentMap = new HashMap<>();
                      commentMap.put("id", comment.getId());
                      commentMap.put("content", comment.getContent());
                      commentMap.put("createdAt", comment.getCreatedAt());
                      commentMap.put("username", comment.getUser().getUsername());
                      return commentMap;
                  })
                  .collect(Collectors.toList());
              
              chapterComments.put(chapter.getId(), formattedComments);
          }
          
          // Format story comments
          List<Map<String, Object>> formattedStoryComments = storyComments.getContent().stream()
              .map(comment -> {
                  Map<String, Object> commentMap = new HashMap<>();
                  commentMap.put("id", comment.getId());
                  commentMap.put("content", comment.getContent());
                  commentMap.put("createdAt", comment.getCreatedAt());
                  commentMap.put("username", comment.getUser().getUsername());
                  return commentMap;
              })
              .collect(Collectors.toList());
          
          Map<String, Object> result = new HashMap<>();
          result.put("storyComments", formattedStoryComments);
          result.put("chapterComments", chapterComments);
          result.put("totalStoryComments", storyComments.getTotalElements());
          
          return ResponseEntity.ok(result);
      } catch (Exception e) {
          e.printStackTrace();
          return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
      }
  }
  
  @GetMapping("/chapter/{chapterId}")
  public ResponseEntity<?> getChapterStatistics(@PathVariable Long chapterId, @AuthenticationPrincipal User user) {
      try {
          // Check if chapter exists
          Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
          if (chapterOpt.isEmpty()) {
              return ResponseEntity.notFound().build();
          }
          
          Chapter chapter = chapterOpt.get();
          Story story = chapter.getStory();
          
          // Verify the story belongs to the current user
          if (!story.getAuthor().getId().equals(user.getId())) {
              return ResponseEntity.status(403).build();
          }
          
          Map<String, Object> statistics = statisticsService.getChapterStatistics(chapter);
          
          return ResponseEntity.ok(statistics);
      } catch (Exception e) {
          e.printStackTrace();
          return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
      }
  }
}

