package app.inkwell.controller;

import app.inkwell.model.Subscription;
import app.inkwell.model.User;
import app.inkwell.repository.SubscriptionRepository;
import app.inkwell.repository.UserRepository;
import app.inkwell.service.EmailService;
import app.inkwell.service.StripeService;
import app.inkwell.service.UserService;
import java.util.Date;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private StripeService stripeService;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @GetMapping("/payment/{username}")
    public String showPaymentPage(
            @PathVariable String username,
            @RequestParam(required = false, defaultValue = "monthly") String plan,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        try {
            if (currentUser == null) {
                return "redirect:/login?redirectUrl=/payment/" + username;
            }
            
            // Try to find user using the service
            Optional<User> writerOpt = userService.getUserByUsername(username);
            
            if (!writerOpt.isPresent()) {
                logger.error("Writer not found with username: {}", username);
                model.addAttribute("errorMessage", "Writer not found");
                return "redirect:/explore";
            }
            
            User writer = writerOpt.get();
            if (!writer.isWriter()) {
                logger.error("User {} is not a writer", username);
                model.addAttribute("errorMessage", "Selected user is not a writer");
                return "redirect:/explore";
            }
            
            // Check if user is already subscribed to this writer
            User subscriber = userService.getUserByUsername(currentUser.getUsername()).orElseThrow();
            boolean isSubscribed = subscriptionRepository.existsBySubscriberAndWriterAndStatus(
                    subscriber, writer, "active");
            
            if (isSubscribed) {
                model.addAttribute("errorMessage", "You are already subscribed to this writer");
                return "redirect:/profile/" + writer.getId();
            }
            
            model.addAttribute("writer", writer);
            model.addAttribute("selectedPlan", plan);
            model.addAttribute("stripePublicKey", stripeService.getPublicKey());
            return "Payment";
        } catch (Exception e) {
            logger.error("Error processing payment page for writer {}: {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while processing your request");
            return "redirect:/explore";
        }
    }
    
    @PostMapping("/payment/create-checkout-session")
    @ResponseBody
    public ResponseEntity<?> createCheckoutSession(
            @RequestParam String plan,
            @RequestParam Long writerId,
            @AuthenticationPrincipal UserDetails currentUser) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
            }
            
            User subscriber = userService.getUserByUsername(currentUser.getUsername()).orElseThrow();
            Optional<User> writerOpt = userRepository.findById(writerId);
            
            if (writerOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Writer not found"));
            }
            
            Session session = stripeService.createSubscriptionCheckoutSession(
                    plan, 
                    writerId, 
                    subscriber.getEmail()
            );
            
            return ResponseEntity.ok(Map.of("sessionId", session.getId()));
        } catch (StripeException e) {
            logger.error("Error creating Stripe checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        }
    }
    
    @GetMapping("/payment/success")
    public String handlePaymentSuccess(
            @RequestParam("session_id") String sessionId,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model) {
        try {
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            Session session = stripeService.retrieveSession(sessionId);
            String writerId = session.getMetadata().get("writerId");
            String plan = session.getMetadata().get("plan");
            
            User subscriber = userService.getUserByUsername(currentUser.getUsername()).orElseThrow();
            User writer = userRepository.findById(Long.parseLong(writerId)).orElseThrow();
            
            // Create subscription record
            Subscription subscription = new Subscription();
            subscription.setSubscriber(subscriber);
            subscription.setWriter(writer);
            subscription.setPlan(plan);
            subscription.setStatus("active");
            subscription.setStartDate(new Date());
            subscription.setStripeSubscriptionId(session.getSubscription());
            
            subscriptionRepository.save(subscription);
            
            // Send receipt email
            emailService.sendSubscriptionReceipt(
                subscriber.getEmail(),
                subscriber.getUsername(),
                writer.getUsername(),
                plan,
                session.getSubscription()
            );

            // Add attributes for the success page
            model.addAttribute("writer", writer);
            model.addAttribute("plan", plan);
            
            return "PaymentSuccess";
        } catch (Exception e) {
            logger.error("Error processing payment success: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "There was an error processing your payment");
            return "redirect:/error";
        }
    }
    
    @GetMapping("/payment/cancel")
    public String handlePaymentCancellation(
            @RequestParam(required = false) String writerId,
            Model model) {
        
        if (writerId != null) {
            return "redirect:/profile/" + writerId;
        }
        
        return "redirect:/explore";
    }
}