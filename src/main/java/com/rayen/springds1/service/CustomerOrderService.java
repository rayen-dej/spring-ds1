package com.rayen.springds1.service;

import com.rayen.springds1.dto.OrderCreateRequest;
import com.rayen.springds1.dto.OrderLineRequest;
import com.rayen.springds1.dto.OrderLineResponse;
import com.rayen.springds1.dto.OrderLineUpdateRequest;
import com.rayen.springds1.dto.OrderResponse;
import com.rayen.springds1.dto.OrderStatusRequest;
import com.rayen.springds1.dto.OrderUpdateRequest;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.exception.BusinessRuleException;
import com.rayen.springds1.exception.InvalidRequestException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.Customer;
import com.rayen.springds1.model.CustomerOrder;
import com.rayen.springds1.model.OrderLine;
import com.rayen.springds1.model.Product;
import com.rayen.springds1.model.enums.OrderStatus;
import com.rayen.springds1.model.enums.PaymentStatus;
import com.rayen.springds1.repository.CustomerOrderRepository;
import com.rayen.springds1.repository.CustomerRepository;
import com.rayen.springds1.repository.OrderLineRepository;
import com.rayen.springds1.repository.ProductRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final OrderLineRepository lineRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CustomerOrderService(
            CustomerOrderRepository orderRepository,
            OrderLineRepository lineRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.lineRepository = lineRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(ResponseMapper::order).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return ResponseMapper.order(getOrder(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomerId(Long customerId) {
        getCustomer(customerId);
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(ResponseMapper::order)
                .toList();
    }

    /** All-or-nothing: if one line fails, the transaction rolls back every stock change. */
    public OrderResponse create(OrderCreateRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setCustomer(getCustomer(request.customerId()));

        Set<Long> productIds = new HashSet<>();
        for (OrderLineRequest lineRequest : request.lines()) {
            if (!productIds.add(lineRequest.productId())) {
                throw new InvalidRequestException("A product can appear only once in an order");
            }
            order.addLine(createLine(lineRequest));
        }
        order.setTotalAmount(order.calculateTotal());
        return ResponseMapper.order(orderRepository.save(order));
    }

    public OrderResponse updateCustomer(Long id, OrderUpdateRequest request) {
        CustomerOrder order = getPendingOrder(id);
        order.setCustomer(getCustomer(request.customerId()));
        return ResponseMapper.order(order);
    }

    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        CustomerOrder order = getOrder(id);
        OrderStatus target = request.status();
        if (!isTransitionAllowed(order.getStatus(), target)) {
            throw new BusinessRuleException("Order status cannot change from " + order.getStatus() + " to " + target);
        }
        if (target == OrderStatus.CANCELLED) {
            cancel(order);
        }
        order.setStatus(target);
        return ResponseMapper.order(order);
    }

    /** Only a cancelled, unpaid order has no stock or financial consequence left. */
    public void delete(Long id) {
        CustomerOrder order = getOrder(id);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Only cancelled orders can be deleted");
        }
        if (order.getPayment() != null) {
            throw new BusinessRuleException("An order with a refunded payment is kept as a financial record");
        }
        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public List<OrderLineResponse> findLines(Long orderId) {
        return getOrder(orderId).getLines().stream().map(ResponseMapper::line).toList();
    }

    public OrderResponse addLine(Long orderId, OrderLineRequest request) {
        CustomerOrder order = getPendingOrder(orderId);
        boolean alreadyInOrder = order.getLines().stream()
                .anyMatch(line -> line.getProduct().getId().equals(request.productId()));
        if (alreadyInOrder) {
            throw new InvalidRequestException("A product can appear only once in an order");
        }
        OrderLine line = createLine(request);
        order.addLine(line);
        lineRepository.save(line);
        order.setTotalAmount(order.calculateTotal());
        return ResponseMapper.order(order);
    }

    /** The unit price stays the one captured when the line was created. */
    public OrderResponse updateLine(Long orderId, Long lineId, OrderLineUpdateRequest request) {
        CustomerOrder order = getPendingOrder(orderId);
        OrderLine line = getLine(order, lineId);
        int difference = request.quantity() - line.getQuantity();
        if (difference > 0) {
            reserveStock(line.getProduct(), difference);
        } else {
            releaseStock(line.getProduct(), -difference);
        }
        line.setQuantity(request.quantity());
        order.setTotalAmount(order.calculateTotal());
        return ResponseMapper.order(order);
    }

    public OrderResponse deleteLine(Long orderId, Long lineId) {
        CustomerOrder order = getPendingOrder(orderId);
        OrderLine line = getLine(order, lineId);
        if (order.getLines().size() == 1) {
            throw new BusinessRuleException("An order must keep at least one line. Cancel the order instead");
        }
        releaseStock(line.getProduct(), line.getQuantity());
        order.getLines().remove(line);
        order.setTotalAmount(order.calculateTotal());
        return ResponseMapper.order(order);
    }

    /** PAID is not reachable from here: an order becomes paid only by creating a payment. */
    private boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    private void cancel(CustomerOrder order) {
        for (OrderLine line : order.getLines()) {
            releaseStock(line.getProduct(), line.getQuantity());
        }
        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.REFUNDED);
        }
    }

    private OrderLine createLine(OrderLineRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        reserveStock(product, request.quantity());
        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(request.quantity());
        line.setUnitPrice(product.getUnitPrice());
        return line;
    }

    private void reserveStock(Product product, int quantity) {
        if (!product.isActive()) {
            throw new BusinessRuleException("Inactive product cannot be ordered: " + product.getSku());
        }
        if (product.getStockQuantity() < quantity) {
            throw new BusinessRuleException("Insufficient stock for product " + product.getSku()
                    + ": requested " + quantity + ", available " + product.getStockQuantity());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
    }

    private void releaseStock(Product product, int quantity) {
        product.setStockQuantity(product.getStockQuantity() + quantity);
    }

    private CustomerOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private CustomerOrder getPendingOrder(Long id) {
        CustomerOrder order = getOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only pending orders can be modified");
        }
        return order;
    }

    private Customer getCustomer(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private OrderLine getLine(CustomerOrder order, Long lineId) {
        return order.getLines().stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order line not found: " + lineId));
    }

    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (orderRepository.existsByOrderNumberIgnoreCase(orderNumber));
        return orderNumber;
    }
}
