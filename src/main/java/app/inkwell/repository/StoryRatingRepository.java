package app.inkwell.repository;

import app.inkwell.model.Story;
import app.inkwell.model.StoryRating;
import app.inkwell.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRatingRepository extends JpaRepository<StoryRating, Long> {
    // Find ratings by story
    List<StoryRating> findByStoryOrderByCreatedAtDesc(Story story);
    Page<StoryRating> findByStoryAndVisibleTrueOrderByCreatedAtDesc(Story story, Pageable pageable);
    
    // Find ratings by user
    List<StoryRating> findByUserOrderByCreatedAtDesc(User user);
    
    // Find rating by story and user (to check if user already rated)
    Optional<StoryRating> findByStoryAndUser(Story story, User user);
    
    // Calculate average rating for a story
    @Query("SELECT AVG(r.rating) FROM StoryRating r WHERE r.story = ?1 AND r.visible = true")
    Double calculateAverageRating(Story story);
    
    // Count ratings for a story
    long countByStoryAndVisibleTrue(Story story);
    
    // Find top helpful ratings
    @Query("SELECT r FROM StoryRating r WHERE r.story = ?1 AND r.visible = true ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    List<StoryRating> findTopRatings(Story story, Pageable pageable);

        @Query("SELECT COUNT(r) FROM StoryRating r WHERE r.story = :story AND r.createdAt > :since")
    int countByStoryAndCreatedAtAfter(Story story, LocalDateTime since);
}