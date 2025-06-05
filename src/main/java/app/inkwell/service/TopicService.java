package app.inkwell.service;

import app.inkwell.model.*;
import app.inkwell.repository.*;
import app.inkwell.utilities.SlugUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TopicService {

    @Autowired
    private TopicRepository topicRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private SubforumRepository subforumRepository;
    
    @Autowired
    private ForumService forumService;

    @Autowired
    private TopicViewRepository topicViewRepository;
    
    public Page<Topic> getTopicsBySubforum(Subforum subforum, Pageable pageable) {
        return topicRepository.findBySubforum(subforum, pageable);
    }
    
    public Page<Topic> getPinnedTopicsBySubforum(Subforum subforum, Pageable pageable) {
        return topicRepository.findBySubforumAndIsPinnedTrue(subforum, pageable);
    }
    
    public Page<Topic> getNonPinnedTopicsBySubforum(Subforum subforum, Pageable pageable) {
        return topicRepository.findBySubforumAndIsPinnedFalse(subforum, pageable);
    }
    
    public List<Topic> getRecentTopicsBySubforum(Subforum subforum) {
        return topicRepository.findTop5BySubforumOrderByLastActiveAtDesc(subforum);
    }
    
    public Optional<Topic> getTopicById(Long id) {
        return topicRepository.findById(id);
    }
    
    public Optional<Topic> getTopicBySlug(String slug) {
        return topicRepository.findBySlug(slug);
    }
    
    @Transactional
    public Topic createTopic(Subforum subforum, User author, String title, String content, Set<String> tags) {
        Topic topic = new Topic();
        topic.setSubforum(subforum);
        topic.setAuthor(author);
        topic.setTitle(title);
        topic.setContent(content);
        topic.setSlug(SlugUtils.createSlug(title));
        topic.setTags(tags);
        
        // Increment subforum topic count
        subforum.setTopicCount(subforum.getTopicCount() + 1);
        subforumRepository.save(subforum);
        
        return topicRepository.save(topic);
    }
    
    @Transactional
    public void togglePinTopic(Topic topic) {
        topic.setPinned(!topic.isPinned());
        topicRepository.save(topic);
    }
    
    @Transactional
    public void toggleLockTopic(Topic topic) {
        topic.setLocked(!topic.isLocked());
        topicRepository.save(topic);
    }
    
    @Transactional
    public Post createPost(Topic topic, User author, String content, Post parent) {
        Post post = new Post();
        post.setTopic(topic);
        post.setAuthor(author);
        post.setContent(content);
        post.setParent(parent);
        
        // Update counters
        topic.incrementReplyCount();
        topicRepository.save(topic);
        
        topic.getSubforum().setPostCount(topic.getSubforum().getPostCount() + 1);
        subforumRepository.save(topic.getSubforum());
        
        return postRepository.save(post);
    }
    
    public Page<Post> getPostsByTopic(Topic topic, Pageable pageable) {
        return postRepository.findByTopic(topic, pageable);
    }
    
    public List<Post> getRepliesByPost(Post post) {
        return postRepository.findByParent(post);
    }

    public List<Post> getTopicPosts(Topic topic) {
        // Get all posts for this topic, ordered by creation time (first post is original)
        return postRepository.findByTopicOrderByCreatedAtAsc(topic);
    }
    
    
    @Transactional
    public void incrementTopicViewCount(Topic topic) {
        topic.setViewCount(topic.getViewCount() + 1);
        topicRepository.save(topic);
    }
    
    public boolean canUserManageTopic(User user, Topic topic) {
        if (user == null) return false;
        
        // The topic author can manage their own topic
        if (user.getId().equals(topic.getAuthor().getId())) {
            return true;
        }
        
        // The forum owner can manage any topic in the forum
        return user.getId().equals(topic.getSubforum().getForum().getWriter().getId());
    }

    @Transactional
    public Topic createTopic(Subforum subforum, User author, String title, String slug, 
                            String content, Set<String> tags, boolean pinned, boolean locked) {
        // Create the topic
        Topic topic = new Topic();
        topic.setSubforum(subforum);
        topic.setAuthor(author);
        topic.setTitle(title);
        topic.setSlug(slug);
        topic.setTags(tags);
        topic.setPinned(pinned);
        topic.setLocked(locked);
        topic.setCreatedAt(LocalDateTime.now());
        topic.setLastActiveAt(LocalDateTime.now());
        topic.setViewCount(0);
        topic.setReplyCount(0);
        
        // Save the topic to get its ID
        Topic savedTopic = topicRepository.save(topic);
        
        // Create the first post
        Post firstPost = new Post();
        firstPost.setTopic(savedTopic);
        firstPost.setAuthor(author);
        firstPost.setContent(content);
        firstPost.setCreatedAt(LocalDateTime.now());
        firstPost.setUpdatedAt(LocalDateTime.now());
        // Don't set isOriginalPost as the field doesn't exist
        
        // Save the post
        postRepository.save(firstPost);
        
        return savedTopic;
    }

    public Optional<Topic> getTopicBySubforumAndSlug(Subforum subforum, String slug) {
    // Instead of directly returning the repository result, get all matching topics
        List<Topic> topics = topicRepository.findAllBySubforumAndSlug(subforum, slug);
        
        if (topics.isEmpty()) {
            return Optional.empty();
        } else if (topics.size() == 1) {
            return Optional.of(topics.get(0));
        } else {
            // If multiple topics found, return the most recently created one
            return Optional.of(topics.stream()
                    .sorted(Comparator.comparing(Topic::getCreatedAt).reversed())
                    .findFirst()
                    .orElse(topics.get(0)));
        }
    }
    

    /**
     * Increment the view count of a topic
     */
    @Transactional
    public void incrementViewCount(Topic topic) {
        topic.setViewCount(topic.getViewCount() + 1);
        topicRepository.save(topic);
    }

    @Transactional
    public boolean recordView(Topic topic, User user) {
        if(user == null){
            topic.setViewCount(topic.getViewCount() + 1);
            topicRepository.save(topic);
            return true;
        }

        if (!topicViewRepository.existsByTopicAndUser(topic, user)) {
            TopicView topicView = new TopicView(topic, user);
            topicViewRepository.save(topicView);

            topic.setViewCount(topic.getViewCount() + 1);
            topicRepository.save(topic);
            return true;
        } 
          
        // User has already viewed this topic
        return false;
        
    }

    public long getUniqueViewCount(Topic topic) {
        return topicViewRepository.countByTopic(topic);
    }
}