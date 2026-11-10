package com.rayen.springds1.service;

import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.dto.SupplierRequest;
import com.rayen.springds1.dto.SupplierResponse;
import com.rayen.springds1.exception.DuplicateResourceException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.Product;
import com.rayen.springds1.model.Supplier;
import com.rayen.springds1.repository.SupplierRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository repository;

    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return repository.findAll().stream().map(ResponseMapper::supplier).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        return ResponseMapper.supplier(getSupplier(id));
    }

    public SupplierResponse create(SupplierRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Supplier email already exists: " + email);
        }
        Supplier supplier = new Supplier();
        apply(supplier, request, email);
        return ResponseMapper.supplier(repository.save(supplier));
    }

    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = getSupplier(id);
        String email = normalizeEmail(request.email());
        if (!supplier.getEmail().equalsIgnoreCase(email) && repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Supplier email already exists: " + email);
        }
        apply(supplier, request, email);
        return ResponseMapper.supplier(supplier);
    }

    /**
     * Product owns the ManyToMany, so the supplier must be removed from each product first.
     * Deleting only the supplier would leave rows in product_supplier and fail.
     */
    public void delete(Long id) {
        Supplier supplier = getSupplier(id);
        for (Product product : supplier.getProducts()) {
            product.getSuppliers().remove(supplier);
        }
        repository.delete(supplier);
    }

    private Supplier getSupplier(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private void apply(Supplier supplier, SupplierRequest request, String email) {
        supplier.setName(request.name().trim());
        supplier.setEmail(email);
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setCity(trimToNull(request.city()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
