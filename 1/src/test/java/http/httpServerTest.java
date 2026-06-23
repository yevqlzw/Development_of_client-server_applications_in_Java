package http;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import network.http.httpServer;
import org.junit.jupiter.api.*;
import warehouse.ProductService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class httpServerTest {
    private httpServer server;
    private String token;

    @BeforeAll
    void startServer() throws IOException {
        Files.deleteIfExists(Paths.get("warehouse.db"));
        server = new httpServer(8082);
        initTestData();
        server.start();
        RestAssured.port = 8082;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private void initTestData() {
        ProductService ps = server.getProductService();
        ps.createGroup(1);
        ps.createGroup(2);
        addProductIfMissing(ps, 10, "Shirt", 1, 100, 45.50);
        addProductIfMissing(ps, 11, "Jeans", 1, 200, 30.00);
        addProductIfMissing(ps, 20, "Jacket", 2, 500, 15.00);
        addProductIfMissing(ps, 21, "Socks", 2, 300, 35.00);
    }

    private void addProductIfMissing(ProductService ps, int id, String name, int groupId, int quantity, double price) {
        if (ps.getProduct(id) == null) {
            ps.addProductToGroup(groupId, id, name);
            ps.addQuantity(id, quantity);
            ps.setPrice(id, price);
        }
    }

    @AfterAll
    void stopServer() {
        server.stop();
    }

    @BeforeEach
    void login() {
        token = given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"qwerty123\"}")
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .path("token");
    }

    @Test
    void shouldGetProductById() {
        given()
                .auth().oauth2(token)
                .when()
                .get("/products/10")
                .then()
                .statusCode(200)
                .body("id", is(10))
                .body("name", is("Shirt"))
                .body("price", is(45.5f));
    }

    @Test
    void shouldReturn404ForMissingProduct() {
        given()
                .auth().oauth2(token)
                .when()
                .get("/products/999")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldCreateProduct() {
        String body = "{\"name\":\"Hat\",\"groupId\":1,\"quantity\":50,\"price\":12.99}";
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/products")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("name", is("Hat"))
                .body("groupId", is(1));
    }

    @Test
    void shouldNotCreateDuplicateProduct() {
        String body = "{\"name\":\"Shirt\",\"groupId\":1,\"quantity\":10,\"price\":10.0}";
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/products")
                .then()
                .statusCode(409);
    }

    @Test
    void shouldUpdateProduct() {
        String createBody = "{\"name\":\"OldName\",\"groupId\":1,\"quantity\":10,\"price\":5.0}";
        int id = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .put("/products")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        String update = "{\"name\":\"NewName\",\"price\":7.0}";
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .post("/products/" + id)
                .then()
                .statusCode(200)
                .body("name", is("NewName"))
                .body("price", is(7.0f));
    }

    @Test
    void shouldDeleteProduct() {
        String createBody = "{\"name\":\"Temporary\",\"groupId\":1,\"quantity\":1,\"price\":1.0}";
        int id = given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .put("/products")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        given()
                .auth().oauth2(token)
                .when()
                .delete("/products/" + id)
                .then()
                .statusCode(204);

        given()
                .auth().oauth2(token)
                .when()
                .get("/products/" + id)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() {
        given()
                .when()
                .get("/products/10")
                .then()
                .statusCode(401);
    }

    @Test
    void shouldReturnUnauthorizedWithInvalidToken() {
        given()
                .auth().oauth2("invalid")
                .when()
                .get("/products/10")
                .then()
                .statusCode(403);
    }
}