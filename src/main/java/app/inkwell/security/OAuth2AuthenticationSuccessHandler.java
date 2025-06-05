package app.inkwell.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import app.inkwell.model.User;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

  @Autowired
  private JwtTokenProvider tokenProvider;

  public OAuth2AuthenticationSuccessHandler() {
    // Set default target URL
    setDefaultTargetUrl("/Home");
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
    logger.info("OAuth2 authentication success for user: {}", authentication.getName());
    
    try {
      // Generate JWT token
      User user = (User) authentication.getPrincipal();
      String token = tokenProvider.generateToken(authentication);
      logger.info("Generated JWT token for user: {}", user.getUsername());
      
      // Set JWT as cookie
      Cookie jwtCookie = new Cookie("jwt", token);
      jwtCookie.setPath("/");
      jwtCookie.setHttpOnly(true);
      jwtCookie.setMaxAge(24 * 60 * 60); // 1 day
      response.addCookie(jwtCookie);
      
      // Store authentication in session
      request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", authentication);
      
      // Get the redirect URI
      String targetUrl = determineTargetUrl(request, response, authentication);
      logger.info("Redirecting to: {}", targetUrl);
      
      if (response.isCommitted()) {
        logger.debug("Response has already been committed. Unable to redirect.");
        return;
      }
      
      getRedirectStrategy().sendRedirect(request, response, targetUrl);
      
    } catch (Exception e) {
      logger.error("Error in OAuth2 success handler", e);
      getRedirectStrategy().sendRedirect(request, response, "/SignIn?error=oauth2");
    }
  }

  @Override
  protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    Optional<String> redirectUri = Optional.ofNullable((String) request.getSession().getAttribute("redirect_uri"));
    
    if (redirectUri.isPresent()) {
      logger.info("Using redirect_uri from session: {}", redirectUri.get());
      // Clear the session attribute
      request.getSession().removeAttribute("redirect_uri");
      return redirectUri.get();
    }
    
    logger.info("No redirect_uri in session, using default: {}", getDefaultTargetUrl());
    return getDefaultTargetUrl();
  }
}

