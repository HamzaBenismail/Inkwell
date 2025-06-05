package app.inkwell.controller;

import app.inkwell.model.Subscription;
import app.inkwell.repository.SubscriptionRepository;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret:whsec_test}")
    private String webhookSecret;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }
        
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            logger.warn("Failed to deserialize Stripe event data");
            return ResponseEntity.ok("Event received, but data could not be deserialized");
        }
        
        // Handle the event
        switch (event.getType()) {
            case "customer.subscription.created":
                logger.info("Subscription created: {}", event.getId());
                break;
                
            case "customer.subscription.updated":
                logger.info("Subscription updated: {}", event.getId());
                handleSubscriptionUpdated(stripeObject);
                break;
                
            case "customer.subscription.deleted":
                logger.info("Subscription canceled: {}", event.getId());
                handleSubscriptionDeleted(stripeObject);
                break;
                
            default:
                logger.info("Unhandled event type: {}", event.getType());
                break;
        }
        
        return ResponseEntity.ok("Webhook received");
    }
    
    private void handleSubscriptionUpdated(StripeObject subscription) {
        try {
            String subscriptionId = (String) subscription.getClass()
                    .getMethod("getId")
                    .invoke(subscription);
            
            String status = (String) subscription.getClass()
                    .getMethod("getStatus")
                    .invoke(subscription);
            
            Optional<Subscription> subscriptionOpt = 
                    subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            
            if (subscriptionOpt.isPresent()) {
                Subscription sub = subscriptionOpt.get();
                sub.setStatus(status);
                subscriptionRepository.save(sub);
                logger.info("Updated subscription status to {}: {}", status, subscriptionId);
            } else {
                logger.warn("Subscription not found in the database: {}", subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Error updating subscription status: {}", e.getMessage(), e);
        }
    }
    
    private void handleSubscriptionDeleted(StripeObject subscription) {
        try {
            String subscriptionId = (String) subscription.getClass()
                    .getMethod("getId")
                    .invoke(subscription);
            
            Optional<Subscription> subscriptionOpt = 
                    subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            
            if (subscriptionOpt.isPresent()) {
                Subscription sub = subscriptionOpt.get();
                sub.setStatus("canceled");
                subscriptionRepository.save(sub);
                logger.info("Marked subscription as canceled: {}", subscriptionId);
            } else {
                logger.warn("Subscription not found in the database: {}", subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Error handling subscription deletion: {}", e.getMessage(), e);
        }
    }
}