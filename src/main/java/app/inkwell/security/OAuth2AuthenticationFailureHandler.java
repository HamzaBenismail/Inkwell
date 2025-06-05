package app.inkwell.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                                        AuthenticationException exception) throws IOException, ServletException {
        
        String targetUrl = "/SignIn?error=true";
        
        // Add error message if available
        if (exception.getMessage() != null) {
            targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error_message", exception.getMessage())
                    .build().toUriString();
        }
        
        // Store the error in the session for Thymeleaf to access
        request.getSession().setAttribute("error", exception.getMessage());
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

