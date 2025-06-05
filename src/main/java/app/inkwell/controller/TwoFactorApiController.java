package app.inkwell.controller;

import app.inkwell.model.User;
import app.inkwell.service.TwoFactorAuthService;
import app.inkwell.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorApiController {

    private static final Logger logger = LoggerFactory.getLogger(TwoFactorApiController.class);

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private UserService userService;

    @GetMapping("/status")
    public ResponseEntity<?> getTwoFactorStatus(@AuthenticationPrincipal User user) {
        if (user == null) {
            // Fallback to check SecurityContextHolder if @AuthenticationPrincipal is null
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            user = (User) auth.getPrincipal();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", user.isUsingMfa());
        logger.info("2FA status checked for user {}: {}", user.getUsername(), user.isUsingMfa());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup")
    public ResponseEntity<?> setupTwoFactor(@AuthenticationPrincipal User user) {
        if (user == null) {
            // Fallback to check SecurityContextHolder if @AuthenticationPrincipal is null
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            user = (User) auth.getPrincipal();
        }

        logger.info("Setting up 2FA for user: {}", user.getUsername());
        String secret = twoFactorAuthService.generateSecret();
        String qrCodeImage = twoFactorAuthService.generateQrCodeImageUri(user, secret);

        Map<String, Object> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeImage", qrCodeImage);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enableTwoFactor(
            @AuthenticationPrincipal User user,
            @RequestParam String secret,
            @RequestParam String code) {
        
        if (user == null) {
            // Fallback to check SecurityContextHolder if @AuthenticationPrincipal is null
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            user = (User) auth.getPrincipal();
        }

        logger.info("Enabling 2FA for user: {}", user.getUsername());
        boolean success = twoFactorAuthService.enableTwoFactorAuth(user, secret, code);
        
        if (success) {
            logger.info("2FA enabled successfully for user: {}", user.getUsername());
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Two-factor authentication enabled successfully"
            ));
        } else {
            logger.warn("Failed to enable 2FA for user: {} - invalid code", user.getUsername());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid verification code"
            ));
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableTwoFactor(@AuthenticationPrincipal User user) {
        if (user == null) {
            // Fallback to check SecurityContextHolder if @AuthenticationPrincipal is null
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            user = (User) auth.getPrincipal();
        }

        logger.info("Disabling 2FA for user: {}", user.getUsername());
        twoFactorAuthService.disableTwoFactorAuth(user);
        return ResponseEntity.ok(Map.of(
            "success", true, 
            "message", "Two-factor authentication disabled successfully"
        ));
    }
}