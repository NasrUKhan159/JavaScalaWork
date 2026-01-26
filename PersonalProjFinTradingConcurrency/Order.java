import java.math.BigDecimal;

/*
* This class represnts a single trade order 
*/
public class Order {
    public enum Side { BUY, SELL }
    public enum Type { MARKET, LIMIT }

    final long id;
    final String symbol;
    final Side side;
    final Type type;
    final BigDecimal price; // Use BigDecimal for financial accuracy
    long quantity;

    public Order(long id, String symbol, Side side, Type type, BigDecimal price, long quantity) {
        this.id = id;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return String.format("Order{id=%d, symbol='%s', side=%s, type=%s, price=%s, quantity=%d}", id, symbol, side, type, price, quantity);
    }
}