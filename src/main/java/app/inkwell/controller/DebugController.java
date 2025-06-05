package app.inkwell.controller;

import app.inkwell.model.User;
import app.inkwell.service.TwoFactorAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);
    
    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @GetMapping("/auth-status")
    public Map<String, Object> getAuthStatus(@AuthenticationPrincipal User user, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        if (user != null) {
            response.put("authenticated", true);
            response.put("username", user.getUsername());
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("2faEnabled", user.isUsingMfa());
            
            // Include MFA info if enabled
            if (user.isUsingMfa()) {
                String mfaSecret = user.getMfaSecret();
                response.put("mfaSecretFormat", mfaSecret != null ? 
                        (mfaSecret.contains(":") ? "Contains device tokens" : "No device tokens") : "null");
                
                // Check if current device is remembered
                String deviceToken = getRememberDeviceToken(request);
                if (deviceToken != null) {
                    boolean isRemembered = twoFactorAuthService.isValidDeviceToken(user, deviceToken);
                    response.put("currentDeviceRemembered", isRemembered);
                } else {
                    response.put("currentDeviceRemembered", false);
                }
            }
        } else {
            response.put("authenticated", false);
        }
        
        // Check for cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Map<String, String> cookieMap = Arrays.stream(cookies)
                    .collect(Collectors.toMap(
                            Cookie::getName,
                            c -> {
                                if (c.getName().equals("jwtToken")) {
                                    return "****" + (c.getValue().length() > 10 ? 
                                            c.getValue().substring(c.getValue().length() - 10) : "");
                                } else if (c.getName().equals("rememberDevice")) {
                                    return "****" + (c.getValue().length() > 8 ? 
                                            c.getValue().substring(c.getValue().length() - 8) : "");
                                } else {
                                    return c.getValue();
                                }
                            }
                    ));
            response.put("cookies", cookieMap);
        } else {
            response.put("cookies", "No cookies found");
        }
        
        logger.info("Auth status request: {}", response);
        return response;
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
}