package app.inkwell.repository;

import app.inkwell.model.Topic;
import app.inkwell.model.TopicView;
import app.inkwell.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicViewRepository extends JpaRepository<TopicView, Long> {
    boolean existsByTopicAndUser(Topic topic, User user);
    long countByTopic(Topic topic);
}