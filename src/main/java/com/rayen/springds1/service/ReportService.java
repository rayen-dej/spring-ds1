package com.rayen.springds1.service;

import com.rayen.springds1.dto.BestSellerResponse;
import com.rayen.springds1.dto.OverdueCustomerResponse;
import com.rayen.springds1.dto.ProductResponse;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.dto.RevenueReportResponse;
import com.rayen.springds1.dto.RevenueTotals;
import com.rayen.springds1.exception.InvalidRequestException;
import com.rayen.springds1.model.enums.OrderStatus;
import com.rayen.springds1.model.enums.PaymentStatus;
import com.rayen.springds1.repository.CustomerRepository;
import com.rayen.springds1.repository.OrderLineRepository;
import com.rayen.springds1.repository.PaymentRepository;
import com.rayen.springds1.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderLineRepository orderLineRepository;
    private final CustomerRepository customerRepository;

    public ReportService(
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            OrderLineRepository orderLineRepository,
            CustomerRepository customerRepository
    ) {
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.orderLineRepository = orderLineRepository;
        this.customerRepository = customerRepository;
    }

    public List<ProductResponse> getProductsNeedingReorder() {
        return productRepository.findProductsNeedingReorder().stream().map(ResponseMapper::product).toList();
    }

    public RevenueReportResponse getRevenue(LocalDate from, LocalDate to) {
        validateRange(from, to);
        RevenueTotals totals = paymentRepository.sumRevenue(PaymentStatus.COMPLETED, startOf(from), endOf(to));
        // SQL SUM returns null when no row matches.
        BigDecimal revenue = totals.totalRevenue() == null ? BigDecimal.ZERO : totals.totalRevenue();
        return new RevenueReportResponse(from, to, totals.paymentCount(), revenue);
    }

    public List<BestSellerResponse> getBestSellers(LocalDate from, LocalDate to, int limit) {
        validateRange(from, to);
        return orderLineRepository.findBestSellingProducts(
                OrderStatus.CANCELLED,
                startOf(from),
                endOf(to),
                PageRequest.of(0, limit)
        );
    }

    public List<OverdueCustomerResponse> getOverdueCustomers(int days) {
        return customerRepository.findOverdueCustomers(OrderStatus.PENDING, LocalDateTime.now().minusDays(days));
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
    }

    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    /** Exclusive upper bound, so that the whole of the last day is included. */
    private LocalDateTime endOf(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
