package warehouse;

import java.util.List;

public class SearchResult {
    private final List<Product> items;
    private final int total;
    private final int page;
    private final int size;

    public SearchResult(List<Product> items, int total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<Product> getItems() { return items; }
    public int getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public int getTotalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}