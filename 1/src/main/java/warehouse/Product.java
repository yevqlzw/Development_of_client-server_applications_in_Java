package warehouse;

import java.util.concurrent.atomic.AtomicInteger;

public class Product {
    private final int id;
    private volatile String name;
    private volatile int groupId;
    private final AtomicInteger quantity;
    private volatile double price;

    public Product(int id, String name, int groupId, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.groupId = groupId;
        this.quantity = new AtomicInteger(quantity);
        this.price = price;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    public boolean compareAndSetQuantity(int expected, int newValue) {
        return quantity.compareAndSet(expected, newValue);
    }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}