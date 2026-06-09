package pipeline;

import pipeline.enums.CommandType;
import pipeline.enums.ComponentType;
import protocol.Message;
import protocol.Package;
import warehouse.Product;
import warehouse.ProductService;
import warehouse.ProductSearchCriteria;
import warehouse.SearchResult;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<ClientPackage> inputQueue;
    private final BlockingQueue<ClientPackage> outputQueue;
    private final AtomicBoolean running;
    private final ProductService productManager;
    private final String name;

    public Processor(BlockingQueue<ClientPackage> inputQueue,
                     BlockingQueue<ClientPackage> outputQueue,
                     AtomicBoolean running,
                     ProductService productManager,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.productManager = productManager;
        this.name = type.getName(id);
    }

    private Message processCommand(Message request) {
        String msg = request.getMessage();
        try {
            CommandType cmd = CommandType.fromCode(request.getcType());
            switch (cmd) {
                case GET_QUANTITY:
                    String[] parts = msg.split(" ");
                    int id = Integer.parseInt(parts[0]);
                    int qty = productManager.getQuantity(id);
                    String prodName = productManager.getProductName(id);
                    return new Message(request.getcType(), request.getbUserId(),
                            (prodName != null ? prodName : "ID:" + id) + ": " + qty);

                case REMOVE_FROM_STOCK:
                    parts = msg.split(" ");
                    int pid = Integer.parseInt(parts[0]), amt = Integer.parseInt(parts[1]);
                    boolean ok = productManager.removeQuantity(pid, amt);
                    return new Message(request.getcType(), request.getbUserId(), ok ? "OK" : "FAILED");

                case DEPOSIT:
                    parts = msg.split(" ");
                    int pid2 = Integer.parseInt(parts[0]), amt2 = Integer.parseInt(parts[1]);
                    boolean deposited = productManager.addQuantity(pid2, amt2);
                    return new Message(request.getcType(), request.getbUserId(),
                            deposited ? "Deposited" : "Product not found");

                case ADD_GROUP:
                    parts = msg.split(" ");
                    productManager.createGroup(Integer.parseInt(parts[0]));
                    return new Message(request.getcType(), request.getbUserId(), "Group created");

                case ADD_PRODUCT:
                    parts = msg.split(" ");
                    int gid = Integer.parseInt(parts[0]), prid = Integer.parseInt(parts[1]);
                    String name = parts.length > 2 ? parts[2] : "Product-" + prid;
                    boolean added = productManager.addProductToGroup(gid, prid, name);
                    return new Message(request.getcType(), request.getbUserId(), added ? "Product added" : "Group not found");

                case SET_PRICE:
                    parts = msg.split(" ");
                    int prId = Integer.parseInt(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    boolean priceSet = productManager.setPrice(prId, price);
                    return new Message(request.getcType(), request.getbUserId(), priceSet ? "Price set" : "Product not found");

                case DELETE_PRODUCT:
                    parts = msg.split(" ");
                    int delId = Integer.parseInt(parts[0]);
                    boolean deleted = productManager.deleteProduct(delId);
                    return new Message(request.getcType(), request.getbUserId(), deleted ? "Deleted" : "Not found");

                case UPDATE_PRODUCT:
                    String[] pipeParts = msg.split("\\|");
                    if (pipeParts.length < 5) {
                        return new Message(request.getcType(), request.getbUserId(),
                                "ERROR: Invalid arguments. Expected: id|newName|newGroupId|newQuantity|newPrice (use null to skip)");
                    }
                    int updId = Integer.parseInt(pipeParts[0]);
                    String newName = pipeParts[1].equals("null") ? null : pipeParts[1];
                    Integer newGroup = pipeParts[2].equals("null") ? null : Integer.parseInt(pipeParts[2]);
                    Integer newQty = pipeParts[3].equals("null") ? null : Integer.parseInt(pipeParts[3]);
                    Double newPrice = pipeParts[4].equals("null") ? null : Double.parseDouble(pipeParts[4]);
                    boolean updated = productManager.updateProduct(updId, newName, newGroup, newQty, newPrice);
                    return new Message(request.getcType(), request.getbUserId(), updated ? "Updated" : "Fail");

                case SEARCH_PRODUCTS:
                    ProductSearchCriteria criteria = parseSearchCriteria(msg);
                    SearchResult result = productManager.searchProducts(criteria);
                    String response = serializeSearchResult(result);
                    return new Message(request.getcType(), request.getbUserId(), response);

                case GET_PRODUCT:
                    parts = msg.split(" ");
                    if (parts.length < 1) {
                        return new Message(request.getcType(), request.getbUserId(), "ERROR: Missing product ID");
                    }
                    int productId = Integer.parseInt(parts[0]);
                    Product product = productManager.getProduct(productId);
                    if (product == null) {
                        return new Message(request.getcType(), request.getbUserId(), "ERROR: Product not found");
                    }
                    String productData = product.getId() + "|" +
                            product.getName() + "|" +
                            product.getGroupId() + "|" +
                            product.getQuantity() + "|" +
                            product.getPrice();
                    return new Message(request.getcType(), request.getbUserId(), productData);

                case CREATE_PRODUCT:
                    pipeParts = msg.split("\\|");
                    if (pipeParts.length < 4) {
                        return new Message(request.getcType(), request.getbUserId(),
                                "ERROR: Invalid arguments. Expected: groupId|name|quantity|price");
                    }
                    try {
                        int groupId = Integer.parseInt(pipeParts[0]);
                        String prodNameNew = pipeParts[1];
                        int quantity = Integer.parseInt(pipeParts[2]);
                        double priceNew = Double.parseDouble(pipeParts[3]);
                        Product newProduct = productManager.createProduct(prodNameNew, groupId, quantity, priceNew);
                        return new Message(request.getcType(), request.getbUserId(),
                                "CREATED " + newProduct.getId());
                    } catch (IllegalArgumentException e) {
                        return new Message(request.getcType(), request.getbUserId(),
                                "ERROR: " + e.getMessage());
                    }

                default:
                    return new Message(request.getcType(), request.getbUserId(), "Unknown command");
            }
        } catch (IllegalArgumentException e) {
            return new Message(request.getcType(), request.getbUserId(), "Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return new Message(request.getcType(), request.getbUserId(), "Error: " + e.getMessage());
        }
    }

    private ProductSearchCriteria parseSearchCriteria(String msg) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        String[] pairs = msg.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length != 2) continue;
            String key = kv[0], val = kv[1];
            try {
                switch (key) {
                    case "name": criteria.setName(val); break;
                    case "groupId": criteria.setGroupId(Integer.parseInt(val)); break;
                    case "minQty": criteria.setMinQuantity(Integer.parseInt(val)); break;
                    case "maxQty": criteria.setMaxQuantity(Integer.parseInt(val)); break;
                    case "minPrice": criteria.setMinPrice(Double.parseDouble(val)); break;
                    case "maxPrice": criteria.setMaxPrice(Double.parseDouble(val)); break;
                    case "page": criteria.setPage(Integer.parseInt(val)); break;
                    case "size": criteria.setSize(Integer.parseInt(val)); break;
                }
            } catch (NumberFormatException ignored) {}
        }
        return criteria;
    }

    private String serializeSearchResult(SearchResult res) {
        StringBuilder sb = new StringBuilder();
        sb.append("TOTAL=").append(res.getTotal())
                .append(";PAGE=").append(res.getPage())
                .append(";SIZE=").append(res.getSize())
                .append(";TOTAL_PAGES=").append(res.getTotalPages())
                .append(";ITEMS=");
        for (Product p : res.getItems()) {
            String escapedName = p.getName().replace(";", "\\;").replace(":", "\\:");
            sb.append(p.getId()).append(":").append(escapedName).append(":")
                    .append(p.getPrice()).append(":").append(p.getQuantity()).append(";");
        }
        return sb.toString();
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ClientPackage cap = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (cap == null) continue;
                Package reqPkg = cap.getPackage();
                Message respMsg = processCommand(reqPkg.getMessage());
                Package respPkg = new Package((byte) 2, reqPkg.getbPktId(), respMsg);
                outputQueue.put(new ClientPackage(respPkg, cap.getClientId()));
                System.out.println("[" + name + "] Processed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}