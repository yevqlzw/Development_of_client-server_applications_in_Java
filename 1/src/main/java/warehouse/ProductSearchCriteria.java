package warehouse;

public class ProductSearchCriteria {
    private String name;
    private Integer groupId;
    private Integer minQuantity;
    private Integer maxQuantity;
    private Double minPrice;
    private Double maxPrice;
    private int page = 0;
    private int size = 10;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }
    public Integer getMinQuantity() { return minQuantity; }
    public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public int getPage() { return page; }
    public void setPage(int page) {
        if (page < 0) throw new IllegalArgumentException("Page must be >= 0");
        this.page = page;
    }
    public int getSize() { return size; }
    public void setSize(int size) {
        if (size <= 0) throw new IllegalArgumentException("Size must be > 0");
        this.size = size;
    }
}