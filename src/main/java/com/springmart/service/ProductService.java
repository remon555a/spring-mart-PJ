package com.springmart.service;

import com.springmart.dto.ProductRequest;
import com.springmart.dto.ProductResponse;
import com.springmart.entity.Inventory;
import com.springmart.entity.Product;
import com.springmart.exception.ResourceNotFoundException;
import com.springmart.repository.InventoryRepository;
import com.springmart.repository.OrderDetailRepository;
import com.springmart.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderDetailRepository orderDetailRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository,
            OrderDetailRepository orderDetailRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getVersion()))
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品が見つかりません: " + id));
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product = productRepository.save(product);

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setStockQuantity(request.getInitialStock());
        inventoryRepository.save(inventory);

        return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品が見つかりません: " + id));

        if (request.getVersion() == null) {
            throw new IllegalArgumentException("更新には version が必須です。");
        }

        if (!product.getVersion().equals(request.getVersion())) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Product.class, id);
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        product = productRepository.save(product);

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getVersion());
    }

    @Transactional
    public void deleteProduct(Long id) {

        if (id == null) {
            throw new IllegalArgumentException("IDがnullです");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品が見つかりません: " + id));

        if (orderDetailRepository.existsByProduct_Id(id)) {
            throw new IllegalStateException("注文履歴がある商品は削除できません。");
        }

        inventoryRepository.deleteById(id);
        if (product != null) {
            productRepository.delete(product);
        }
    }
}
