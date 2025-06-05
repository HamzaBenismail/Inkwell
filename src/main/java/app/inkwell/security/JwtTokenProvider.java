package app.inkwell.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

   private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

   @Value("${app.jwt.secret}")
   private String jwtSecret;

   @Value("${app.jwt.expiration-ms}")
   private int jwtExpirationInMs;

   public String generateToken(Authentication authentication) {
       if (authentication == null || authentication.getPrincipal() == null) {
           logger.error("Authentication or principal is null");
           return null;
       }
       
       try {
           Object principal = authentication.getPrincipal();
           if (!(principal instanceof app.inkwell.model.User)) {
               logger.error("Principal is not a User instance: {}", principal.getClass().getName());
               return null;
           }
           
           app.inkwell.model.User userPrincipal = (app.inkwell.model.User) principal;
           if (userPrincipal.getId() == null) {
               logger.error("User ID is null");
               return null;
           }
           
           logger.info("Generating token for user: {} (ID: {})", userPrincipal.getUsername(), userPrincipal.getId());
           
           Date now = new Date();
           Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
           
           return Jwts.builder()
                   .setSubject(Long.toString(userPrincipal.getId()))
                   .setIssuedAt(new Date())
                   .setExpiration(expiryDate)
                   .signWith(getSigningKey())
                   .compact();
       } catch (Exception e) {
           logger.error("Error generating JWT token", e);
           return null;
       }
   }
   
   public String generateToken(app.inkwell.model.User user) {
       if (user == null || user.getId() == null) {
           logger.error("User or user ID is null");
           return null;
       }
       
       try {
           logger.info("Generating token for user: {} (ID: {})", user.getUsername(), user.getId());
           
           Date now = new Date();
           Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
           
           return Jwts.builder()
                   .setSubject(Long.toString(user.getId()))
                   .setIssuedAt(new Date())
                   .setExpiration(expiryDate)
                   .signWith(getSigningKey())
                   .compact();
       } catch (Exception e) {
           logger.error("Error generating JWT token", e);
           return null;
       }
   }

   public Long getUserIdFromJWT(String token) {
       try {
           Claims claims = Jwts.parserBuilder()
                   .setSigningKey(getSigningKey())
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
           
           String subject = claims.getSubject();
           if (subject == null || subject.isEmpty()) {
               logger.error("JWT subject is null or empty");
               return null;
           }
           
           try {
               return Long.parseLong(subject);
           } catch (NumberFormatException e) {
               logger.error("Invalid user ID in JWT token: {}", subject);
               throw e;
           }
       } catch (Exception e) {
           logger.error("Error extracting user ID from JWT", e);
           throw e;
       }
   }

   public boolean validateToken(String authToken) {
       if (authToken == null || authToken.isEmpty()) {
           logger.error("JWT token is null or empty");
           return false;
       }
       
       try {
           Jwts.parserBuilder()
               .setSigningKey(getSigningKey())
               .build()
               .parseClaimsJws(authToken);
           return true;
       } catch (SignatureException ex) {
           logger.error("Invalid JWT signature");
           return false;
       } catch (MalformedJwtException ex) {
           logger.error("Invalid JWT token");
           return false;
       } catch (ExpiredJwtException ex) {
           logger.error("Expired JWT token");
           return false;
       } catch (UnsupportedJwtException ex) {
           logger.error("Unsupported JWT token");
           return false;
       } catch (IllegalArgumentException ex) {
           logger.error("JWT claims string is empty");
           return false;
       } catch (Exception ex) {
           logger.error("JWT validation error", ex);
           return false;
       }
   }

   private SecretKey getSigningKey() {
       byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
       return Keys.hmacShaKeyFor(keyBytes);
   }
}
