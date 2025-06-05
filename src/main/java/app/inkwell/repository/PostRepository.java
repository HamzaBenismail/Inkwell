package app.inkwell.repository;

import app.inkwell.model.Post;
import app.inkwell.model.Topic;
import app.inkwell.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByTopic(Topic topic, Pageable pageable);
    List<Post> findByAuthor(User author);
    List<Post> findByParent(Post parent);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.topic = ?1")
    void deleteAllByTopic(Topic topic);

    //findByTopicOrderByCreatedAtAsc(Topic topic, PageReqeuest pageRequest);
    Page<Post> findByTopicOrderByCreatedAtAsc(Topic topic, Pageable pageable);
    
    // Get all replies to a specific post
    List<Post> findByParentOrderByCreatedAtAsc(Post parent);
    
    // Get original post + top level replies with pagination

// Add or modify this method
List<Post> findByTopicOrderByCreatedAtAsc(Topic topic);

long countByTopic(Topic topic);
}