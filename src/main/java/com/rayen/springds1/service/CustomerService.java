package com.rayen.springds1.service;

import com.rayen.springds1.dto.CustomerRequest;
import com.rayen.springds1.dto.CustomerResponse;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.exception.BusinessRuleException;
import com.rayen.springds1.exception.DuplicateResourceException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.Customer;
import com.rayen.springds1.repository.CustomerOrderRepository;
import com.rayen.springds1.repository.CustomerRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository repository;
    private final CustomerOrderRepository orderRepository;

    public CustomerService(CustomerRepository repository, CustomerOrderRepository orderRepository) {
        this.repository = repository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return repository.findAll().stream().map(ResponseMapper::customer).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return ResponseMapper.customer(getCustomer(id));
    }

    public CustomerResponse create(CustomerRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Customer email already exists: " + email);
        }
        Customer customer = new Customer();
        apply(customer, request, email);
        return ResponseMapper.customer(repository.save(customer));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomer(id);
        String email = normalizeEmail(request.email());
        if (!customer.getEmail().equalsIgnoreCase(email) && repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Customer email already exists: " + email);
        }
        apply(customer, request, email);
        return ResponseMapper.customer(customer);
    }

    public void delete(Long id) {
        Customer customer = getCustomer(id);
        if (orderRepository.existsByCustomerId(id)) {
            throw new BusinessRuleException("Customer has orders and cannot be deleted");
        }
        repository.delete(customer);
    }

    private Customer getCustomer(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private void apply(Customer customer, CustomerRequest request, String email) {
        customer.setCompanyName(request.companyName().trim());
        customer.setEmail(email);
        customer.setPhone(request.phone().trim());
        customer.setAddress(request.address().trim());
        customer.setCity(request.city().trim());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
