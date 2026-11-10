package com.rayen.springds1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rayen.springds1.repository.CategoryRepository;
import com.rayen.springds1.repository.CustomerOrderRepository;
import com.rayen.springds1.repository.CustomerRepository;
import com.rayen.springds1.repository.PaymentRepository;
import com.rayen.springds1.repository.ProductRepository;
import com.rayen.springds1.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Exercises the REST API through the real controllers, services and database mappings.
 * Deliberately not @Transactional: open-in-view is off, so lazy-loading bugs must be able to appear.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String ALL_TIME = "from=2020-01-01&to=2099-12-31";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PaymentRepository payments;
    @Autowired private CustomerOrderRepository orders;
    @Autowired private ProductRepository products;
    @Autowired private CustomerRepository customers;
    @Autowired private CategoryRepository categories;
    @Autowired private SupplierRepository suppliers;

    private int sequence;

    @BeforeEach
    void cleanDatabase() {
        payments.deleteAllInBatch();
        jdbc.update("delete from order_line");
        orders.deleteAllInBatch();
        jdbc.update("delete from product_supplier");
        products.deleteAllInBatch();
        customers.deleteAllInBatch();
        categories.deleteAllInBatch();
        suppliers.deleteAllInBatch();
    }

    // --- CRUD, validation and errors -----------------------------------------------------------

    @Test
    void categoryCrud() throws Exception {
        long id = category();
        send(get("/api/categories/" + id)).andExpect(status().isOk());
        send(put("/api/categories/" + id), "{\"name\":\"Renamed\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
        send(get("/api/categories")).andExpect(jsonPath("$", hasSize(1)));
        send(delete("/api/categories/" + id)).andExpect(status().isNoContent());
        send(get("/api/categories/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/categories/" + id));
    }

    @Test
    void supplierAndCustomerCrud() throws Exception {
        long supplier = supplier();
        send(put("/api/suppliers/" + supplier),
                "{\"name\":\"New\",\"email\":\"NEW@Mail.com\",\"phone\":\"\",\"city\":\"  \"}")
                .andExpect(status().isBadRequest());
        send(put("/api/suppliers/" + supplier), "{\"name\":\"New\",\"email\":\"NEW@Mail.com\",\"city\":\"  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@mail.com"))
                .andExpect(jsonPath("$.city").value(nullValue()));
        send(delete("/api/suppliers/" + supplier)).andExpect(status().isNoContent());

        long customer = customer();
        send(get("/api/customers/" + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt", notNullValue()));
        send(delete("/api/customers/" + customer)).andExpect(status().isNoContent());
    }

    @Test
    void duplicatesAreRejectedIgnoringCaseAndSpaces() throws Exception {
        send(post("/api/categories"), "{\"name\":\"Laptops\"}").andExpect(status().isCreated());
        send(post("/api/categories"), "{\"name\":\"  LAPTOPS \"}").andExpect(status().isConflict());

        long category = category();
        product(category, "ab-1", "10.00", 5, 1);
        send(post("/api/products"), productJson(category, "AB-1", "10.00", 5, 1, ""))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidBodiesReturn400() throws Exception {
        send(post("/api/categories"), "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
        send(post("/api/customers"), "{\"companyName\":\"A\",\"email\":\"nope\",\"phone\":\"1\",\"address\":\"a\",\"city\":\"c\"}")
                .andExpect(status().isBadRequest());

        long category = category();
        send(post("/api/products"), productJson(category, "A B", "10.00", 5, 1, "")).andExpect(status().isBadRequest());
        send(post("/api/products"), productJson(category, "A1", "0.00", 5, 1, "")).andExpect(status().isBadRequest());
        send(post("/api/products"), productJson(category, "A1", "10.00", -1, 1, "")).andExpect(status().isBadRequest());
        send(post("/api/products"), productJson(category, "A1", "10.00", 5, 0, "")).andExpect(status().isBadRequest());
        send(post("/api/products"), productJson(category, "A1", "10.00", 5, 1, "null")).andExpect(status().isBadRequest());
        send(post("/api/orders"), "{\"customerId\":1,\"lines\":[]}").andExpect(status().isBadRequest());
        send(post("/api/orders"), "{\"customerId\":1,\"lines\":[null]}").andExpect(status().isBadRequest());
    }

    @Test
    void malformedRequestsUseTheSameErrorFormat() throws Exception {
        send(post("/api/categories"), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing, is not valid JSON, or contains an invalid value"));
        send(patch("/api/orders/1/status"), "{\"status\":\"BANANA\"}").andExpect(status().isBadRequest());
        send(get("/api/categories/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: id"));
        send(put("/api/reports/revenue"), "{}")
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        send(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mvc.perform(post("/api/categories").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    // --- Deletion rules ------------------------------------------------------------------------

    @Test
    void referencedDataCannotBeDeleted() throws Exception {
        long category = category();
        long product = product(category, "SKU-1", "10.00", 5, 1);
        long customer = customer();
        order(customer, product, 1);

        send(delete("/api/categories/" + category)).andExpect(status().isConflict());
        send(delete("/api/products/" + product))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("active")));
        send(delete("/api/customers/" + customer)).andExpect(status().isConflict());
    }

    @Test
    void deletingASupplierUnlinksItFromProducts() throws Exception {
        long supplier = supplier();
        long keptSupplier = supplier();
        long product = idOf(send(post("/api/products"),
                productJson(category(), "LINK-1", "10.00", 5, 1, supplier + "," + keptSupplier))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.suppliers", hasSize(2))));

        send(delete("/api/suppliers/" + supplier)).andExpect(status().isNoContent());

        send(get("/api/products/" + product))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers", hasSize(1)))
                .andExpect(jsonPath("$.suppliers[0].id").value(keptSupplier));
    }

    @Test
    void productCanBeDeletedWhenNeverSold() throws Exception {
        long product = product(category(), "FREE-1", "10.00", 5, 1);
        send(delete("/api/products/" + product)).andExpect(status().isNoContent());
    }

    @Test
    void updatingAProductWithoutActiveKeepsItsState() throws Exception {
        long category = category();
        long product = product(category, "INACT-1", "10.00", 5, 1);
        String inactive = productJson(category, "INACT-1", "10.00", 5, 1, "").replace("}", ",\"active\":false}");
        send(put("/api/products/" + product), inactive)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        send(put("/api/products/" + product), productJson(category, "INACT-1", "12.00", 5, 1, ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void productSupplierJoinTableRejectsDuplicates() throws Exception {
        long supplier = supplier();
        long product = idOf(send(post("/api/products"),
                productJson(category(), "PK-1", "10.00", 5, 1, String.valueOf(supplier))));
        assertThatThrownBy(() -> jdbc.update(
                "insert into product_supplier (product_id, supplier_id) values (?, ?)", product, supplier))
                .isInstanceOf(DataAccessException.class);
    }

    // --- Orders --------------------------------------------------------------------------------

    @Test
    void orderCapturesPriceCalculatesTotalAndReducesStock() throws Exception {
        long category = category();
        long laptop = product(category, "LAP-1", "1000.50", 10, 2);
        long mouse = product(category, "MOU-1", "20.25", 10, 2);

        String body = "{\"customerId\":%d,\"lines\":[{\"productId\":%d,\"quantity\":2},{\"productId\":%d,\"quantity\":3}]}"
                .formatted(customer(), laptop, mouse);
        long order = idOf(send(post("/api/orders"), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.orderNumber", containsString("ORD-")))
                .andExpect(jsonPath("$.totalAmount").value(2061.75))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].id", notNullValue()))
                .andExpect(jsonPath("$.lines[0].lineTotal").value(2001.00)));

        assertStock(laptop, 8);
        assertStock(mouse, 7);

        send(put("/api/products/" + laptop), productJson(category, "LAP-1", "1.00", 8, 2, ""))
                .andExpect(status().isOk());
        send(get("/api/orders/" + order))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].unitPrice").value(1000.50))
                .andExpect(jsonPath("$.totalAmount").value(2061.75));
    }

    @Test
    void insufficientStockRollsBackTheWholeOrder() throws Exception {
        long category = category();
        long enough = product(category, "OK-1", "10.00", 5, 1);
        long tooFew = product(category, "NO-1", "10.00", 1, 1);

        String body = "{\"customerId\":%d,\"lines\":[{\"productId\":%d,\"quantity\":2},{\"productId\":%d,\"quantity\":9}]}"
                .formatted(customer(), enough, tooFew);
        send(post("/api/orders"), body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("requested 9, available 1")));

        assertStock(enough, 5);
        assertThat(orders.count()).isZero();
    }

    @Test
    void invalidOrderInputs() throws Exception {
        long category = category();
        long product = product(category, "DUP-1", "10.00", 5, 1);
        long customer = customer();

        String duplicate = "{\"customerId\":%d,\"lines\":[{\"productId\":%d,\"quantity\":1},{\"productId\":%d,\"quantity\":1}]}"
                .formatted(customer, product, product);
        send(post("/api/orders"), duplicate).andExpect(status().isBadRequest());
        send(post("/api/orders"), orderJson(9999, product, 1)).andExpect(status().isNotFound());
        send(post("/api/orders"), orderJson(customer, 9999, 1)).andExpect(status().isNotFound());

        String inactive = productJson(category, "DUP-1", "10.00", 5, 1, "").replace("}", ",\"active\":false}");
        send(put("/api/products/" + product), inactive).andExpect(status().isOk());
        send(post("/api/orders"), orderJson(customer, product, 1)).andExpect(status().isConflict());
    }

    @Test
    void ordersCanBeListedByCustomerWithoutLazyLoadingErrors() throws Exception {
        long customer = customer();
        order(customer, product(category(), "LIST-1", "10.00", 5, 1), 1);

        send(get("/api/orders/customer/" + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lines[0].sku").value("LIST-1"));
        send(get("/api/orders")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        send(get("/api/orders/customer/9999")).andExpect(status().isNotFound());
    }

    @Test
    void orderLinesCrudKeepsStockAndTotalConsistent() throws Exception {
        long category = category();
        long first = product(category, "L-1", "10.00", 10, 1);
        long second = product(category, "L-2", "5.00", 10, 1);
        long order = order(customer(), first, 2);

        String added = send(post("/api/orders/" + order + "/lines"), "{\"productId\":%d,\"quantity\":4}".formatted(second))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(40.00))
                .andExpect(jsonPath("$.lines[1].id", notNullValue()))
                .andReturn().getResponse().getContentAsString();
        assertStock(second, 6);
        long lineId = ((Number) JsonPath.read(added, "$.lines[1].id")).longValue();

        send(post("/api/orders/" + order + "/lines"), "{\"productId\":%d,\"quantity\":1}".formatted(second))
                .andExpect(status().isBadRequest());
        send(put("/api/orders/" + order + "/lines/" + lineId), "{\"quantity\":6}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(50.00));
        assertStock(second, 4);
        send(put("/api/orders/" + order + "/lines/" + lineId), "{\"quantity\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(25.00));
        assertStock(second, 9);
        send(put("/api/orders/" + order + "/lines/" + lineId), "{\"quantity\":99}").andExpect(status().isConflict());
        assertStock(second, 9);
        send(put("/api/orders/" + order + "/lines/9999"), "{\"quantity\":1}").andExpect(status().isNotFound());
        send(get("/api/orders/" + order + "/lines")).andExpect(jsonPath("$", hasSize(2)));

        send(delete("/api/orders/" + order + "/lines/" + lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
        assertStock(second, 10);

        String remaining = send(get("/api/orders/" + order)).andReturn().getResponse().getContentAsString();
        long lastLine = ((Number) JsonPath.read(remaining, "$.lines[0].id")).longValue();
        send(delete("/api/orders/" + order + "/lines/" + lastLine)).andExpect(status().isConflict());
    }

    @Test
    void paidOrdersCannotBeEdited() throws Exception {
        long product = product(category(), "LOCK-1", "10.00", 5, 1);
        long customer = customer();
        long order = order(customer, product, 1);
        pay(order, "10.00").andExpect(status().isCreated());

        send(post("/api/orders/" + order + "/lines"), "{\"productId\":%d,\"quantity\":1}".formatted(product))
                .andExpect(status().isConflict());
        send(put("/api/orders/" + order), "{\"customerId\":%d}".formatted(customer)).andExpect(status().isConflict());
    }

    @Test
    void pendingOrderCanChangeCustomer() throws Exception {
        long newCustomer = customer();
        long order = order(customer(), product(category(), "MOVE-1", "10.00", 5, 1), 1);
        send(put("/api/orders/" + order), "{\"customerId\":%d}".formatted(newCustomer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(newCustomer));
    }

    @Test
    void fullOrderLifecycle() throws Exception {
        long order = order(customer(), product(category(), "FLOW-1", "10.00", 5, 1), 2);

        setStatus(order, "SHIPPED").andExpect(status().isConflict());
        setStatus(order, "PAID").andExpect(status().isConflict());

        long payment = idOf(pay(order, "20").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED")));
        send(get("/api/orders/" + order))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.id").value(payment));

        setStatus(order, "DELIVERED").andExpect(status().isConflict());
        setStatus(order, "SHIPPED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        setStatus(order, "CANCELLED").andExpect(status().isConflict());
        setStatus(order, "DELIVERED").andExpect(status().isOk());
        setStatus(order, "CANCELLED").andExpect(status().isConflict());
    }

    @Test
    void cancellingAPaidOrderRefundsAndRestoresStock() throws Exception {
        long product = product(category(), "CANC-1", "10.00", 5, 1);
        long order = order(customer(), product, 2);
        long payment = idOf(pay(order, "20.00"));
        assertStock(product, 3);

        setStatus(order, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.status").value("REFUNDED"));
        assertStock(product, 5);

        send(put("/api/payments/" + payment), "{\"method\":\"CASH\"}").andExpect(status().isConflict());
        send(delete("/api/payments/" + payment)).andExpect(status().isConflict());
        send(delete("/api/orders/" + order)).andExpect(status().isConflict());
    }

    @Test
    void onlyCancelledUnpaidOrdersCanBeDeleted() throws Exception {
        long product = product(category(), "DEL-1", "10.00", 5, 1);
        long order = order(customer(), product, 2);
        send(delete("/api/orders/" + order)).andExpect(status().isConflict());

        setStatus(order, "CANCELLED").andExpect(status().isOk());
        assertStock(product, 5);
        send(delete("/api/orders/" + order)).andExpect(status().isNoContent());
        send(get("/api/orders/" + order)).andExpect(status().isNotFound());
    }

    // --- Payments ------------------------------------------------------------------------------

    @Test
    void paymentRules() throws Exception {
        long order = order(customer(), product(category(), "PAY-1", "10.00", 5, 1), 1);

        pay(order, "9.99").andExpect(status().isBadRequest());
        pay(9999, "10.00").andExpect(status().isNotFound());
        send(post("/api/payments"), "{\"orderId\":%d,\"amount\":10,\"method\":\"GOLD\"}".formatted(order))
                .andExpect(status().isBadRequest());

        long payment = idOf(pay(order, "10.0").andExpect(status().isCreated()));
        pay(order, "10.00").andExpect(status().isConflict());

        send(put("/api/payments/" + payment), "{\"method\":\"BANK_TRANSFER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("BANK_TRANSFER"));
        send(get("/api/payments")).andExpect(jsonPath("$", hasSize(1)));
        send(get("/api/payments/" + payment)).andExpect(jsonPath("$.orderId").value(order));
    }

    @Test
    void deletingAPaymentReopensAPaidOrder() throws Exception {
        long order = order(customer(), product(category(), "UNDO-1", "10.00", 5, 1), 1);
        long payment = idOf(pay(order, "10.00"));

        send(delete("/api/payments/" + payment)).andExpect(status().isNoContent());

        send(get("/api/orders/" + order))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payment").value(nullValue()));
        pay(order, "10.00").andExpect(status().isCreated());
    }

    @Test
    void paymentOfAShippedOrderCannotBeDeleted() throws Exception {
        long order = order(customer(), product(category(), "SHIP-1", "10.00", 5, 1), 1);
        long payment = idOf(pay(order, "10.00"));
        setStatus(order, "SHIPPED").andExpect(status().isOk());

        send(delete("/api/payments/" + payment)).andExpect(status().isConflict());
        send(get("/api/orders/" + order)).andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    // --- Reports -------------------------------------------------------------------------------

    @Test
    void reorderReportIncludesProductsWithoutSuppliers() throws Exception {
        long category = category();
        long supplier = supplier();
        product(category, "LOW-NOSUP", "10.00", 1, 5);
        send(post("/api/products"), productJson(category, "LOW-SUP", "10.00", 3, 5, String.valueOf(supplier)))
                .andExpect(status().isCreated());
        product(category, "AT-LEVEL", "10.00", 5, 5);
        product(category, "HIGH", "10.00", 50, 5);
        String retired = productJson(category, "RETIRED", "10.00", 0, 5, "").replace("}", ",\"active\":false}");
        send(post("/api/products"), retired).andExpect(status().isCreated());

        send(get("/api/reports/reorder-needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].sku").value("LOW-NOSUP"))
                .andExpect(jsonPath("$[0].suppliers", hasSize(0)))
                .andExpect(jsonPath("$[1].suppliers", hasSize(1)))
                .andExpect(jsonPath("$[2].sku").value("AT-LEVEL"));
    }

    @Test
    void revenueCountsCompletedPaymentsOnly() throws Exception {
        long product = product(category(), "REV-1", "10.00", 20, 1);
        long customer = customer();
        pay(order(customer, product, 3), "30.00").andExpect(status().isCreated());
        long refunded = order(customer, product, 1);
        pay(refunded, "10.00").andExpect(status().isCreated());
        setStatus(refunded, "CANCELLED").andExpect(status().isOk());
        order(customer, product, 1);

        send(get("/api/reports/revenue?" + ALL_TIME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentCount").value(1))
                .andExpect(jsonPath("$.totalRevenue").value(30.00));
    }

    @Test
    void revenueIsZeroWhenNothingMatchesAndRangeIncludesTheLastDay() throws Exception {
        send(get("/api/reports/revenue?from=2020-01-01&to=2020-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentCount").value(0))
                .andExpect(jsonPath("$.totalRevenue").value(0));

        long order = order(customer(), product(category(), "DAY-1", "10.00", 5, 1), 1);
        pay(order, "10.00").andExpect(status().isCreated());
        jdbc.update("update payment set paid_at = '2025-03-31 23:59:00'");

        send(get("/api/reports/revenue?from=2025-03-01&to=2025-03-31")).andExpect(jsonPath("$.paymentCount").value(1));
        send(get("/api/reports/revenue?from=2025-04-01&to=2025-04-30")).andExpect(jsonPath("$.paymentCount").value(0));
    }

    @Test
    void bestSellersAreRankedAndIgnoreCancelledOrders() throws Exception {
        long category = category();
        long customer = customer();
        long slow = product(category, "SLOW", "100.00", 50, 1);
        long fast = product(category, "FAST", "1.00", 50, 1);
        order(customer, slow, 2);
        order(customer, fast, 5);
        order(customer, fast, 4);
        setStatus(order(customer, slow, 40), "CANCELLED").andExpect(status().isOk());

        send(get("/api/reports/best-sellers?" + ALL_TIME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].sku").value("FAST"))
                .andExpect(jsonPath("$[0].unitsSold").value(9))
                .andExpect(jsonPath("$[0].revenue").value(9.00))
                .andExpect(jsonPath("$[1].sku").value("SLOW"))
                .andExpect(jsonPath("$[1].revenue").value(200.00));
        send(get("/api/reports/best-sellers?limit=1&" + ALL_TIME)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void overdueCustomersOnlyIncludeOldPendingOrders() throws Exception {
        long product = product(category(), "OLD-1", "10.00", 50, 1);
        long late = customer();
        long onTime = customer();
        long oldOrder = order(late, product, 2);
        long oldOrder2 = order(late, product, 1);
        order(onTime, product, 1);
        long paidOldOrder = order(onTime, product, 1);
        pay(paidOldOrder, "10.00").andExpect(status().isCreated());
        jdbc.update("update customer_order set order_date = '2025-01-01 10:00:00' where id in (?, ?, ?)",
                oldOrder, oldOrder2, paidOldOrder);

        send(get("/api/reports/overdue-customers?days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].customerId").value(late))
                .andExpect(jsonPath("$[0].overdueOrders").value(2))
                .andExpect(jsonPath("$[0].amountDue").value(30.00));
    }

    @Test
    void invalidReportParametersReturn400() throws Exception {
        send(get("/api/reports/revenue?from=2025-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing parameter: to"));
        send(get("/api/reports/revenue?from=2025-02-01&to=2025-01-01")).andExpect(status().isBadRequest());
        send(get("/api/reports/revenue?from=banana&to=2025-01-01")).andExpect(status().isBadRequest());
        send(get("/api/reports/best-sellers?limit=0&" + ALL_TIME))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("limit")));
        send(get("/api/reports/overdue-customers?days=0")).andExpect(status().isBadRequest());
    }

    // --- Helpers -------------------------------------------------------------------------------

    private ResultActions send(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long category() throws Exception {
        return idOf(send(post("/api/categories"), "{\"name\":\"Category %d\"}".formatted(++sequence))
                .andExpect(status().isCreated()));
    }

    private long supplier() throws Exception {
        return idOf(send(post("/api/suppliers"), "{\"name\":\"Supplier\",\"email\":\"supplier%d@mail.com\",\"phone\":\"+216 71 000 000\"}"
                .formatted(++sequence)).andExpect(status().isCreated()));
    }

    private long customer() throws Exception {
        String json = "{\"companyName\":\"Company\",\"email\":\"customer%d@mail.com\",\"phone\":\"+216 71 000 000\",\"address\":\"1 Main St\",\"city\":\"Tunis\"}"
                .formatted(++sequence);
        return idOf(send(post("/api/customers"), json).andExpect(status().isCreated()));
    }

    private long product(long category, String sku, String price, int stock, int reorderLevel) throws Exception {
        return idOf(send(post("/api/products"), productJson(category, sku, price, stock, reorderLevel, ""))
                .andExpect(status().isCreated()));
    }

    private String productJson(long category, String sku, String price, int stock, int reorderLevel, String supplierIds) {
        return "{\"sku\":\"%s\",\"name\":\"Product\",\"unitPrice\":%s,\"stockQuantity\":%d,\"reorderLevel\":%d,\"categoryId\":%d,\"supplierIds\":[%s]}"
                .formatted(sku, price, stock, reorderLevel, category, supplierIds);
    }

    private String orderJson(long customer, long product, int quantity) {
        return "{\"customerId\":%d,\"lines\":[{\"productId\":%d,\"quantity\":%d}]}".formatted(customer, product, quantity);
    }

    private long order(long customer, long product, int quantity) throws Exception {
        return idOf(send(post("/api/orders"), orderJson(customer, product, quantity)).andExpect(status().isCreated()));
    }

    private ResultActions pay(long order, String amount) throws Exception {
        return send(post("/api/payments"), "{\"orderId\":%d,\"amount\":%s,\"method\":\"CARD\"}".formatted(order, amount));
    }

    private ResultActions setStatus(long order, String status) throws Exception {
        return send(patch("/api/orders/" + order + "/status"), "{\"status\":\"%s\"}".formatted(status));
    }

    private void assertStock(long product, int expected) throws Exception {
        send(get("/api/products/" + product)).andExpect(jsonPath("$.stockQuantity").value(expected));
    }
}
