package database;

import warehouse.Product;
import warehouse.ProductSearchCriteria;
import warehouse.ProductService;
import warehouse.SearchResult;

import java.sql.*;
import java.util.*;

public class ProductDbService implements ProductService {

    public ProductDbService() {
        DatabaseConnection.init();
    }

    private int update(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Update failed: " + sql, e);
        }
    }

    private long insertAndGetId(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed: " + sql, e);
        }
    }

    private Product queryForProduct(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapProduct(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    private List<Product> queryForProducts(String sql, Object... params) {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return list;
    }

    private long count(String sql, Object... params) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Count failed: " + sql, e);
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("group_id"),
                rs.getInt("quantity"),
                rs.getDouble("price")
        );
    }


    @Override
    public Product createProduct(String name, int groupId, int quantity, double price) {
        if (!groupExists(groupId)) throw new IllegalArgumentException("Group " + groupId + " does not exist");
        long id = insertAndGetId(
                "INSERT INTO product (name, group_id, quantity, price) VALUES (?, ?, ?, ?)",
                name, groupId, quantity, price
        );
        return new Product((int) id, name, groupId, quantity, price);
    }

    @Override
    public boolean addProductToGroup(int groupId, int productId, String name) {
        if (!groupExists(groupId)) return false;
        Product existing = getProduct(productId);
        if (existing != null) {
            return update("UPDATE product SET group_id = ?, name = ? WHERE id = ?", groupId, name, productId) > 0;
        } else {
            return update("INSERT INTO product (id, name, group_id, quantity, price) VALUES (?, ?, ?, 0, 0.0)",
                    productId, name, groupId) > 0;
        }
    }

    @Override
    public Product getProduct(int id) {
        return queryForProduct("SELECT id, name, group_id, quantity, price FROM product WHERE id = ?", id);
    }

    @Override
    public int getQuantity(int id) {
        Product p = getProduct(id);
        return p == null ? -1 : p.getQuantity();
    }

    @Override
    public String getProductName(int id) {
        Product p = getProduct(id);
        return p == null ? null : p.getName();
    }

    @Override
    public Collection<Product> getAllProducts() {
        return queryForProducts("SELECT id, name, group_id, quantity, price FROM product ORDER BY id");
    }

    @Override
    public boolean updateProduct(int id, String newName, Integer newGroupId, Integer newQuantity, Double newPrice) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (newName != null) { sets.add("name = ?"); params.add(newName); }
        if (newGroupId != null) {
            if (!groupExists(newGroupId)) return false;
            sets.add("group_id = ?"); params.add(newGroupId);
        }
        if (newQuantity != null) { sets.add("quantity = ?"); params.add(newQuantity); }
        if (newPrice != null) { sets.add("price = ?"); params.add(newPrice); }
        if (sets.isEmpty()) return true;
        params.add(id);
        String sql = "UPDATE product SET " + String.join(", ", sets) + " WHERE id = ?";
        return update(sql, params.toArray()) > 0;
    }

    @Override
    public boolean setPrice(int id, double price) {
        return update("UPDATE product SET price = ? WHERE id = ?", price, id) > 0;
    }

    @Override
    public boolean addQuantity(int id, int amount) {
        return update("UPDATE product SET quantity = quantity + ? WHERE id = ?", amount, id) > 0;
    }

    @Override
    public boolean removeQuantity(int id, int amount) {
        Product p = getProduct(id);
        if (p == null || p.getQuantity() < amount) return false;
        return update("UPDATE product SET quantity = quantity - ? WHERE id = ? AND quantity >= ?", amount, id, amount) > 0;
    }

    @Override
    public boolean deleteProduct(int id) {
        return update("DELETE FROM product WHERE id = ?", id) > 0;
    }

    @Override
    public boolean deleteGroup(int groupId) {
        update("DELETE FROM product WHERE group_id = ?", groupId);
        return update("DELETE FROM product_group WHERE id = ?", groupId) > 0;
    }

    @Override
    public SearchResult searchProducts(ProductSearchCriteria criteria) {
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            where.add("LOWER(name) LIKE ?");
            params.add("%" + criteria.getName().toLowerCase() + "%");
        }
        if (criteria.getGroupId() != null) {
            where.add("group_id = ?");
            params.add(criteria.getGroupId());
        }
        if (criteria.getMinQuantity() != null) {
            where.add("quantity >= ?");
            params.add(criteria.getMinQuantity());
        }
        if (criteria.getMaxQuantity() != null) {
            where.add("quantity <= ?");
            params.add(criteria.getMaxQuantity());
        }
        if (criteria.getMinPrice() != null) {
            where.add("price >= ?");
            params.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            where.add("price <= ?");
            params.add(criteria.getMaxPrice());
        }

        String whereClause = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        String orderBy = " ORDER BY id";
        String limit = " LIMIT ? OFFSET ?";

        int total = (int) count("SELECT COUNT(*) FROM product" + whereClause, params.toArray());

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(criteria.getSize());
        queryParams.add(criteria.getPage() * criteria.getSize());

        List<Product> items = queryForProducts(
                "SELECT id, name, group_id, quantity, price FROM product" + whereClause + orderBy + limit,
                queryParams.toArray()
        );

        return new SearchResult(items, total, criteria.getPage(), criteria.getSize());
    }

    @Override
    public void createGroup(int groupId) {
        update("INSERT OR IGNORE INTO product_group (id) VALUES (?)", groupId);
    }

    @Override
    public boolean groupExists(int groupId) {
        return count("SELECT 1 FROM product_group WHERE id = ?", groupId) > 0;
    }
}