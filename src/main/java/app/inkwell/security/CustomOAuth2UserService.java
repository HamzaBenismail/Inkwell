package app.inkwell.security;

import app.inkwell.model.Authority;
import app.inkwell.model.User;
import app.inkwell.repository.AuthorityRepository;
import app.inkwell.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

 private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

 @Autowired
 private UserRepository userRepository;
 
 @Autowired
 private AuthorityRepository authorityRepository;

 @Override
 public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
     OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
     
     try {
         return processOAuth2User(oAuth2UserRequest, oAuth2User);
     } catch (AuthenticationException ex) {
         throw ex;
     } catch (Exception ex) {
         throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
     }
 }

 private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
     String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
     OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
     
     if(!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
         throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
     }

     Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
     User user;

     if(userOptional.isPresent()) {
         user = userOptional.get();
         user = updateExistingUser(user, oAuth2UserInfo, registrationId);
     } else {
         user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
     }
     
     Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
     user.setAttributes(attributes);
     
     return user;
 }

 private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
     String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
     
     User user = new User();
     
     // Set OAuth2 provider info
     user.setOauth2Provider(registrationId);
     user.setOauth2Id(oAuth2UserInfo.getId());
     
     // Set provider as integer (1 for Google, 0 for local)
     if ("google".equalsIgnoreCase(registrationId)) {
         user.setProvider(1);
     } else {
         user.setProvider(0); // Default
     }
     
     // Set basic user info
     user.setEmail(oAuth2UserInfo.getEmail());
     user.setProfileImage(oAuth2UserInfo.getImageUrl());
     
     // Generate a unique username based on the name or email
     String baseUsername = oAuth2UserInfo.getName() != null ? 
             oAuth2UserInfo.getName().replaceAll("[^a-zA-Z0-9]", "") : 
             oAuth2UserInfo.getEmail().split("@")[0];
     
     if (baseUsername.length() < 3) {
         baseUsername = "user" + baseUsername;
     }
     
     String username = baseUsername;
     int counter = 1;
     
     while (userRepository.existsByUsername(username)) {
         username = baseUsername + counter++;
     }
     
     user.setUsername(username);
     
     // Set account status
     user.setEnabled(true);
     user.setAccountNonExpired(true);
     user.setAccountNonLocked(true);
     user.setCredentialsNonExpired(true);
     
     // Set timestamps
     user.setCreatedAt(LocalDateTime.now());
     user.setLastLogin(LocalDateTime.now());
     
     // Set a random password for OAuth2 users
     user.setPassword(UUID.randomUUID().toString());
     
     // Add USER role
     Authority userAuthority = authorityRepository.findByName("ROLE_USER")
             .orElseGet(() -> {
                 Authority authority = new Authority();
                 authority.setName("ROLE_USER");
                 return authorityRepository.save(authority);
             });
     
     user.addAuthority(userAuthority);
     
     logger.info("Saving new OAuth2 user: {}", username);
     return userRepository.save(user);
 }
 
 private User updateExistingUser(User user, OAuth2UserInfo oAuth2UserInfo, String registrationId) {
     // Update OAuth2 provider info if not already set
     if (user.getOauth2Provider() == null) {
         user.setOauth2Provider(registrationId);
         user.setOauth2Id(oAuth2UserInfo.getId());
         
         // Update provider as integer
         if ("google".equalsIgnoreCase(registrationId)) {
             user.setProvider(1);
         } 
     }
     
     // Update profile image if available
     if (oAuth2UserInfo.getImageUrl() != null) {
         user.setProfileImage(oAuth2UserInfo.getImageUrl());
     }
     
     // Update last login time
     user.setLastLogin(LocalDateTime.now());
     
     logger.info("Updating existing user: {}", user.getUsername());
     return userRepository.save(user);
 }
}
