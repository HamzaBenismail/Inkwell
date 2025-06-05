package app.inkwell.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subscriber_id")
    private User subscriber;

    @ManyToOne
    @JoinColumn(name = "writer_id")
    private User writer;

    private String stripeSubscriptionId;
    
    private String plan; // "monthly" or "annual"
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private String status; // active, canceled, past_due, etc.

    // Constructors
    public Subscription() {
    }

    public Subscription(User subscriber, User writer, String stripeSubscriptionId, String plan) {
        this.subscriber = subscriber;
        this.writer = writer;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.plan = plan;
        this.startDate = LocalDateTime.now();
        this.status = "active";
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(User subscriber) {
        this.subscriber = subscriber;
    }

    public User getWriter() {
        return writer;
    }

    public void setWriter(User writer) {
        this.writer = writer;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime();
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}