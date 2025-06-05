package app.inkwell.service;

import app.inkwell.model.Subscription;
import app.inkwell.model.User;
import app.inkwell.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    /**
     * Checks if a user is subscribed to a writer
     */
    public boolean isSubscribed(User subscriber, User writer) {
        if (subscriber == null || writer == null) {
            return false;
        }
        
        // Check if there's an active subscription
        return subscriptionRepository.existsBySubscriberAndWriterAndStatus(subscriber, writer, "active");
    }

    public List<Subscription> getSubscriptionsBySubscriber(User subscriber) {
        if (subscriber == null) {
            return List.of();
        }
        
        return subscriptionRepository.findBySubscriber(subscriber);
    }

    public List<Subscription> getActiveSubscriptionsBySubscriber(User subscriber) {
        if (subscriber == null) {
            return List.of();
        }
        
        List<Subscription> allSubscriptions = subscriptionRepository.findBySubscriber(subscriber);
        return allSubscriptions.stream()
                .filter(sub -> "active".equals(sub.getStatus()))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all writers that a user is subscribed to
     */
    public Set<User> getSubscribedWriters(User subscriber) {
        if (subscriber == null) {
            return new HashSet<>();
        }
        
        List<Subscription> subscriptions = subscriptionRepository.findBySubscriber(subscriber);
        
        // Filter for active subscriptions only and extract the writer
        return subscriptions.stream()
                .filter(sub -> "active".equals(sub.getStatus()))
                .map(Subscription::getWriter)
                .collect(Collectors.toSet());
    }
    
    /**
     * Gets all subscribers for a writer
     */
    public Set<User> getSubscribers(User writer) {
        if (writer == null) {
            return new HashSet<>();
        }
        
        List<Subscription> subscriptions = subscriptionRepository.findByWriter(writer);
        
        // Filter for active subscriptions only and extract the subscriber
        return subscriptions.stream()
                .filter(sub -> "active".equals(sub.getStatus()))
                .map(Subscription::getSubscriber)
                .collect(Collectors.toSet());
    }
    
    /**
     * Gets a count of active subscribers for a writer
     */
    public long getSubscriberCount(User writer) {
        return getSubscribers(writer).size();
    }
}