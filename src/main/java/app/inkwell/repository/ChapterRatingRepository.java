package app.inkwell.repository;

import app.inkwell.model.Chapter;
import app.inkwell.model.ChapterRating;
import app.inkwell.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRatingRepository extends JpaRepository<ChapterRating, Long> {
    // Find ratings by chapter
    List<ChapterRating> findByChapterOrderByCreatedAtDesc(Chapter chapter);
    Page<ChapterRating> findByChapterAndVisibleTrueOrderByCreatedAtDesc(Chapter chapter, Pageable pageable);
    
    // Find ratings by user
    List<ChapterRating> findByUserOrderByCreatedAtDesc(User user);
    
    // Find rating by chapter and user (to check if user already rated)
    Optional<ChapterRating> findByChapterAndUser(Chapter chapter, User user);
    
    // Calculate average rating for a chapter
    @Query("SELECT AVG(r.rating) FROM ChapterRating r WHERE r.chapter = ?1 AND r.visible = true")
    Double calculateAverageRating(Chapter chapter);
    
    // Count ratings for a chapter
    long countByChapterAndVisibleTrue(Chapter chapter);
    
    // Find top helpful ratings
    @Query("SELECT r FROM ChapterRating r WHERE r.chapter = ?1 AND r.visible = true ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    List<ChapterRating> findTopRatings(Chapter chapter, Pageable pageable);


@Query("SELECT cr FROM ChapterRating cr WHERE cr.chapter.id = :chapterId AND cr.user.id = :userId")
List<ChapterRating> findByChapterIdAndUserId(@Param("chapterId") Long chapterId, @Param("userId") Long userId);

@Query("SELECT AVG(cr.rating) FROM ChapterRating cr WHERE cr.chapter.id = :chapterId AND cr.visible = true")
Double calculateAverageRatingForChapter(@Param("chapterId") Long chapterId);

@Query("SELECT COUNT(cr) FROM ChapterRating cr WHERE cr.chapter.story.id = :storyId AND cr.createdAt > :since")
int countByStoryAndCreatedAtAfter(Long storyId, LocalDateTime since);
}