package warehouse;

import java.util.Collection;

public interface ProductService {
    Product createProduct(String name, int groupId, int quantity, double price);
    boolean addProductToGroup(int groupId, int productId, String name);

    Product getProduct(int id);
    int getQuantity(int id);
    String getProductName(int id);
    Collection<Product> getAllProducts();

    boolean updateProduct(int id, String newName, Integer newGroupId, Integer newQuantity, Double newPrice);
    boolean setPrice(int id, double price);
    boolean addQuantity(int id, int amount);
    boolean removeQuantity(int id, int amount);

    boolean deleteProduct(int id);
    boolean deleteGroup(int groupId);

    SearchResult searchProducts(ProductSearchCriteria criteria);

    void createGroup(int groupId);
    boolean groupExists(int groupId);
}