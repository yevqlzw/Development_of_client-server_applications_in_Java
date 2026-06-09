package warehouse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ProductManager implements ProductService {
    private final Map<Integer, Product> products = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> groups = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(100);

    @Override
    public Product createProduct(String name, int groupId, int quantity, double price) {
        if (!groups.containsKey(groupId)) {
            throw new IllegalArgumentException("Group " + groupId + " does not exist");
        }
        int id = idGenerator.incrementAndGet();
        Product p = new Product(id, name, groupId, quantity, price);
        products.put(id, p);
        groups.get(groupId).add(id);
        return p;
    }

    @Override
    public synchronized boolean addProductToGroup(int groupId, int productId, String name) {
        if (!groups.containsKey(groupId)) return false;
        Product p = products.computeIfAbsent(productId,
                k -> new Product(k, name, groupId, 0, 0.0));
        if (p.getGroupId() != groupId) {
            Set<Integer> oldGroup = groups.get(p.getGroupId());
            if (oldGroup != null) oldGroup.remove(productId);
            p.setGroupId(groupId);
        }
        p.setName(name);
        groups.get(groupId).add(productId);
        return true;
    }

    @Override
    public Product getProduct(int id) {
        return products.get(id);
    }

    @Override
    public int getQuantity(int id) {
        Product p = products.get(id);
        return p == null ? -1 : p.getQuantity();
    }

    @Override
    public String getProductName(int id) {
        Product p = products.get(id);
        return p == null ? null : p.getName();
    }

    @Override
    public Collection<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    @Override
    public synchronized boolean updateProduct(int id, String newName, Integer newGroupId,
                                              Integer newQuantity, Double newPrice) {
        Product p = products.get(id);
        if (p == null) return false;
        if (newGroupId != null) {
            if (!groups.containsKey(newGroupId)) return false;
            Set<Integer> oldGroup = groups.get(p.getGroupId());
            if (oldGroup != null) oldGroup.remove(id);
            p.setGroupId(newGroupId);
            groups.get(newGroupId).add(id);
        }
        if (newName != null) p.setName(newName);
        if (newQuantity != null) p.setQuantity(newQuantity);
        if (newPrice != null) p.setPrice(newPrice);
        return true;
    }

    @Override
    public boolean setPrice(int id, double price) {
        Product p = products.get(id);
        if (p == null) return false;
        p.setPrice(price);
        return true;
    }

    @Override
    public boolean addQuantity(int id, int amount) {
        Product p = products.get(id);
        if (p == null) return false;
        while (true) {
            int current = p.getQuantity();
            if (p.compareAndSetQuantity(current, current + amount)) return true;
        }
    }

    @Override
    public boolean removeQuantity(int id, int amount) {
        Product p = products.get(id);
        if (p == null) return false;
        while (true) {
            int current = p.getQuantity();
            if (current < amount) return false;
            if (p.compareAndSetQuantity(current, current - amount)) return true;
        }
    }

    @Override
    public boolean deleteProduct(int id) {
        Product p = products.remove(id);
        if (p != null) {
            Set<Integer> group = groups.get(p.getGroupId());
            if (group != null) group.remove(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteGroup(int groupId) {
        Set<Integer> group = groups.remove(groupId);
        if (group == null) return false;
        for (Integer pid : group) {
            products.remove(pid);
        }
        return true;
    }

    @Override
    public SearchResult searchProducts(ProductSearchCriteria criteria) {
        List<Product> allFiltered = products.values().stream()
                .filter(p -> criteria.getName() == null || p.getName().toLowerCase().contains(criteria.getName().toLowerCase()))
                .filter(p -> criteria.getGroupId() == null || p.getGroupId() == criteria.getGroupId())
                .filter(p -> criteria.getMinQuantity() == null || p.getQuantity() >= criteria.getMinQuantity())
                .filter(p -> criteria.getMaxQuantity() == null || p.getQuantity() <= criteria.getMaxQuantity())
                .filter(p -> criteria.getMinPrice() == null || p.getPrice() >= criteria.getMinPrice())
                .filter(p -> criteria.getMaxPrice() == null || p.getPrice() <= criteria.getMaxPrice())
                .sorted(Comparator.comparingInt(Product::getId))
                .collect(Collectors.toList());

        int total = allFiltered.size();
        int page = criteria.getPage();
        int size = criteria.getSize();
        int start = page * size;
        if (start >= total) {
            return new SearchResult(Collections.emptyList(), total, page, size);
        }
        int end = Math.min(start + size, total);
        List<Product> items = new ArrayList<>(allFiltered.subList(start, end));
        return new SearchResult(items, total, page, size);
    }

    @Override
    public void createGroup(int groupId) {
        groups.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet());
    }

    @Override
    public boolean groupExists(int groupId) {
        return groups.containsKey(groupId);
    }
}