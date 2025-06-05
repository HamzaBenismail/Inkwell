package app.inkwell.service;

import app.inkwell.model.Authority;
import app.inkwell.model.User;
import app.inkwell.repository.AuthorityRepository;
import app.inkwell.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import app.inkwell.security.WriterAuthority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.url}")
  private String appUrl;

  @Autowired
  private JavaMailSender mailSender;

  @Autowired
  private WriterAuthority writerAuthority;

  @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    @Transactional
    public User registerUser(String username, String email, String password) {
        try {
            logger.info("Registering new user: {}, email: {}", username, email);
            
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }
            
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setCreatedAt(LocalDateTime.now());
            
            // Set provider to 0 (local authentication)
            user.setProvider(0);
            
            // Add default user role
            Authority userAuthority = authorityRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        logger.info("Creating ROLE_USER authority as it doesn't exist");
                        Authority newAuth = new Authority("ROLE_USER");
                        return authorityRepository.save(newAuth);
                    });
            
            user.addAuthority(userAuthority);
            
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully: {}", username);
            return savedUser;
        } catch (Exception e) {
            logger.error("Error registering user: {}", username, e);
            throw e;
        }
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        logger.debug("Looking up user by username: {}", username);
        User user = userRepository.findByUsername(username);
        return Optional.ofNullable(user);
    }

    public Optional<User> getUserByEmail(String email) {
        logger.debug("Looking up user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Saves a user to the database
     */
    public User save(User user) {
        return userRepository.save(user);
    }


    public void updatePassword(String username, String currentPassword, String newPassword) throws Exception {
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUsername(username));
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        
        User user = userOptional.get();
        
        // Verify current password matches
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new Exception("Invalid current password.");
        }
        
        // Encode the new password
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        
        // Update the password
        user.setPassword(encodedNewPassword);
        userRepository.save(user);
    }


    private boolean usingMfa;
    private String mfaSecret;

    public void setUsingMfa(boolean usingMfa) {
        this.usingMfa = usingMfa;
    }

   public void setMfaSecret(String mfaSecret) {
       this.mfaSecret = mfaSecret;
   }

     /**
   * Generates a verification token for the user
   */
  public String generateVerificationToken(User user) {
    String token = UUID.randomUUID().toString();
    user.setVerificationToken(token);
    save(user);
    return token;
}

public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        
        String username = auth.getName();
        return userRepository.findByUsername(username);
    }

/**
 * Sends a verification email to the user
 */
public void sendVerificationEmail(User user) {
    if (user == null) {
        throw new IllegalArgumentException("User cannot be null");
    }
    
    logger.info("Sending verification email to {}", user.getEmail());
    
    // Generate a new verification token
    String token = UUID.randomUUID().toString();
    user.setVerificationToken(token);
    userRepository.save(user);
    
    try {
        // Create email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("fundflow.sender@gmail.com"); // Must match your spring.mail.username
        message.setTo(user.getEmail());
        message.setSubject("Inkwell Email Verification");
        message.setText("Hello " + user.getUsername() + ",\n\n" +
                "Please verify your email address by clicking the link below:\n\n" +
                appUrl + "/settings/email/verify?token=" + token + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "Thank you,\n" +
                "The Inkwell Team");
        
        logger.info("Attempting to send email to {} with token {}", user.getEmail(), token);
        mailSender.send(message);
        logger.info("Verification email sent successfully to {}", user.getEmail());
    } catch (Exception e) {
        logger.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage(), e);
        throw e;
    }
}

/**
 * Verifies a user's email using the token
 */
public boolean verifyEmail(String token) {
    logger.debug("Verifying email with token: {}", token);
    
    if (token == null || token.isEmpty()) {
        logger.warn("Token is null or empty");
        return false;
    }
    
    User user = userRepository.findByVerificationToken(token);
    
    if (user != null) {
        logger.info("Found user: {} with token: {}", user.getUsername(), token);
        user.setEmailVerified(true);
        user.setVerificationToken(null); // Clear the token after use
        userRepository.save(user);
        return true;
    } else {
        logger.warn("No user found with token: {}", token);
        return false;
    }
}

@PersistenceContext
private EntityManager entityManager;

/**
 * Makes a user a writer
 */
public boolean makeWriter(User user) {
    if (!user.isEmailVerified()) {
        logger.warn("Cannot make user a writer without email verification: {}", user.getUsername());
        return false;
    }
    
    user.setWriter(true);
    writerAuthority.addWriterAuthority(user);
    userRepository.save(user);
    
    logger.info("User is now a writer: {}", user.getUsername());
    return true;
}

    @Transactional
    public User promoteToWriter(User user) {
    if (user == null) {
        throw new IllegalArgumentException("User cannot be null");
    }
    
    logger.info("Promoting user {} to writer status", user.getUsername());
    
    // Set the writer flag
    user.setWriter(true);
    
    // Find or create the WRITER authority
    Authority writerAuthority = authorityRepository.findByName("WRITER")
        .orElseGet(() -> {
            logger.info("Creating new WRITER authority");
            Authority auth = new Authority("WRITER");
            return authorityRepository.save(auth);
        });
    
    // Initialize authorities collection if null
    if (user.getAuthorities() == null) {
        user.setAuthorities(new HashSet<>());
    }
    
    // Check if user already has the authority
    boolean hasWriterAuthority = user.getAuthorities().stream()
        .anyMatch(auth -> ((Authority) auth).getName().equals("WRITER"));
        
    if (!hasWriterAuthority) {
        logger.info("Adding WRITER authority to user {}", user.getUsername());
        user.addAuthority(writerAuthority);
    }
    
    // Save and refresh the user entity to ensure all relationships are loaded correctly
    User savedUser = userRepository.saveAndFlush(user);
    
    // Force a refresh of the user entity to ensure we have the latest data
    entityManager.refresh(savedUser);
    
    logger.info("User {} successfully promoted to writer status with authorities: {}", 
        savedUser.getUsername(), 
        savedUser.getAuthorities().stream().map(auth -> ((Authority) auth).getName()).collect(Collectors.joining(", ")));
    
    return savedUser;
}

@Transactional
public boolean updateUserProfile(User user, String username, String bio, MultipartFile profileImage) throws IOException {
    boolean updated = false;
    
    // Update username if provided and different
    if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
        // Check if username is already taken
        if (userRepository.existsByUsername(username) && !user.getUsername().equals(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        user.setUsername(username);
        updated = true;
    }
    
    // Update bio if provided and different
    if (bio != null && !bio.equals(user.getBio())) {
        user.setBio(bio);
        updated = true;
    }
    
    // Process profile image if provided
    if (profileImage != null && !profileImage.isEmpty()) {
        String originalFilename = profileImage.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            // Generate a unique filename to prevent collisions
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Ensure upload directory exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save file to disk
            Path filePath = uploadPath.resolve(newFilename);
            Files.write(filePath, profileImage.getBytes());
            
            // Update user profile image path
            user.setProfileImage("/uploads/" + newFilename);
            updated = true;
        }
    }
    
    if (updated) {
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    return updated;
}

}

