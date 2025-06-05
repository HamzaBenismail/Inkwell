package app.inkwell.controller;

import app.inkwell.model.User;
import app.inkwell.repository.UserRepository;
import app.inkwell.payload.ApiResponse;
import app.inkwell.service.UserService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/auth")
public class WriterController {
    private static final Logger logger = LoggerFactory.getLogger(WriterController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.url}")
    private String appUrl;

    @PostMapping("/send-verification-email")
    @ResponseBody
    public ResponseEntity<?> sendVerificationEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Attempting to send verification email. Authentication: {}", auth);
        
        try {
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                logger.info("User found: {}, Email: {}", user.getUsername(), user.getEmail());
                
                try {
                    userService.sendVerificationEmail(user);
                    return ResponseEntity.ok(new ApiResponse(true, "Verification email sent successfully"));
                } catch (Exception e) {
                    logger.error("Error sending verification email", e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse(false, "Failed to send verification email: " + e.getMessage()));
                }
            } else {
                logger.warn("User not properly authenticated: {}", auth);
                if (auth != null && auth.getPrincipal() != null) {
                    logger.warn("Principal class: {}", auth.getPrincipal().getClass().getName());
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "User not authenticated or invalid user type"));
            }
        } catch (Exception e) {
            logger.error("Unexpected error in send-verification-email endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Server error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/verify-email")
public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
    try {
        // Call the service method and get the result
        boolean verified = userService.verifyEmail(token);
        
        // Add the result as a redirect attribute
        redirectAttributes.addAttribute("emailVerified", verified ? "true" : "false");
        
        // Redirect to the Settings page
        return "redirect:/Settings";
    } catch (Exception e) {
        logger.error("Error verifying email with token {}: {}", token, e.getMessage(), e);
        redirectAttributes.addAttribute("emailVerified", "false");
        redirectAttributes.addAttribute("error", "verification-failed");
        return "redirect:/Settings";
    }
}
    
@PostMapping("/become-writer")
@ResponseBody
public ResponseEntity<?> becomeWriter(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    logger.info("Attempting to promote user to writer status");
    
    try {
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            User user = (User) auth.getPrincipal();
            logger.info("Processing writer upgrade for user: {}", user.getUsername());
            
            // Check if email is verified
            if (!user.isEmailVerified()) {
                logger.warn("User {} tried to become a writer without email verification", user.getUsername());
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Please verify your email address first"));
            }
            
            // Promote to writer
            User updatedUser = userService.promoteToWriter(user);
            logger.info("User {} has been successfully promoted to writer status", user.getUsername());
            
            // Create a new authentication token with updated authorities
            UsernamePasswordAuthenticationToken newAuth = 
                new UsernamePasswordAuthenticationToken(updatedUser, null, updatedUser.getAuthorities());
            
            // Set details from the current request
            newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Update the security context with the new authentication
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            logger.info("Updated security context with new writer authorities for user: {}", user.getUsername());
            
            return ResponseEntity.ok(new ApiResponse(true, "You are now a writer"));
        } else {
            logger.warn("User not properly authenticated for writer upgrade");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "User not authenticated"));
        }
    } catch (Exception e) {
        logger.error("Error promoting user to writer", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse(false, "Server error: " + e.getMessage()));
    }
}

    private User save(User user) {
        return userRepository.save(user);
    }
}
