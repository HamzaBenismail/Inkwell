package app.inkwell.security;

import app.inkwell.model.User;
import app.inkwell.service.TwoFactorAuthService;
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
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    public CustomAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/Home");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Get authenticated user
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            logger.error("Principal is not a User instance: {}", principal.getClass().getName());
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        
        User user = (User) principal;
        
        // Parse parameters
        String redirect = request.getParameter("redirect");
        boolean rememberMe = Optional.ofNullable(request.getParameter("remember-me"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        
        // Check if 2FA is enabled for the user
        if (user.isUsingMfa()) {     
            // Check if this device is remembered for 2FA
            String deviceToken = getRememberDeviceToken(request);
            boolean isRememberedDevice = deviceToken != null && twoFactorAuthService.isValidDeviceToken(user, deviceToken);
            
            if (isRememberedDevice) {
                // Skip 2FA, issue JWT token and redirect
                issueJwtToken(response, user, rememberMe);
                redirectToTargetUrl(request, response, redirect);
            } else {
                // Redirect to 2FA verification page
                String targetUrl = "/2fa/verify?userId=" + user.getId();
                if (redirect != null && !redirect.isEmpty()) {
                    targetUrl += "&redirect=" + redirect;
                }
                if (rememberMe) {
                    targetUrl += "&rememberMe=true";
                }
                
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
        } else {
            // No 2FA, issue JWT token and redirect
            issueJwtToken(response, user, rememberMe);
            redirectToTargetUrl(request, response, redirect);
        }
    }
    
    private String getRememberDeviceToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("rememberDevice".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    private void issueJwtToken(HttpServletResponse response, User user, boolean rememberMe) {
        String jwt = tokenProvider.generateToken(user);
        
        Cookie jwtCookie = new Cookie("jwtToken", jwt);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        // 7 days for rememberMe
        jwtCookie.setMaxAge(rememberMe ? 7 * 24 * 60 * 60 : -1);
        response.addCookie(jwtCookie);
    }
    
    private void redirectToTargetUrl(HttpServletRequest request, HttpServletResponse response, String redirect) 
            throws IOException {
        String targetUrl = redirect != null && !redirect.isEmpty() ? redirect : getDefaultTargetUrl();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}