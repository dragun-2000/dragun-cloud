package vn.co.cake.queue;

public interface MessagePublisher {

    void publish(final String message);
}