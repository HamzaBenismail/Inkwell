package app.inkwell.controller;

import app.inkwell.model.User;
import app.inkwell.security.JwtTokenProvider;
import app.inkwell.service.TwoFactorAuthService;
import app.inkwell.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class TwoFactorVerificationController {

  private static final Logger logger = LoggerFactory.getLogger(TwoFactorVerificationController.class);

  @Autowired
  private TwoFactorAuthService twoFactorAuthService;

  @Autowired
  private UserService userService;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @GetMapping("/2fa/verify")
  public String showVerificationPage(
          @RequestParam("userId") Long userId,
          @RequestParam(value = "redirect", required = false) String redirect,
          @RequestParam(value = "rememberMe", required = false) boolean rememberMe,
          Model model,
          HttpSession session) {
      
      logger.info("Showing 2FA verification page for user ID: {}", userId);
      
      // Check if user exists and has 2FA enabled
      Optional<User> userOpt = userService.getUserById(userId);
      if (userOpt.isEmpty() || !userOpt.get().isUsingMfa()) {
          logger.warn("User not found or 2FA not enabled for user ID: {}", userId);
          return "redirect:/SignIn?error=true";
      }
      
      // Store parameters in session for the post request
      session.setAttribute("2fa_userId", userId);
      session.setAttribute("2fa_redirect", redirect);
      session.setAttribute("2fa_rememberMe", rememberMe);
      
      model.addAttribute("userId", userId);
      return "2fa-verification";
  }

  @PostMapping("/2fa/verify")
  public String verifyCode(
          @RequestParam("code") String code,
          @RequestParam(value = "rememberDevice", required = false) boolean rememberDevice,
          HttpSession session,
          HttpServletResponse response) {
      
      // Get parameters from session
      Long userId = (Long) session.getAttribute("2fa_userId");
      String redirect = (String) session.getAttribute("2fa_redirect");
      Boolean rememberMe = (Boolean) session.getAttribute("2fa_rememberMe");
      
      logger.info("Verifying 2FA code for user ID: {}, rememberDevice: {}", userId, rememberDevice);
      
      if (userId == null) {
          logger.warn("No userId found in session for 2FA verification");
          return "redirect:/SignIn?error=true";
      }
      
      // Check if user exists and has 2FA enabled
      Optional<User> userOpt = userService.getUserById(userId);
      if (userOpt.isEmpty() || !userOpt.get().isUsingMfa()) {
          logger.warn("User not found or 2FA not enabled for user ID: {}", userId);
          return "redirect:/SignIn?error=true";
      }
      
      User user = userOpt.get();
      
      // Verify the code
      if (twoFactorAuthService.verifyCode(code, user.getMfaSecret())) {
          logger.info("2FA verification successful for user: {}", user.getUsername());
          // Generate JWT token
          String jwt = tokenProvider.generateToken(user);
          
          // Set JWT as cookie
          Cookie jwtCookie = new Cookie("jwtToken", jwt);
          jwtCookie.setPath("/");
          jwtCookie.setHttpOnly(true);
          jwtCookie.setMaxAge(rememberMe != null && rememberMe ? 7 * 24 * 60 * 60 : -1); // 7 days if remember me, else session cookie
          response.addCookie(jwtCookie);
          
          // Handle "Remember this device" functionality
          if (rememberDevice) {
            logger.info("Remembering device for user: {}", user.getUsername());
            String deviceToken = twoFactorAuthService.addRememberDeviceToken(user);
            
            // Create the remember device cookie with proper attributes
            Cookie rememberDeviceCookie = new Cookie("rememberDevice", deviceToken);
            rememberDeviceCookie.setPath("/");
            rememberDeviceCookie.setHttpOnly(true);
            rememberDeviceCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
            response.addCookie(rememberDeviceCookie);
            
            logger.info("Set rememberDevice cookie: {}", deviceToken);
          }

          // Clean up session
          session.removeAttribute("2fa_userId");
          session.removeAttribute("2fa_redirect");
          session.removeAttribute("2fa_rememberMe");
          
          // Redirect to the requested page or default to home
          String redirectUrl = redirect != null && !redirect.isEmpty() ? redirect : "/Home";
          logger.info("Redirecting to: {}", redirectUrl);
          return "redirect:" + redirectUrl;
      } else {
          logger.warn("Invalid 2FA code provided for user ID: {}", userId);
          return "redirect:/2fa/verify?userId=" + userId + "&error=true" + 
                 (redirect != null ? "&redirect=" + redirect : "") + 
                 (rememberMe != null && rememberMe ? "&rememberMe=true" : "");
      }
  }
}