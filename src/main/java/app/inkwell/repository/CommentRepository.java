package app.inkwell.repository;

import app.inkwell.model.Chapter;
import app.inkwell.model.Comment;
import app.inkwell.model.Story;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    Page<Comment> findByStory(Story story, Pageable pageable);
    
    Page<Comment> findByChapter(Chapter chapter, Pageable pageable);
    
    @Query("SELECT c FROM Comment c WHERE c.story = ?1 ORDER BY c.likeCount DESC")
    Page<Comment> findByStoryOrderByLikes(Story story, Pageable pageable);

    Page<Comment> findByStoryIdAndChapterId(Long storyId, Long chapterId, Pageable pageable);

    Page<Comment> findByStoryIdAndChapterIdIsNull(Long storyId, Pageable pageable);

        @Query("SELECT COUNT(c) FROM Comment c WHERE c.story.id = :storyId AND c.createdAt > :sinceDate")
    int countByStoryAndCreatedAtAfter(@Param("storyId") Long storyId, @Param("sinceDate") LocalDateTime sinceDate);
}

