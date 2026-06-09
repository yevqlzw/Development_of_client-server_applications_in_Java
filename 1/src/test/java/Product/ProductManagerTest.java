package Product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import warehouse.Product;
import warehouse.ProductManager;
import warehouse.ProductSearchCriteria;
import warehouse.SearchResult;
import static org.junit.jupiter.api.Assertions.*;

class ProductManagerTest {
    private ProductManager pm;

    @BeforeEach
    void setUp() {
        pm = new ProductManager();
        pm.createGroup(1);
        pm.createGroup(2);
        pm.addProductToGroup(1, 10, "Shirt");
        pm.addProductToGroup(1, 11, "Jeans");
        pm.addQuantity(10, 100);
        pm.addQuantity(11, 200);
        pm.setPrice(10, 45.50);
        pm.setPrice(11, 30.00);
    }

    @Test
    void testCreateProductSuccess() {
        Product p = pm.createProduct("Hat", 2, 50, 12.99);
        assertNotNull(p);
        assertTrue(p.getId() > 100);
        assertEquals("Hat", p.getName());
        assertEquals(2, p.getGroupId());
        assertEquals(50, p.getQuantity());
        assertEquals(12.99, p.getPrice());
    }

    @Test
    void testCreateProductFailsForMissingGroup() {
        assertThrows(IllegalArgumentException.class, () -> pm.createProduct("Hat", 99, 10, 5.0));
    }

    @Test
    void testAddProductToGroupWhenProductExists() {
        assertTrue(pm.addProductToGroup(2, 10, "New Shirt"));
        Product p = pm.getProduct(10);
        assertEquals(2, p.getGroupId());
        assertEquals("New Shirt", p.getName());
    }

    @Test
    void testGetProduct() {
        Product p = pm.getProduct(10);
        assertNotNull(p);
        assertEquals("Shirt", p.getName());
        assertEquals(100, p.getQuantity());
    }

    @Test
    void testUpdateProduct() {
        boolean updated = pm.updateProduct(10, "T-Shirt", 2, 150, 49.99);
        assertTrue(updated);
        Product p = pm.getProduct(10);
        assertEquals("T-Shirt", p.getName());
        assertEquals(2, p.getGroupId());
        assertEquals(150, p.getQuantity());
        assertEquals(49.99, p.getPrice());
        assertTrue(pm.groupExists(2));
        assertTrue(pm.getProduct(10).getGroupId() == 2);
    }

    @Test
    void testDeleteProduct() {
        assertTrue(pm.deleteProduct(10));
        assertNull(pm.getProduct(10));
        assertEquals(-1, pm.getQuantity(10));
    }

    @Test
    void testAddQuantity() {
        assertTrue(pm.addQuantity(10, 30));
        assertEquals(130, pm.getQuantity(10));
        assertFalse(pm.addQuantity(999, 10));
    }

    @Test
    void testRemoveQuantity() {
        assertTrue(pm.removeQuantity(10, 30));
        assertEquals(70, pm.getQuantity(10));
        assertFalse(pm.removeQuantity(10, 200));
        assertFalse(pm.removeQuantity(999, 10));
    }

    @Test
    void testSetPrice() {
        assertTrue(pm.setPrice(10, 99.99));
        assertEquals(99.99, pm.getProduct(10).getPrice());
        assertFalse(pm.setPrice(999, 10.0));
    }

    @Test
    void testSearchByName() {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setName("shirt");
        SearchResult result = pm.searchProducts(criteria);
        assertEquals(1, result.getTotal());
        assertEquals("Shirt", result.getItems().get(0).getName());
    }

    @Test
    void testSearchByPriceRange() {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setMinPrice(40.0);
        criteria.setMaxPrice(50.0);
        SearchResult result = pm.searchProducts(criteria);
        assertEquals(1, result.getTotal());
        assertEquals(10, result.getItems().get(0).getId());
    }

    @Test
    void testSearchWithPagination() {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setGroupId(1);
        criteria.setPage(0);
        criteria.setSize(1);
        SearchResult result = pm.searchProducts(criteria);
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testSearchCombinedFilters() {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setGroupId(1);
        criteria.setMinQuantity(150);
        criteria.setMaxQuantity(250);
        SearchResult result = pm.searchProducts(criteria);
        assertEquals(1, result.getTotal());
        assertEquals(11, result.getItems().get(0).getId());
    }

    @Test
    void testDeleteGroup() {
        assertTrue(pm.deleteGroup(1));
        assertFalse(pm.groupExists(1));
        assertNull(pm.getProduct(10));
        assertNull(pm.getProduct(11));
        assertTrue(pm.groupExists(2));
    }

    @Test
    void testConcurrentRemoveQuantity() throws InterruptedException {
        Thread t1 = new Thread(() -> pm.removeQuantity(10, 30));
        Thread t2 = new Thread(() -> pm.removeQuantity(10, 30));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        int qty = pm.getQuantity(10);
        assertTrue(qty == 40 || qty == 70);
    }
}