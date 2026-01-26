import java.util.concurrent.*;

/*
* Single-threaded consumer: uns on a single thread and processes orders sequentially 
from a thread-safe BlockingQueue to prevent race conditions during matching.
*/
public class MatchingEngine implements Runnable {
    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
    private final OrderBook orderBook = new OrderBook();
    private volatile boolean running = true;

    public void submitOrder(Order order) {
        try {
            orderQueue.put(order); // Thread-safe submission
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Order order = orderQueue.take(); // Blocking wait for new orders
                processOrder(order);
                orderBook.printBook(); // Observe state after processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void processOrder(Order newOrder) {
        // Simplified processing: in a real engine this involves complex matching
        System.out.println("Processing order: " + newOrder);
        // For this basic example, just add the order to the book.
        // Complex matching logic goes here.
        orderBook.addOrder(newOrder);
    }

    public void stop() {
        running = false;
    }
}