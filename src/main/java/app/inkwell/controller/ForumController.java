package app.inkwell.controller;

import app.inkwell.model.*;
import app.inkwell.repository.PostRepository;
import app.inkwell.service.*;
import app.inkwell.repository.TopicRepository;
import app.inkwell.repository.SubforumRepository;
import app.inkwell.utilities.SlugUtils;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/forums")
public class ForumController {

    

    @Autowired
    private ForumService forumService;
    
    @Autowired
    private TopicService topicService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private StoryService storyService;
    
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private SubforumRepository subforumRepository;

    // PLEASE FOR THE LOVE OF GOD STOP TOUCHING THIS FUCKING FILE
    // IT'S A MESS AND I DON'T KNOW HOW TO FIX IT
    
    @GetMapping
    public String getAllForums(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("activeTab", "Forums");

        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get all active subscriptions for the current user
        List<Subscription> subscriptions = subscriptionService.getActiveSubscriptionsBySubscriber(currentUser); 
        
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("currentUser", currentUser);
        
        // If the user is a writer, check if they have a forum yet
        if (currentUser.isWriter()) {
            boolean hasOwnForum = forumService.getForumByWriter(currentUser).isPresent();
            model.addAttribute("hasOwnForum", hasOwnForum);
            model.addAttribute("isWriter", true);
        }
        
        return "Forums";
    }
    
    /**
     * Writer's forum page - shows all subforums for a specific writer
     */
    @GetMapping("/{username}")
    public String getWriterForum(@PathVariable String username, Model model) {
        // Find the writer by username
        Optional<User> writerOpt = userService.getUserByUsername(username);
        if (writerOpt.isEmpty()) {
            // Writer not found
            return "redirect:/error/404";
        }
        
        User writer = writerOpt.get();
        
        // Check if the writer is actually a writer
        if (!writer.isWriter()) {
            // Not a writer, can't have a forum
            return "redirect:/error/404";
        }
        
        // Get the currently authenticated user
        User currentUser = null;
        boolean isAuthenticated = false;
        boolean isForumOwner = false;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
            if (currentUserOpt.isPresent()) {
                currentUser = currentUserOpt.get();
                isAuthenticated = true;
                isForumOwner = currentUser.getId().equals(writer.getId());
            }
        }
        
        // Check if the user is subscribed to this writer (if not the owner)
        boolean isSubscribed = false;
        if (isAuthenticated && currentUser != null && !isForumOwner) {
            isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
        }
        
        // Check if forum exists and create if it doesn't
        Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
        Forum forum = null;
        boolean autoCreatedForum = false;
        boolean subforumsCreated = false;
        boolean initialSetupComplete = false;
        
        if (forumOpt.isEmpty()) {
            // Create forum for writer
            forum = forumService.createForum(writer, writer.getUsername() + "'s Forum", 
                "Discussion space for " + writer.getUsername() + "'s works");
            autoCreatedForum = true;
            System.out.println("Created new forum for writer: " + writer.getUsername());
        } else {
            forum = forumOpt.get();
        }
        
        // Get existing subforums for this forum
        List<Subforum> subforums = forumService.getSubforumsByForum(forum);
        
        // Get all stories by this writer
        List<Story> stories = storyService.getStoriesByAuthor(writer);
        System.out.println("Found " + stories.size() + " stories by " + writer.getUsername());
        for (Story story : stories) {
            System.out.println("Story: " + story.getTitle() + " (ID: " + story.getId() + ")");
        }
        
        // Create subforums for stories if needed
        if (autoCreatedForum || (subforums.isEmpty() && !stories.isEmpty())) {
            System.out.println("Creating subforums for stories...");
            
            // Create a general discussion subforum
            if (subforums.stream().noneMatch(s -> s.getName().equals("General Discussion"))) {
                forumService.createSubforum(forum, "General Discussion", 
                    "General discussion about " + writer.getUsername() + "'s works", 
                    "fas fa-comments", "bg-blue-800 bg-opacity-30", null);
            }
            
            // Create an announcements subforum
            if (subforums.stream().noneMatch(s -> s.getName().equals("Announcements & FAQs"))) {
                forumService.createSubforum(forum, "Announcements & FAQs", 
                    "Important updates and frequently asked questions", 
                    "fas fa-bullhorn", "bg-amber-900 bg-opacity-30", null);
            }
            
            // Create a subforum for each story
            for (Story story : stories) {
                System.out.println("Creating subforum for story: " + story.getTitle());
                if (subforums.stream().noneMatch(s -> s.getStory() != null && 
                                              s.getStory().getId().equals(story.getId()))) {
                    String iconClass = getIconClassForStory(story);
                    String iconBgClass = getBackgroundClassForStory(story);
                    forumService.createSubforum(forum, story.getTitle(), 
                        "Discuss " + story.getTitle(), iconClass, iconBgClass, story);
                }
            }
            
            // Refresh subforums after creating new ones
            subforums = forumService.getSubforumsByForum(forum);
            subforumsCreated = true;
            initialSetupComplete = true;
        }
        
