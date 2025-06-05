package app.inkwell.repository;

import app.inkwell.model.Subforum;
import app.inkwell.model.Topic;
import app.inkwell.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Page<Topic> findBySubforum(Subforum subforum, Pageable pageable);
    Page<Topic> findBySubforumAndIsPinnedTrue(Subforum subforum, Pageable pageable);
    Page<Topic> findBySubforumAndIsPinnedFalse(Subforum subforum, Pageable pageable);
    List<Topic> findTop5BySubforumOrderByLastActiveAtDesc(Subforum subforum);
    Optional<Topic> findBySlug(String slug);
    List<Topic> findByAuthor(User author);
    // findBySubforumAndSlug(Subforum subforum, String slug);
    Optional<Topic> findBySubforumAndSlug(Subforum subforum, String slug);
    // Add this method to your repository
    List<Topic> findAllBySubforumAndSlug(Subforum subforum, String slug);
}