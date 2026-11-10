package com.rayen.springds1.controller;

import com.rayen.springds1.dto.BestSellerResponse;
import com.rayen.springds1.dto.OverdueCustomerResponse;
import com.rayen.springds1.dto.ProductResponse;
import com.rayen.springds1.dto.RevenueReportResponse;
import com.rayen.springds1.service.ReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/reorder-needed")
    public List<ProductResponse> reorderNeeded() {
        return service.getProductsNeedingReorder();
    }

    @GetMapping("/revenue")
    public RevenueReportResponse revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.getRevenue(from, to);
    }

    @GetMapping("/best-sellers")
    public List<BestSellerResponse> bestSellers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return service.getBestSellers(from, to, limit);
    }

    @GetMapping("/overdue-customers")
    public List<OverdueCustomerResponse> overdueCustomers(
            @RequestParam(defaultValue = "30") @Min(1) @Max(3650) int days
    ) {
        return service.getOverdueCustomers(days);
    }
}
