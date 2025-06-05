package app.inkwell.repository;

import app.inkwell.model.Subscription;
import app.inkwell.model.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findBySubscriber(User subscriber);
    List<Subscription> findByWriter(User writer);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    boolean existsBySubscriberAndWriterAndStatus(User subscriber, User writer, String status);
}