package com.rayen.springds1.controller;

import com.rayen.springds1.dto.OrderCreateRequest;
import com.rayen.springds1.dto.OrderLineRequest;
import com.rayen.springds1.dto.OrderLineResponse;
import com.rayen.springds1.dto.OrderLineUpdateRequest;
import com.rayen.springds1.dto.OrderResponse;
import com.rayen.springds1.dto.OrderStatusRequest;
import com.rayen.springds1.dto.OrderUpdateRequest;
import com.rayen.springds1.service.CustomerOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class CustomerOrderController {

    private final CustomerOrderService service;

    public CustomerOrderController(CustomerOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> findByCustomer(@PathVariable Long customerId) {
        return service.findByCustomerId(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@PathVariable Long id, @Valid @RequestBody OrderUpdateRequest request) {
        return service.updateCustomer(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}/lines")
    public List<OrderLineResponse> findLines(@PathVariable Long id) {
        return service.findLines(id);
    }

    @PostMapping("/{id}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse addLine(@PathVariable Long id, @Valid @RequestBody OrderLineRequest request) {
        return service.addLine(id, request);
    }

    @PutMapping("/{id}/lines/{lineId}")
    public OrderResponse updateLine(
            @PathVariable Long id,
            @PathVariable Long lineId,
            @Valid @RequestBody OrderLineUpdateRequest request
    ) {
        return service.updateLine(id, lineId, request);
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    public OrderResponse deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        return service.deleteLine(id, lineId);
    }
}
