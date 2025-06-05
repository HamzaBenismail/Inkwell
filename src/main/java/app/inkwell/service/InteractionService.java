package app.inkwell.service;

import app.inkwell.model.*;
import app.inkwell.repository.ChapterLikeRepository;
import app.inkwell.repository.CommentRepository;
import app.inkwell.repository.StoryLikeRepository;
import app.inkwell.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InteractionService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private StoryLikeRepository storyLikeRepository;
    
    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private ChapterLikeRepository chapterLikeRepository;
    
    @Transactional
    public Comment addComment(User user, Story story, Chapter chapter, String content, Comment parentComment) {
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setStory(story);
        comment.setChapter(chapter);
        comment.setContent(content);
        comment.setParentComment(parentComment);
        comment.setCreatedAt(LocalDateTime.now());
        
        return commentRepository.save(comment);
    }
    
    @Transactional
    public void likeStory(User user, Story story) {
        // Check if already liked
        Optional<StoryLike> existingLike = storyLikeRepository.findByUserAndStory(user, story);
        
        if (existingLike.isEmpty()) {
            StoryLike like = new StoryLike();
            like.setUser(user);
            like.setStory(story);
            like.setCreatedAt(LocalDateTime.now());
            storyLikeRepository.save(like);
        }
    }
    
    @Transactional
    public void unlikeStory(User user, Story story) {
        storyLikeRepository.findByUserAndStory(user, story)
            .ifPresent(storyLikeRepository::delete);
    }

    @Transactional
  public void likeChapter(User user, Chapter chapter) {
      // Check if already liked
      Optional<ChapterLike> existingLike = chapterLikeRepository.findByUserAndChapter(user, chapter);
      
      if (existingLike.isEmpty()) {
          ChapterLike like = new ChapterLike();
          like.setUser(user);
          like.setChapter(chapter);
          like.setCreatedAt(LocalDateTime.now());
          chapterLikeRepository.save(like);
      }
  }

  public boolean hasUserLikedChapter(User user, Chapter chapter) {
    return chapterLikeRepository.findByUserAndChapter(user, chapter).isPresent();
}
  
  @Transactional
  public void unlikeChapter(User user, Chapter chapter) {
      chapterLikeRepository.findByUserAndChapter(user, chapter)
          .ifPresent(chapterLikeRepository::delete);
  }
    
    public boolean hasUserLikedStory(User user, Story story) {
        return storyLikeRepository.findByUserAndStory(user, story).isPresent();
    }
    
    public Page<Comment> getStoryComments(Story story, Pageable pageable) {
        return commentRepository.findByStory(story, pageable);
    }
    
    public Page<Comment> getChapterComments(Chapter chapter, Pageable pageable) {
        return commentRepository.findByChapter(chapter, pageable);
    }
    
    public Page<Comment> getMostLikedComments(Story story, Pageable pageable) {
        return commentRepository.findByStoryOrderByLikes(story, pageable);
    }
}

