package app.inkwell.repository;

import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    
    Page<Story> findByPublishedTrue(Pageable pageable);
    
    List<Story> findByPublishedTrue();
    
    List<Story> findByAuthor(User author);
    
    Page<Story> findByAuthor(String author, Pageable pageable);
    
    @Query("SELECT s FROM Story s WHERE s.published = true AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Story> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
    
    List<Story> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    
    List<Story> findTop6ByPublishedTrueOrderByCreatedAtDesc();
    
    List<Story> findTop6ByPublishedTrueOrderByReadCountDesc();

    List<Story> findTop6ByPublishedTrueOrderByLastUpdatedDesc();

    @Query("SELECT s FROM Story s WHERE s.published = true AND NOT EXISTS (SELECT st FROM s.tags st WHERE st.name = ?1)")
  Page<Story> findStoriesWithoutTag(String tagName, Pageable pageable);

  @Query("SELECT s FROM Story s JOIN s.tags t WHERE s.published = true AND t.name = ?1")
  Page<Story> findByTagName(String tagName, Pageable pageable);

  // Add this method to find stories that don't have a specific tag with sorting
  List<Story> findByTagsNameNotIgnoreCase(String tagName, Sort sort);
    
  // Add this method for pageable results with tag name not matching
  Page<Story> findByTagsNameNotIgnoreCase(String tagName, Pageable pageable);
    
  // Add this method for searching with pagination
  Page<Story> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);
  
  // New genre-related methods
  
  /**
   * Find stories by genre with published state filter
   */
  Page<Story> findByGenreAndPublishedTrue(String genre, Pageable pageable);
  
  /**
   * Find stories by genre
   */
  List<Story> findByGenre(String genre);
  
  /**
   * Find stories by genre with pagination
   */
  Page<Story> findByGenre(String genre, Pageable pageable);
  
  /**
   * Find all distinct genres in the database
   */
  @Query("SELECT DISTINCT s.genre FROM Story s WHERE s.genre IS NOT NULL AND s.published = true")
  List<String> findDistinctGenres();
  
  /**
   * Advanced search with genre filter
   */
  @Query("SELECT s FROM Story s WHERE s.published = true AND " +
         "(:genre IS NULL OR s.genre = :genre) AND " +
         "(:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
         "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<Story> searchByGenreAndKeyword(
      @Param("genre") String genre, 
      @Param("keyword") String keyword, 
      Pageable pageable
  );
  
  /**
   * Find stories that have both a specific genre and tag
   */
  @Query("SELECT s FROM Story s JOIN s.tags t WHERE s.published = true AND " +
         "s.genre = :genre AND t.name = :tagName")
  Page<Story> findByGenreAndTagName(
      @Param("genre") String genre, 
      @Param("tagName") String tagName, 
      Pageable pageable
  );
  
  /**
   * Find top stories by genre ordered by read count
   */
  List<Story> findTop6ByGenreAndPublishedTrueOrderByReadCountDesc(String genre);
  
  /**
   * Find recently updated stories by genre
   */
  List<Story> findTop6ByGenreAndPublishedTrueOrderByLastUpdatedDesc(String genre);

  // Add these methods to your StoryRepository interface
Page<Story> findByPublishedTrueOrderByAverageRatingDesc(Pageable pageable);


Page<Story> findByPublishedTrueOrderByAverageRatingAsc(Pageable pageable);

// Add these methods to your StoryRepository interface
@Query("SELECT s FROM Story s WHERE s.published = true ORDER BY CASE WHEN s.averageRating IS NULL THEN 0 ELSE s.averageRating END DESC")
Page<Story> findAllByPublishedTrueOrderByAverageRatingDescNullsLast(Pageable pageable);

@Query("SELECT s FROM Story s WHERE s.published = true ORDER BY CASE WHEN s.averageRating IS NULL THEN 999 ELSE s.averageRating END ASC")
Page<Story> findAllByPublishedTrueOrderByAverageRatingAscNullsLast(Pageable pageable);


// Add to StoryRepository interface
@Query("SELECT COUNT(r) FROM ReadRecord r WHERE r.story.id = :storyId AND r.timestamp > :sinceDate")
int countRecentReads(@Param("storyId") Long storyId, @Param("sinceDate") LocalDateTime sinceDate);

Page<Story> findByPublishedTrueOrderByTrendingScoreDesc(Pageable pageable);



  
// Add this to StoryRepository.java
@Query("SELECT COUNT(s) FROM Story s WHERE s = :story AND s.createdAt > :sinceDate")
int countRecentReadsForStory(@Param("story") Story story, @Param("sinceDate") LocalDateTime sinceDate);

// Add this query method to find stories by tag name but exclude fan fiction
@Query("SELECT DISTINCT s FROM Story s " +
       "JOIN s.tags t1 " +
       "WHERE t1.name = :tagName " +
       "AND NOT EXISTS (SELECT 1 FROM s.tags t2 WHERE t2.name = :excludeTag)")
Page<Story> findByTagsNameAndTagsNameNot(
    @Param("tagName") String tagName, 
    @Param("excludeTag") String excludeTag, 
    Pageable pageable
);

@Query("SELECT DISTINCT s FROM Story s " +
       "JOIN s.tags t1 " +
       "WHERE t1.name = :includedTag " +
       "AND NOT EXISTS (SELECT 1 FROM s.tags t2 WHERE t2.name = :excludedTag)")
Page<Story> findByTagNameAndExcludeTag(
    @Param("includedTag") String includedTag,
    @Param("excludedTag") String excludedTag,
    Pageable pageable
);



Page<Story> findByGenreNot(String genre, Pageable pageable);

Page<Story> findByGenreAndGenreNot(String genre, String excludeGenre, Pageable pageable);

Page<Story> findByTitleContainingIgnoreCaseAndGenreNotOrDescriptionContainingIgnoreCaseAndGenreNot(
    String title, String excludeGenre, 
    String description, String excludeGenre2, 
    Pageable pageable);


    Page<Story> findByTitleContainingIgnoreCaseAndGenreOrDescriptionContainingIgnoreCaseAndGenre(
    String title, String genre,
    String description, String sameGenre,
    Pageable pageable);
}



