package com.rayen.springds1.service;

import com.rayen.springds1.dto.ProductRequest;
import com.rayen.springds1.dto.ProductResponse;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.exception.BusinessRuleException;
import com.rayen.springds1.exception.DuplicateResourceException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.Category;
import com.rayen.springds1.model.Product;
import com.rayen.springds1.model.Supplier;
import com.rayen.springds1.repository.CategoryRepository;
import com.rayen.springds1.repository.OrderLineRepository;
import com.rayen.springds1.repository.ProductRepository;
import com.rayen.springds1.repository.SupplierRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final OrderLineRepository orderLineRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            OrderLineRepository orderLineRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.orderLineRepository = orderLineRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(ResponseMapper::product).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ResponseMapper.product(getProduct(id));
    }

    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + sku);
        }
        Product product = new Product();
        product.setActive(request.active() == null || request.active());
        apply(product, request, sku);
        return ResponseMapper.product(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        String sku = normalizeSku(request.sku());
        if (!product.getSku().equalsIgnoreCase(sku) && productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + sku);
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        apply(product, request, sku);
        return ResponseMapper.product(product);
    }

    /** A product that was sold must stay in the database so that past orders remain correct. */
    public void delete(Long id) {
        Product product = getProduct(id);
        if (orderLineRepository.existsByProductId(id)) {
            throw new BusinessRuleException("Product appears in orders and cannot be deleted. Set active to false instead");
        }
        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void apply(Product product, ProductRequest request, String sku) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setUnitPrice(request.unitPrice());
        product.setStockQuantity(request.stockQuantity());
        product.setReorderLevel(request.reorderLevel());
        product.setCategory(category);
        product.setSuppliers(resolveSuppliers(request.supplierIds()));
    }

    private Set<Supplier> resolveSuppliers(Set<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Supplier> suppliers = supplierRepository.findAllById(supplierIds);
        if (suppliers.size() != supplierIds.size()) {
            throw new ResourceNotFoundException("One or more suppliers were not found");
        }
        return new HashSet<>(suppliers);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }
}