        // Check if we need to create subforums for any new stories
        if (!autoCreatedForum && !subforums.isEmpty() && !stories.isEmpty()) {
            boolean createdNewSubforums = false;
            
            for (Story story : stories) {
                boolean hasSubforum = false;
                for (Subforum subforum : subforums) {
                    if (subforum.getStory() != null && 
                        subforum.getStory().getId().equals(story.getId())) {
                        hasSubforum = true;
                        break;
                    }
                }
                
                if (!hasSubforum) {
                    System.out.println("Creating missing subforum for story: " + story.getTitle());
                    String iconClass = getIconClassForStory(story);
                    String iconBgClass = getBackgroundClassForStory(story);
                    forumService.createSubforum(forum, story.getTitle(), 
                        "Discuss " + story.getTitle(), iconClass, iconBgClass, story);
                    createdNewSubforums = true;
                }
            }
            
            if (createdNewSubforums) {
                // Refresh subforums after creating new ones
                subforums = forumService.getSubforumsByForum(forum);
                subforumsCreated = true;
            }
        }
        
        // Add attributes to model
        model.addAttribute("writer", writer);
        model.addAttribute("forum", forum);
        model.addAttribute("subforums", subforums);
        model.addAttribute("writerStories", stories);
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isForumOwner", isForumOwner);
        model.addAttribute("isSubscribed", isSubscribed);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("autoCreatedForum", autoCreatedForum);
        model.addAttribute("subforumsCreated", subforumsCreated);
        model.addAttribute("initialSetupComplete", initialSetupComplete);
        model.addAttribute("activeTab", "Forums");
        
        System.out.println("Writer: " + writer.getUsername());
        System.out.println("Forum: " + (forum != null ? forum.getName() : "null"));
        System.out.println("Subforums count: " + subforums.size());
        
