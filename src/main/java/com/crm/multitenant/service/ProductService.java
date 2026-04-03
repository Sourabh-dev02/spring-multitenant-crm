package com.crm.multitenant.service;

import com.crm.multitenant.domain.Product;
import com.crm.multitenant.dto.ProductRequest;
import com.crm.multitenant.dto.ProductResponse;
import com.crm.multitenant.exception.ConflictException;
import com.crm.multitenant.exception.ResourceNotFoundException;
import com.crm.multitenant.mapper.ProductMapper;
import com.crm.multitenant.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper     productMapper;

    // product lists are a prime caching candidate - they're read far more than written
    @Cacheable(value = "products", key = "'available:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ProductResponse> listAvailable(Pageable pageable) {
        return productRepository.findAvailable(pageable).map(productMapper::toResponse);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("SKU already exists: " + request.getSku());
        }
        Product product = productMapper.toEntity(request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("SKU already exists: " + request.getSku());
        }

        productMapper.updateEntity(request, product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void softDelete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setDeleted(true);
        product.setActive(false);
        productRepository.save(product);
        log.debug("Soft-deleted product id={} sku={}", id, product.getSku());
    }
}
