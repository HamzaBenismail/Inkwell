package app.inkwell.controller;

import app.inkwell.model.Chapter;
import app.inkwell.model.Comment;
import app.inkwell.model.Story;
import app.inkwell.model.User;
import app.inkwell.payload.ApiResponse;
import app.inkwell.payload.CommentRequest;
import app.inkwell.repository.ChapterRepository;
import app.inkwell.repository.CommentRepository;
import app.inkwell.repository.StoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    // Get comments for a story or chapter
    @GetMapping
    public ResponseEntity<?> getComments(
            @RequestParam Long storyId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String) 
                ? (User) authentication.getPrincipal() 
                : null;
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Comment> commentsPage;
            
            if (chapterId != null) {
                // Get comments for a specific chapter
                commentsPage = commentRepository.findByStoryIdAndChapterId(storyId, chapterId, pageable);
            } else {
                // Get comments for the story (not specific to any chapter)
                commentsPage = commentRepository.findByStoryIdAndChapterIdIsNull(storyId, pageable);
            }
            
            List<Map<String, Object>> commentDtos = commentsPage.getContent().stream()
                    .map(comment -> {
                        Map<String, Object> commentDto = new HashMap<>();
                        commentDto.put("id", comment.getId());
                        commentDto.put("content", comment.getContent());
                        commentDto.put("createdAt", comment.getCreatedAt());
                        
                        Map<String, Object> userDto = new HashMap<>();
                        userDto.put("id", comment.getUser().getId());
                        userDto.put("username", comment.getUser().getUsername());
                        userDto.put("profileImage", comment.getUser().getProfileImage());
                        commentDto.put("user", userDto);
                        
                        // Check if current user can delete this comment
                        boolean canDelete = currentUser != null && 
                                (comment.getUser().getId().equals(currentUser.getId()) || 
                                 currentUser.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
                        commentDto.put("canDelete", canDelete);
                        
                        return commentDto;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comments", commentDtos);
            response.put("hasMore", !commentsPage.isLast());
            response.put("totalPages", commentsPage.getTotalPages());
            response.put("totalComments", commentsPage.getTotalElements());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error fetching comments: " + e.getMessage()));
        }
    }
    
    // Add a new comment
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addComment(@RequestBody CommentRequest commentRequest) {
        try {
            // Get the authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "User not authenticated"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            Story story = storyRepository.findById(commentRequest.getStoryId())
                    .orElseThrow(() -> new RuntimeException("Story not found"));
            
            Comment comment = new Comment();
            comment.setContent(commentRequest.getContent());
            comment.setUser(currentUser);
            comment.setStory(story);
            comment.setCreatedAt(LocalDateTime.now());
            
            // If chapter ID is provided, link the comment to the chapter
            if (commentRequest.getChapterId() != null) {
                Chapter chapter = chapterRepository.findById(commentRequest.getChapterId())
                        .orElseThrow(() -> new RuntimeException("Chapter not found"));
                comment.setChapter(chapter);
            }
            
            commentRepository.save(comment);
            
            // Return the newly created comment with user info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comment added successfully");
            
            Map<String, Object> commentDto = new HashMap<>();
            commentDto.put("id", comment.getId());
            commentDto.put("content", comment.getContent());
            commentDto.put("createdAt", comment.getCreatedAt());
            
            Map<String, Object> userDto = new HashMap<>();
            userDto.put("id", currentUser.getId());
            userDto.put("username", currentUser.getUsername());
            userDto.put("profileImage", currentUser.getProfileImage());
            commentDto.put("user", userDto);
            commentDto.put("canDelete", true);
            
            response.put("comment", commentDto);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error adding comment: " + e.getMessage()));
        }
    }
    
    // Delete a comment
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        try {
            // Get the authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!authentication.isAuthenticated() || authentication.getPrincipal() instanceof String) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "User not authenticated"));
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found"));
            
            // Check if the current user is the comment owner or an admin
            if (!comment.getUser().getId().equals(currentUser.getId()) && 
                !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "You don't have permission to delete this comment"));
            }
            
            commentRepository.delete(comment);
            
            return ResponseEntity.ok(new ApiResponse(true, "Comment deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error deleting comment: " + e.getMessage()));
        }
    }
}