        return "WritersForum";
    }

    // Helper methods for story icons remain the same
    private String getIconClassForStory(Story story) {
        // Existing implementation
        if (story == null) {
            return "fas fa-comments";
        }
        
        String genre = story.getGenre();
        if (genre == null) {
            return "fas fa-book";
        }
        
        // Return appropriate icon based on genre
        return switch (genre.toLowerCase()) {
            case "fantasy" -> "fas fa-dragon";
            case "sci-fi", "science fiction" -> "fas fa-rocket";
            case "mystery" -> "fas fa-magnifying-glass";
            case "thriller" -> "fas fa-mask";
            case "romance" -> "fas fa-heart";
            case "horror" -> "fas fa-ghost";
            case "adventure" -> "fas fa-mountain";
            case "historical" -> "fas fa-landmark";
            case "poetry" -> "fas fa-feather";
            default -> "fas fa-book";
        };
    }

    private String getBackgroundClassForStory(Story story) {
        // Existing implementation 
        if (story == null) {
            return "bg-blue-800 bg-opacity-30";
        }
        
        String genre = story.getGenre();
        if (genre == null) {
            return "bg-blue-800 bg-opacity-30";
        }
        
        // Return appropriate background based on genre
        return switch (genre.toLowerCase()) {
            case "fantasy" -> "bg-purple-800 bg-opacity-30";
            case "sci-fi", "science fiction" -> "bg-cyan-800 bg-opacity-30"; 
            case "mystery" -> "bg-indigo-800 bg-opacity-30";
            case "thriller" -> "bg-red-900 bg-opacity-30";
            case "romance" -> "bg-pink-800 bg-opacity-30";
            case "horror" -> "bg-gray-900 bg-opacity-30";
            case "adventure" -> "bg-amber-700 bg-opacity-30";
            case "historical" -> "bg-yellow-800 bg-opacity-30";
            case "poetry" -> "bg-teal-800 bg-opacity-30";
            default -> "bg-blue-800 bg-opacity-30";
        };
    }


    @GetMapping("/{username}/{subforumSlug}")
    public String getSubforum(@PathVariable String username, 
                            @PathVariable String subforumSlug,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "latest") String sort,
                            Model model) {
        // Find the writer by username
        Optional<User> writerOpt = userService.getUserByUsername(username);
        if (writerOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        User writer = writerOpt.get();
        
        // Get the writer's forum
        Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
        if (forumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Forum forum = forumOpt.get();
        
        // Find the subforum by slug
        Optional<Subforum> subforumOpt = forumService.getSubforumByForumAndSlug(forum, subforumSlug);
        if (subforumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Subforum subforum = subforumOpt.get();
        
        // Get the currently authenticated user
        User currentUser = null;
        boolean isAuthenticated = false;
        boolean isForumOwner = false;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
            if (currentUserOpt.isPresent()) {
                currentUser = currentUserOpt.get();
                isAuthenticated = true;
                isForumOwner = currentUser.getId().equals(writer.getId());
            }
        }
        
        // Check if the user is subscribed to this writer (if not the owner)
        boolean isSubscribed = false;
        if (isAuthenticated && currentUser != null && !isForumOwner) {
            isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
        }
        
        // Define page size
        int pageSize = 10;
        
        // Create pageable with sort order
        Sort topicSort;
        switch (sort) {
            case "newest":
                topicSort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "most-viewed":
                topicSort = Sort.by(Sort.Direction.DESC, "viewCount");
                break;
            case "most-replies":
                topicSort = Sort.by(Sort.Direction.DESC, "replyCount");
                break;
            case "latest":
            default:
                topicSort = Sort.by(Sort.Direction.DESC, "lastActiveAt");
                break;
        }
        
        PageRequest pageable = PageRequest.of(page, pageSize, topicSort);
        
        // Get pinned and regular topics separately
        Page<Topic> pinnedTopicsPage = topicService.getPinnedTopicsBySubforum(subforum, PageRequest.of(0, 5, topicSort));
        Page<Topic> topicsPage = topicService.getNonPinnedTopicsBySubforum(subforum, pageable);
        
        // Add attributes to model
        model.addAttribute("writer", writer);
        model.addAttribute("forum", forum);
        model.addAttribute("subforum", subforum);
        model.addAttribute("topics", topicsPage.getContent());
        model.addAttribute("pinnedTopics", pinnedTopicsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", topicsPage.getTotalPages());
        model.addAttribute("sortBy", sort);
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isForumOwner", isForumOwner);
        model.addAttribute("isSubscribed", isSubscribed);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeTab", "Forums");
        
        return "SubforumView";
    }

    @GetMapping("/{username}/{subforumSlug}/new-topic")
    public String showNewTopicForm(@PathVariable String username, 
                                @PathVariable String subforumSlug,
                                Model model) {
        // Find the writer
        Optional<User> writerOpt = userService.getUserByUsername(username);
        if (writerOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        User writer = writerOpt.get();
        
        // Get the forum
        Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
        if (forumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Forum forum = forumOpt.get();
        
        // Find the subforum
        Optional<Subforum> subforumOpt = forumService.getSubforumByForumAndSlug(forum, subforumSlug);
        if (subforumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Subforum subforum = subforumOpt.get();
        
        // Get current user
        User currentUser = null;
        boolean isAuthenticated = false;
        boolean isForumOwner = false;
        boolean isSubscribed = false;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        
        Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        currentUser = currentUserOpt.get();
        isAuthenticated = true;
        isForumOwner = currentUser.getId().equals(writer.getId());
        
        // Check if user is subscribed or owner
        if (!isForumOwner) {
            isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
            
            // If not subscribed or the forum owner, redirect
            if (!isSubscribed) {
                return "redirect:/forums/" + username;
            }
        }
        
        // Add to model
        model.addAttribute("writer", writer);
        model.addAttribute("forum", forum);
        model.addAttribute("subforum", subforum);
        model.addAttribute("topicForm", new TopicForm());
        model.addAttribute("isForumOwner", isForumOwner);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeTab", "Forums");
        
        return "NewTopic";
    }

    @PostMapping("/{username}/{subforumSlug}/new-topic")
    public String createNewTopic(@PathVariable String username,
                                @PathVariable String subforumSlug,
                                @Valid @ModelAttribute("topicForm") TopicForm topicForm,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        // Find the writer
        Optional<User> writerOpt = userService.getUserByUsername(username);
        if (writerOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        User writer = writerOpt.get();
        
        // Get the forum
        Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
        if (forumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Forum forum = forumOpt.get();
        
        // Find the subforum
        Optional<Subforum> subforumOpt = forumService.getSubforumByForumAndSlug(forum, subforumSlug);
        if (subforumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Subforum subforum = subforumOpt.get();
        
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        
        Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        User currentUser = currentUserOpt.get();
        boolean isForumOwner = currentUser.getId().equals(writer.getId());
        
        // Check if user is subscribed or owner
        if (!isForumOwner) {
            boolean isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
            
            // If not subscribed or the forum owner, redirect
            if (!isSubscribed) {
                return "redirect:/forums/" + username;
            }
        }
        
        // Handle validation errors
        if (bindingResult.hasErrors()) {
            // Re-populate model attributes
            model.addAttribute("writer", writer);
            model.addAttribute("forum", forum);
            model.addAttribute("subforum", subforum);
            model.addAttribute("isForumOwner", isForumOwner);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeTab", "Forums");
            
            return "NewTopic";
        }
        
        // Parse tags from JSON string
        Set<String> tags = new HashSet<>();
        if (topicForm.getTags() != null && !topicForm.getTags().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String[] tagsArray = mapper.readValue(topicForm.getTags(), String[].class);
                tags =  new HashSet<>(Arrays.asList(tagsArray));
            } catch (JsonProcessingException e) {
                // Log the error
                System.err.println("Error parsing tags: " + e.getMessage());
            }
        }
        
        // Create the topic slug
        String slug = SlugUtils.createSlug(topicForm.getTitle());
        
        // Only forum owner can pin/lock topics
        boolean pinned = isForumOwner && topicForm.isPinned();
        boolean locked = isForumOwner && topicForm.isLocked();
        
        // Create the topic
        Topic topic = topicService.createTopic(
            subforum,
            currentUser,
            topicForm.getTitle(),
            slug,
            topicForm.getContent(),
            tags,
            pinned,
            locked
        );
        
        // Add success message
        redirectAttributes.addFlashAttribute("successMessage", "Topic created successfully!");
        
        // Redirect to the new topic
        return "redirect:/forums/" + username + "/" + subforumSlug + "/" + topic.getSlug();
    }

    @GetMapping("/{username}/{subforumSlug}/{topicSlug}")
    public String viewTopic(@PathVariable String username, 
                        @PathVariable String subforumSlug,
                        @PathVariable String topicSlug,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
    // Find the writer
    Optional<User> writerOpt = userService.getUserByUsername(username);
    if (writerOpt.isEmpty()) {
        return "redirect:/error/404";
    }
    
    User writer = writerOpt.get();
    
    // Get the forum
    Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
    if (forumOpt.isEmpty()) {
        return "redirect:/error/404";
    }
    
    Forum forum = forumOpt.get();
    
    // Find the subforum
    Optional<Subforum> subforumOpt = forumService.getSubforumByForumAndSlug(forum, subforumSlug);
    if (subforumOpt.isEmpty()) {
        return "redirect:/error/404";
    }
    
    Subforum subforum = subforumOpt.get();
    
    // Find the topic
    Optional<Topic> topicOpt = topicService.getTopicBySubforumAndSlug(subforum, topicSlug);
    if (topicOpt.isEmpty()) {
        return "redirect:/error/404";
    }
    
    Topic topic = topicOpt.get();
    
    // Get current user
    User currentUser = null;
    boolean isAuthenticated = false;
    boolean isForumOwner = false;
    boolean isTopicAuthor = false;
    
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
        Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
        if (currentUserOpt.isPresent()) {
            currentUser = currentUserOpt.get();
            isAuthenticated = true;
            isForumOwner = currentUser.getId().equals(writer.getId());
            isTopicAuthor = currentUser.getId().equals(topic.getAuthor().getId());
        }
    }
    
    topicService.recordView(topic, currentUser);
    long UniqueView = topicService.getUniqueViewCount(topic);

    // Check if the user is subscribed to this writer (if not the owner)
    boolean isSubscribed = false;
    if (isAuthenticated && currentUser != null && !isForumOwner) {
        isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
    }
    
    
    // Get all posts for this topic in chronological order
    List<Post> allPosts = topicService.getTopicPosts(topic);
    
    // The first post is the original post
    Post originalPost = allPosts.isEmpty() ? null : allPosts.get(0);
    List<Post> replies = allPosts.size() > 1 ? allPosts.subList(1, allPosts.size()) : new ArrayList<>();
    
    // Add attributes to model
    model.addAttribute("writer", writer);
    model.addAttribute("forum", forum);
    model.addAttribute("subforum", subforum);
    model.addAttribute("topic", topic);
    model.addAttribute("originalPost", originalPost);
    model.addAttribute("replies", replies);
    model.addAttribute("isAuthenticated", isAuthenticated);
    model.addAttribute("isForumOwner", isForumOwner);
    model.addAttribute("isTopicAuthor", isTopicAuthor);
    model.addAttribute("isSubscribed", isSubscribed);
    model.addAttribute("currentUser", currentUser);
    model.addAttribute("activeTab", "Forums");
    model.addAttribute("UniqueView", UniqueView);
    
    return "TopicView";
}   

    /**
 * Handle posting a reply to a topic
 */
    @PostMapping("/{username}/{subforumSlug}/{topicSlug}/reply")
    public String postReply(@PathVariable String username,
                            @PathVariable String subforumSlug,
                            @PathVariable String topicSlug,
                            @RequestParam("content") String content,
                            RedirectAttributes redirectAttributes) {
        
        // Find the writer
        Optional<User> writerOpt = userService.getUserByUsername(username);
        if (writerOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        User writer = writerOpt.get();
        
        // Get the forum
        Optional<Forum> forumOpt = forumService.getForumByWriter(writer);
        if (forumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Forum forum = forumOpt.get();
        
        // Find the subforum
        Optional<Subforum> subforumOpt = forumService.getSubforumByForumAndSlug(forum, subforumSlug);
        if (subforumOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Subforum subforum = subforumOpt.get();
        
        // Find the topic
        Optional<Topic> topicOpt = topicService.getTopicBySubforumAndSlug(subforum, topicSlug);
        if (topicOpt.isEmpty()) {
            return "redirect:/error/404";
        }
        
        Topic topic = topicOpt.get();
        
        // Check if the topic is locked
        if (topic.isLocked()) {
            redirectAttributes.addFlashAttribute("errorMessage", "This topic is locked and cannot receive new replies.");
            return "redirect:/forums/" + username + "/" + subforumSlug + "/" + topicSlug;
        }
        
        // Get current user
        User currentUser = null;
        boolean isAuthenticated = false;
        boolean isSubscribed = false;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        
        Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        currentUser = currentUserOpt.get();
        isAuthenticated = true;
        boolean isForumOwner = currentUser.getId().equals(writer.getId());
        
        // Check if user is subscribed or owner
        if (!isForumOwner) {
            isSubscribed = subscriptionService.isSubscribed(currentUser, writer);
            
            // If not subscribed or the forum owner, redirect
            if (!isSubscribed) {
                redirectAttributes.addFlashAttribute("errorMessage", "You need to be subscribed to post in this forum.");
                return "redirect:/forums/" + username + "/" + subforumSlug + "/" + topicSlug;
            }
        }
        
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reply content cannot be empty.");
            return "redirect:/forums/" + username + "/" + subforumSlug + "/" + topicSlug;
        }
        
        // Create the reply post
        Post reply = new Post();
        reply.setTopic(topic);
        reply.setAuthor(currentUser);
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        postRepository.save(reply);
        
        // Update the topic's last activity time and reply count
        topic.setLastActiveAt(LocalDateTime.now());
        topic.setReplyCount(topic.getReplyCount() + 1);
        topicRepository.save(topic);
        
        // Update the subforum's post count
        subforum.setPostCount(subforum.getPostCount() + 1);
        subforumRepository.save(subforum);
        
        // Add success message
        redirectAttributes.addFlashAttribute("successMessage", "Reply posted successfully!");
        
        // Redirect to the last page of the topic to show the new reply
        int totalReplies = topic.getReplyCount();
        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) (totalReplies + 1) / pageSize); // +1 for original post
        int lastPage = totalPages - 1;
        
        return "redirect:/forums/" + username + "/" + subforumSlug + "/" + topicSlug + "?page=" + lastPage;
    }
}