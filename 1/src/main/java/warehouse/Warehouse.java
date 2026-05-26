package warehouse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Warehouse {
    private final ConcurrentHashMap<Integer, AtomicInteger> inventory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Set<Integer>> productGroups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> productNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Double> prices = new ConcurrentHashMap<>();

    public int getQuantity(int productID) {
        AtomicInteger count = inventory.get(productID);
        return count != null ? count.get() : -1;
    }

    public boolean removeQuantity(int productID, int debit) {
        AtomicInteger count = inventory.get(productID);
        if (count == null) return false;

        while (true) {
            int current = count.get();
            if (current < debit) return false;
            if (count.compareAndSet(current, current - debit)) return true;
        }
    }

    public void addQuantity(int productID, int quantity) {
        AtomicInteger count = inventory.computeIfAbsent(productID, key -> new AtomicInteger(0));
        count.addAndGet(quantity);
    }

    public void createGroup(int groupID) {
        productGroups.computeIfAbsent(groupID, key -> ConcurrentHashMap.newKeySet());
    }

    public boolean addProductNameToGroup(int groupID, int productID, String productName) {
        Set<Integer> group = productGroups.computeIfPresent(groupID, (key, existingGroup) -> {
            productNames.put(productID, productName);
            existingGroup.add(productID);
            inventory.putIfAbsent(productID, new AtomicInteger(0));
            prices.putIfAbsent(productID, 0.0);
            return existingGroup;
        });
        return group != null;
    }

    public void setPrice(int productID, double price) {
        prices.put(productID, price);
    }

    public Double getPrice(int productID) {
        return prices.get(productID);
    }

    public String getProductName(int productID) {
        return productNames.get(productID);
    }

    public boolean groupExists(int groupID) {
        return productGroups.containsKey(groupID);
    }

    public boolean productExists(int productID) {
        return inventory.containsKey(productID);
    }
}