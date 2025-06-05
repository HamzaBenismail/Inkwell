package app.inkwell.controller;

import app.inkwell.model.User;
import app.inkwell.security.JwtTokenProvider;
import app.inkwell.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

@Autowired
private AuthenticationManager authenticationManager;

@Autowired
private UserService userService;

@Autowired
private JwtTokenProvider tokenProvider;

@PostMapping("/signin")
public String authenticateUser(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam(value = "remember-me", required = false) boolean rememberMe,
                              @RequestParam(value = "redirect", required = false) String redirect,
                              HttpServletRequest request,
                              HttpServletResponse response,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
    try {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
                )
        );

        // Get the authenticated user
        User user = (User) authentication.getPrincipal();

        if (user.getProvider() == null) {
            user.setProvider(0);
        }
        
        // Check if 2FA is enabled for this user, if so, redirect to 2FA verification
       if (user.isUsingMfa()) {
        return "redirect:/2fa/verify?userId=" + user.getId() + 
               (redirect != null ? "&redirect=" + redirect : "") + 
               (rememberMe ? "&rememberMe=true" : "");
        }
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // JWT token generation
        String jwt = tokenProvider.generateToken(authentication);
        
        // Jtw token cookie
        Cookie jwtCookie = new Cookie("jwtToken", jwt);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(rememberMe ? 7 * 24 * 60 * 60 : -1); 
        response.addCookie(jwtCookie);
        
        String redirectUrl = redirect != null && !redirect.isEmpty() ? redirect : "/Home";
        
        return "redirect:" + redirectUrl;
        
    } catch (AuthenticationException e) {
        logger.error("Authentication error", e);
        session.setAttribute("error", "Invalid username/email or password");
        return "redirect:/SignIn?error=true";
    }
}

@PostMapping("/login")
public String legacyLogin(@RequestParam("username") String username,
                         @RequestParam("password") String password,
                         @RequestParam(value = "remember-me", required = false) boolean rememberMe,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes,
                         HttpSession session) {
    // Redirect to the new endpoint
    return authenticateUser(username, password, rememberMe, redirect, request, response, redirectAttributes, session);
}

@PostMapping("/register")
public String registerUser(@RequestParam("username") String username,
                          @RequestParam("email") String email,
                          @RequestParam("password") String password,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes,
                          HttpSession session) {
    
    try {      
        // Check if username or email already exists
        if (userService.existsByUsername(username)) {
            logger.warn("Username already taken: {}", username);
            session.setAttribute("error", "Username is already taken!");
            return "redirect:/SignUp?error=true";
        }
        
        if (userService.existsByEmail(email)) {
            logger.warn("Email already in use: {}", email);
            session.setAttribute("error", "Email is already in use!");
            return "redirect:/SignUp?error=true";
        }
        
        // Register the user
        User user = userService.registerUser(username, email, password);
        logger.info("User registered successfully: {}", username);
        
        // Redirect to login page with success message
        return "redirect:/SignIn?success=true" + (redirect != null ? "&redirect=" + redirect : "");
        
    } catch (Exception e) {
        logger.error("Registration failed", e);
        String errorMessage = e.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "An unexpected error occurred during registration. Please try again.";
        }
        session.setAttribute("error", "Registration failed: " + errorMessage);
        return "redirect:/SignUp?error=true";
    }
}

@GetMapping("/logout")
public String logout(HttpServletRequest request, HttpServletResponse response) {
    // Clear the JWT cookie
    Cookie jwtCookie = new Cookie("jwtToken", null);
    jwtCookie.setPath("/");
    jwtCookie.setHttpOnly(true);
    jwtCookie.setMaxAge(0);
    response.addCookie(jwtCookie);
    
    // Invalidate session
    request.getSession().invalidate();
    
    return "redirect:/SignIn?logout=true";
}
}

