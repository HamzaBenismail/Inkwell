package app.inkwell.service;

import app.inkwell.model.*;
import app.inkwell.repository.*;
import app.inkwell.utilities.SlugUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ForumService {
    
    @Autowired
    private ForumRepository forumRepository;
    
    @Autowired
    private SubforumRepository subforumRepository;
    
    @Autowired
    private TopicRepository topicRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private StoryService storyService;
    
    /**
     * Gets all forums for writers that the user is subscribed to
     */
    public List<Forum> getForumsForSubscribedWriters(User reader) {
        Set<User> subscribedWriters = subscriptionService.getSubscribedWriters(reader);
        
        // Add the user themselves if they are a writer
        if (reader.isWriter()) {
            subscribedWriters.add(reader);
        }
        
        return subscribedWriters.stream()
                .map(this::getForumByWriter)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a forum by writer
     */
    public Optional<Forum> getForumByWriter(User writer) {
        List<Forum> forums = forumRepository.findByWriter(writer);
        return forums.isEmpty() ? Optional.empty() : Optional.of(forums.get(0));
    }
    
    /**
     * Gets a forum by its URL slug
     */
    public Optional<Forum> getForumBySlug(String slug) {
        return forumRepository.findBySlug(slug);
    }
    
    /**
     * Gets a forum by writer and slug
     */
    public Optional<Forum> getForumByWriterAndSlug(User writer, String slug) {
        return forumRepository.findByWriterAndSlug(writer, slug);
    }
    
    /**
     * Creates a new forum for a writer
     */
    @Transactional
    public Forum createForum(User writer, String name, String description) {
        // Check if the user is a writer
        if (!writer.isWriter()) {
            throw new IllegalArgumentException("Only writers can create forums");
        }
        
        Forum forum = new Forum();
        forum.setWriter(writer);
        forum.setName(name);
        forum.setDescription(description);
        forum.setSlug(SlugUtils.createSlug(name));
        return forumRepository.save(forum);
    }
    
    /**
     * Gets all subforums in a forum
     */
    public List<Subforum> getSubforumsByForum(Forum forum) {
        return subforumRepository.findByForum(forum);
    }
    
    /**
     * Gets a subforum by ID
     */
    public Optional<Subforum> getSubforumById(Long id) {
        return subforumRepository.findById(id);
    }
    
    /**
     * Gets a subforum by slug
     */
    public Optional<Subforum> getSubforumBySlug(String slug) {
        return subforumRepository.findBySlug(slug);
    }
    
    /**
     * Gets a subforum by forum and slug
     */
    public Optional<Subforum> getSubforumByForumAndSlug(Forum forum, String slug) {
        return subforumRepository.findByForumAndSlug(forum, slug);
    }
    
    /**
     * Creates a new subforum
     */
    @Transactional
    public Subforum createSubforum(Forum forum, String name, String description, String iconClass, String iconBgClass, Story story) {
        Subforum subforum = new Subforum();
        subforum.setForum(forum);
        subforum.setName(name);
        subforum.setDescription(description);
        subforum.setSlug(SlugUtils.createSlug(name));
        subforum.setIconClass(iconClass != null ? iconClass : "fas fa-comments");
        subforum.setIconBgClass(iconBgClass != null ? iconBgClass : "bg-blue-800 bg-opacity-30");
        subforum.setStory(story);
        subforum.setTopicCount(0);
        subforum.setPostCount(0);
        subforum.setCreatedAt(LocalDateTime.now());
        subforum.setUpdatedAt(LocalDateTime.now());
        return subforumRepository.save(subforum);
    }
    
    /**
     * Checks if a user has access to a forum
     */
    public boolean hasAccessToForum(User user, Forum forum) {
        // Writers have access to their own forums
        if (user.getId().equals(forum.getWriter().getId())) {
            return true;
        }
        
        // Admin users have access to all forums
        if (user.isAdmin()) {
            return true;
        }
        
        // Subscribers have access to the writer's forums
        return subscriptionService.isSubscribed(user, forum.getWriter());
    }
    
    /**
     * Get popular subforums across all forums
     */
    public List<Subforum> getPopularSubforums(int limit) {
        // This would typically use a more complex query to find active forums
        // For now, just return based on post count
        return subforumRepository.findAll()
            .stream()
            .sorted((a, b) -> b.getPostCount().compareTo(a.getPostCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recently updated subforums for a user based on subscriptions
     */
    public List<Subforum> getRecentSubforumsForUser(User user, int limit) {
        List<Forum> accessibleForums = getForumsForSubscribedWriters(user);
        
        return accessibleForums.stream()
            .flatMap(forum -> getSubforumsByForum(forum).stream())
            .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a subforum (admin or forum owner only)
     */
    @Transactional
    public boolean deleteSubforum(User user, Long subforumId) {
        Optional<Subforum> subforumOpt = subforumRepository.findById(subforumId);
        if (subforumOpt.isEmpty()) {
            return false;
        }
        
        Subforum subforum = subforumOpt.get();
        Forum forum = subforum.getForum();
        
        // Only the forum owner or admin can delete subforums
        if (!user.getId().equals(forum.getWriter().getId()) && !user.isAdmin()) {
            return false;
        }
        
        // Delete all topics and posts first
        List<Topic> topics = (List<Topic>) topicRepository.findBySubforum(subforum, PageRequest.of(0, Integer.MAX_VALUE));
        for (Topic topic : topics) {
            postRepository.deleteAllByTopic(topic);
            topicRepository.delete(topic);
        }
        
        // Then delete the subforum
        subforumRepository.delete(subforum);
        return true;
    }

    public Post getOriginalPost(Topic topic) {
        List<Post> posts = postRepository.findByTopicOrderByCreatedAtAsc(topic);
        return posts.isEmpty() ? null : posts.get(0);
    }
    
    public List<Post> getTopicReplies(Topic topic) {
        List<Post> allPosts = postRepository.findByTopicOrderByCreatedAtAsc(topic);
        return allPosts.size() > 1 ? allPosts.subList(1, allPosts.size()) : List.of();
    }
}