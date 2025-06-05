package app.inkwell.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.public.key}")
    private String stripePublicKey;
    
    @Value("${app.url}")
    private String appUrl;

    // Replace these with your actual Stripe price IDs
    // You can create these in your Stripe dashboard
    private static final String MONTHLY_PRICE_ID = "price_1RGgpTCWq3uv4VBYF2uM94Ar";
    private static final String ANNUAL_PRICE_ID = "price_YOUR_ANNUAL_PRICE_ID";

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Creates a Stripe checkout session for a subscription
     * 
     * @param plan The subscription plan (monthly/annual)
     * @param writerId The ID of the writer being subscribed to
     * @param userEmail The email of the subscriber
     * @return Checkout session
     * @throws StripeException If there's an error creating the session
     */
    public Session createSubscriptionCheckoutSession(String plan, Long writerId, String userEmail) 
            throws StripeException {
        
        String priceId = plan.equalsIgnoreCase("annual") ? ANNUAL_PRICE_ID : MONTHLY_PRICE_ID;
        String successUrl = appUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = appUrl + "/payment/cancel?writerId=" + writerId;
        
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .setCustomerEmail(userEmail)
            .putMetadata("writerId", writerId.toString())
            .putMetadata("plan", plan)
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .build());
        
        SessionCreateParams params = paramsBuilder.build();
        return Session.create(params);
    }

    /**
     * Retrieves a checkout session by ID
     * 
     * @param sessionId The Stripe session ID
     * @return The checkout session
     * @throws StripeException If there's an error retrieving the session
     */
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    /**
     * Gets the Stripe public key
     * 
     * @return The Stripe public key
     */
    public String getPublicKey() {
        return stripePublicKey;
    }
}