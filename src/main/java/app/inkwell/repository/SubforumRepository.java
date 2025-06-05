package app.inkwell.repository;

import app.inkwell.model.Story;

import app.inkwell.model.Forum;
import app.inkwell.model.Subforum;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubforumRepository extends JpaRepository<Subforum, Long> {
    List<Subforum> findByForum(Forum forum);
    List<Subforum> findByStory(Story story);
    Optional<Subforum> findBySlug(String slug);
    Optional<Subforum> findByForumAndSlug(Forum forum, String slug);
    List<Subforum> findByForumOrderByNameAsc(Forum forum);
    Optional<Subforum> findByForumAndStoryId(Forum forum, Long storyId);
}