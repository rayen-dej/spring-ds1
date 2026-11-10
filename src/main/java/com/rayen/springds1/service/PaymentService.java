package com.rayen.springds1.service;

import com.rayen.springds1.dto.PaymentCreateRequest;
import com.rayen.springds1.dto.PaymentResponse;
import com.rayen.springds1.dto.PaymentUpdateRequest;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.exception.BusinessRuleException;
import com.rayen.springds1.exception.InvalidRequestException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.CustomerOrder;
import com.rayen.springds1.model.Payment;
import com.rayen.springds1.model.enums.OrderStatus;
import com.rayen.springds1.model.enums.PaymentStatus;
import com.rayen.springds1.repository.CustomerOrderRepository;
import com.rayen.springds1.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerOrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, CustomerOrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream().map(ResponseMapper::payment).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        return ResponseMapper.payment(getPayment(id));
    }

    public PaymentResponse create(PaymentCreateRequest request) {
        CustomerOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.orderId()));
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new BusinessRuleException("This order already has a payment");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Payment is allowed only for pending orders");
        }
        // compareTo, not equals: BigDecimal.equals treats 100.0 and 100.00 as different.
        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new InvalidRequestException("Payment amount must equal the order total: " + order.getTotalAmount());
        }
        Payment payment = new Payment();
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setOrder(order);
        order.setPayment(payment);
        order.setStatus(OrderStatus.PAID);
        return ResponseMapper.payment(paymentRepository.save(payment));
    }

    public PaymentResponse updateMethod(Long id, PaymentUpdateRequest request) {
        Payment payment = getPayment(id);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessRuleException("A refunded payment cannot be modified");
        }
        payment.setMethod(request.method());
        return ResponseMapper.payment(payment);
    }

    /** Corrects a payment entered by mistake, which is only safe before the order ships. */
    public void delete(Long id) {
        Payment payment = getPayment(id);
        CustomerOrder order = payment.getOrder();
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessRuleException("A payment can be deleted only while its order is PAID");
        }
        order.setPayment(null);
        order.setStatus(OrderStatus.PENDING);
        paymentRepository.delete(payment);
    }

    private Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }
}
