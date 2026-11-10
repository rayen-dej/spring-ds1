package com.rayen.springds1.service;

import com.rayen.springds1.dto.CategoryRequest;
import com.rayen.springds1.dto.CategoryResponse;
import com.rayen.springds1.dto.ResponseMapper;
import com.rayen.springds1.exception.BusinessRuleException;
import com.rayen.springds1.exception.DuplicateResourceException;
import com.rayen.springds1.exception.ResourceNotFoundException;
import com.rayen.springds1.model.Category;
import com.rayen.springds1.repository.CategoryRepository;
import com.rayen.springds1.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(ResponseMapper::category).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return ResponseMapper.category(getCategory(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category name already exists: " + name);
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        return ResponseMapper.category(repository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name) && repository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category name already exists: " + name);
        }
        category.setName(name);
        category.setDescription(request.description());
        return ResponseMapper.category(category);
    }

    public void delete(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Category still contains products and cannot be deleted");
        }
        repository.delete(category);
    }

    private Category getCategory(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
