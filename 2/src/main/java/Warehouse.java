import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Warehouse {
                                   //productID  count
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
        if (count == null) {
            return false;
        }
        while (true) {
            int currentQuantity = count.get();
            if (currentQuantity < debit) {
                return false;
            }

            int newQuantity = currentQuantity - debit;
            if (count.compareAndSet(currentQuantity, newQuantity)) {
                return true;
            }
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
        Set<Integer> group = productGroups.get(groupID);
        if (group == null) {
            return false;
        }
        productNames.put(productID, productName);
        group.add(productID);
        return true;
    }

    public void setPrice(int productID, double price) {
        prices.put(productID, price);
    }
}