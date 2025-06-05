package app.inkwell.security;

import app.inkwell.model.User;
import app.inkwell.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

// Add this import
import app.inkwell.security.WriterAuthority;

@Service
public class CustomUserDetailsService implements UserDetailsService {

   private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private WriterAuthority writerAuthority;

   @Override
   @Transactional
   public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
       logger.debug("Loading user by username or email: {}", usernameOrEmail);
       
       // Let users login with either username or email
       User user = null;
       
       // First try to find by username
       user = userRepository.findByUsername(usernameOrEmail);
       
       // If not found by username, try email
       if (user == null) {
           user = userRepository.findByEmail(usernameOrEmail)
                   .orElse(null);
       }
       
       if (user == null) {
           logger.error("User not found with username or email: {}", usernameOrEmail);
           throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
       }
       
       logger.debug("User found: {}", user.getUsername());
       // Get authorities including writer role if applicable
       // Get authorities including writer role if applicable
      Collection<? extends GrantedAuthority> authorities = writerAuthority.getAuthorities(user);
      
      // Return the app.inkwell.model.User object
      return user;
   }

   @Transactional
   public UserDetails loadUserById(Long id) {
       User user = userRepository.findById(id)
               .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
       
       return user;
   }
}
