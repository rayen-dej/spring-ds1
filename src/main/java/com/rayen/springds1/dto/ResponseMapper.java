package com.rayen.springds1.dto;

import com.rayen.springds1.model.Category;
import com.rayen.springds1.model.Customer;
import com.rayen.springds1.model.CustomerOrder;
import com.rayen.springds1.model.OrderLine;
import com.rayen.springds1.model.Payment;
import com.rayen.springds1.model.Product;
import com.rayen.springds1.model.Supplier;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static CategoryResponse category(Category value) {
        return new CategoryResponse(value.getId(), value.getName(), value.getDescription());
    }

    public static SupplierResponse supplier(Supplier value) {
        return new SupplierResponse(value.getId(), value.getName(), value.getEmail(), value.getPhone(), value.getCity());
    }

    public static CustomerResponse customer(Customer value) {
        return new CustomerResponse(
                value.getId(),
                value.getCompanyName(),
                value.getEmail(),
                value.getPhone(),
                value.getAddress(),
                value.getCity(),
                value.getCreatedAt()
        );
    }

    public static ProductResponse product(Product value) {
        return new ProductResponse(
                value.getId(),
                value.getSku(),
                value.getName(),
                value.getUnitPrice(),
                value.getStockQuantity(),
                value.getReorderLevel(),
                value.isActive(),
                value.getCategory().getId(),
                value.getCategory().getName(),
                value.getSuppliers().stream()
                        .map(ResponseMapper::supplier)
                        .sorted(Comparator.comparing(SupplierResponse::id))
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    public static OrderResponse order(CustomerOrder value) {
        List<OrderLineResponse> lines = value.getLines().stream()
                .map(ResponseMapper::line)
                .toList();
        PaymentResponse payment = value.getPayment() == null ? null : payment(value.getPayment());
        return new OrderResponse(
                value.getId(),
                value.getOrderNumber(),
                value.getOrderDate(),
                value.getStatus(),
                value.getTotalAmount(),
                value.getCustomer().getId(),
                value.getCustomer().getCompanyName(),
                lines,
                payment
        );
    }

    public static OrderLineResponse line(OrderLine value) {
        return new OrderLineResponse(
                value.getId(),
                value.getProduct().getId(),
                value.getProduct().getSku(),
                value.getProduct().getName(),
                value.getQuantity(),
                value.getUnitPrice(),
                value.getLineTotal()
        );
    }

    public static PaymentResponse payment(Payment value) {
        return new PaymentResponse(
                value.getId(),
                value.getAmount(),
                value.getMethod(),
                value.getStatus(),
                value.getPaidAt(),
                value.getOrder().getId()
        );
    }
}
