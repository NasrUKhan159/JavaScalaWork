import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
* This is where pending orders are stored. For simplicity, it uses SortedMaps, which are efficient for 
* managing prices. In a real-world engine, this class would not be thread-safe by design, as only the 
* single MatchingEngine thread will access it.
*/
public class OrderBook {
    // Buy book: highest price first
    private final SortedMap<BigDecimal, Queue<Order>> buyBook = new TreeMap<>(Comparator.reverseOrder());
    // Sell book: lowest price first
    private final SortedMap<BigDecimal, Queue<Order>> sellBook = new TreeMap<>();
    // Map to quickly look up orders by ID for cancellation
    private final Map<Long, Order> orderById = new ConcurrentHashMap<>(); // Concurrent for lookup/cancellation

    public void addOrder(Order order) {
        orderById.put(order.id, order);
        SortedMap<BigDecimal, Queue<Order>> book = (order.side == Order.Side.BUY) ? buyBook : sellBook;
        book.computeIfAbsent(order.price, k -> new LinkedList<>()).add(order);
        System.out.println("Added: " + order);
    }

    public void printBook() {
        System.out.println("\n--- Order Book ---");
        System.out.println("Sells:");
        sellBook.forEach((price, orders) -> System.out.printf("  %s @ %s (%d)\n", orders.size(), price, orders.stream().mapToLong(o -> o.quantity).sum()));
        System.out.println("Buys:");
        buyBook.forEach((price, orders) -> System.out.printf("  %s @ %s (%d)\n", orders.size(), price, orders.stream().mapToLong(o -> o.quantity).sum()));
        System.out.println("------------------\n");
    }

    // Matching logic would be here, but is handled by the single-threaded engine for safety/performance
}