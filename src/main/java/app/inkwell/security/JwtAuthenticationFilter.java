package app.inkwell.security;

import app.inkwell.model.User;
import app.inkwell.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

   private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

   @Autowired
   private JwtTokenProvider tokenProvider;

   @Autowired
   private UserRepository userRepository;

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
       try {
           // Skip if user is already authenticated
           if (SecurityContextHolder.getContext().getAuthentication() != null && 
               SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
               !(SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
               logger.debug("User is already authenticated: {}", SecurityContextHolder.getContext().getAuthentication().getName());
               filterChain.doFilter(request, response);
               return;
           }
           
           String jwt = getJwtFromRequest(request);

           if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
               Long userId = tokenProvider.getUserIdFromJWT(jwt);

               if (userId != null) {
                   Optional<User> userOptional = userRepository.findById(userId);
                   if (userOptional.isPresent()) {
                       User user = userOptional.get();

                       // Check for "remember this device" cookie
                      Cookie[] cookies = request.getCookies();
                      boolean rememberDevice = false;
                      if (cookies != null) {
                          for (Cookie cookie : cookies) {
                              if ("rememberDevice".equals(cookie.getName())) {
                                  // Validate the cookie value against the stored token
                                  if (user.getMfaSecret() != null && user.getMfaSecret().contains(":" + cookie.getValue())) {
                                      rememberDevice = true;
                                  }
                                  break;
                              }
                          }
                      }
                      
                      // Bypass 2FA if "remember this device" cookie is present
                      if (user.isUsingMfa() && !rememberDevice) {
                          logger.debug("2FA is enabled, but 'rememberDevice' cookie is missing or invalid. Skipping authentication.");
                          filterChain.doFilter(request, response);
                          return;
                      }
                       
                       UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                           user, null, user.getAuthorities());
                       authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                       SecurityContextHolder.getContext().setAuthentication(authentication);
                       logger.debug("Set authentication for user: {}", user.getUsername());
                   } else {
                       logger.warn("User not found with ID: {}", userId);
                   }
               } else {
                   logger.warn("Could not get user ID from JWT token");
               }
           } else {
               logger.debug("No valid JWT token found");
           }
       } catch (Exception ex) {
           logger.error("Could not set user authentication in security context", ex);
       }

       filterChain.doFilter(request, response);
   }

   private String getJwtFromRequest(HttpServletRequest request) {
       // First check Authorization header
       String bearerToken = request.getHeader("Authorization");
       if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
           return bearerToken.substring(7);
       }
       
       // Then check cookies
       Cookie[] cookies = request.getCookies();
       if (cookies != null) {
           Optional<Cookie> jwtCookie = Arrays.stream(cookies)
               .filter(cookie -> "jwt".equals(cookie.getName()))
               .findFirst();
           
           if (jwtCookie.isPresent()) {
               return jwtCookie.get().getValue();
           }
       }
       
       return null;
   }
}
