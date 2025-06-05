package app.inkwell.repository;

import app.inkwell.model.User;

import app.inkwell.model.Forum;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ForumRepository extends JpaRepository<Forum, Long> {
    List<Forum> findByWriter(User writer);
    Optional<Forum> findBySlug(String slug);
    Optional<Forum> findByWriterAndSlug(User writer, String slug);
}