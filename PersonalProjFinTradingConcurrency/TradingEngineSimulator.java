import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/*
* Producer/client simulator (multi-threaded) - multiple clients (simulated by separate
* threads) concurrently produce and submit orders to the engine.
*/
public class TradingEngineSimulator {
    private static final AtomicLong orderIdGenerator = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException {
        MatchingEngine engine = new MatchingEngine();
        ExecutorService engineExecutor = Executors.newSingleThreadExecutor();
        engineExecutor.submit(engine);

        ExecutorService producerExecutor = Executors.newFixedThreadPool(4);

        // Simulate multiple clients submitting orders concurrently
        for (int i = 0; i < 10; i++) {
            producerExecutor.submit(() -> {
                long id = orderIdGenerator.incrementAndGet();
                Order order = new Order(id, "GBPUSD", Order.Side.BUY, Order.Type.LIMIT, new BigDecimal("150000.00"), 10);
                engine.submitOrder(order);
            });
            producerExecutor.submit(() -> {
                long id = orderIdGenerator.incrementAndGet();
                Order order = new Order(id, "EURUSD", Order.Side.SELL, Order.Type.LIMIT, new BigDecimal("151000.00"), 5);
                engine.submitOrder(order);
            });
        }

        producerExecutor.shutdown();
        producerExecutor.awaitTermination(1, TimeUnit.MINUTES);
        engine.stop();
        engineExecutor.shutdown();
    }
}