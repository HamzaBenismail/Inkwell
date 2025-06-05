package app.inkwell.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
@RequestMapping("/oauth2/authorize")
public class OAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);
    
    @GetMapping("/{provider}")
    public void authorizeOAuth2Provider(
            @PathVariable String provider,
            @RequestParam(required = false) String redirect_uri,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        logger.info("OAuth2 authorization request for provider: {}, redirect_uri: {}", provider, redirect_uri);
        
        // Store the redirect_uri in the session to be used after authentication
        if (redirect_uri != null && !redirect_uri.isEmpty()) {
            request.getSession().setAttribute("redirect_uri", redirect_uri);
            logger.info("Stored redirect_uri in session: {}", redirect_uri);
        } else {
            // Default redirect to home
            request.getSession().setAttribute("redirect_uri", "/Home");
        }
        
        // Redirect to the Spring Security OAuth2 authorization endpoint
        String authorizationUri = "/oauth2/authorization/" + provider;
        logger.info("Redirecting to OAuth2 authorization URI: {}", authorizationUri);
        response.sendRedirect(authorizationUri);
    }
}

