package app.inkwell.service;

public interface EmailService {
    void sendSubscriptionReceipt(String to, String subscriberName, String writerName, String plan, String amount);
}