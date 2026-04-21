package com.springmart.service;

import com.springmart.dto.ProductRequest;
import com.springmart.entity.Product;
import com.springmart.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ProductServiceConcurrencyTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  private Long targetProductId;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    Product product = new Product();
    product.setName("初期商品");
    product.setPrice(1000);
    product = productRepository.save(product);
    targetProductId = product.getId();
  }

  @Test
  void testUpdateProductConcurrency() throws InterruptedException {
    int numberOfThreads = 2;
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    for (int i = 0; i < numberOfThreads; i++) {
      executorService.execute(() -> {
        try {
          startLatch.await();

          ProductRequest request = new ProductRequest();
          request.setName("並行更新");
          request.setPrice(2000);
          request.setVersion(0);

          productService.updateProduct(targetProductId, request);
          successCount.incrementAndGet();
        } catch (ObjectOptimisticLockingFailureException e) {
          failureCount.incrementAndGet();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    doneLatch.await();

    assertEquals(1, successCount.get(), "一方は成功すること");
    assertEquals(1, failureCount.get(), "もう一方は楽観的ロックで失敗すること");

    executorService.shutdown();
  }

  @Test
  void testUpdateProduct_OldVersion_ShouldThrowException() {
    ProductRequest request = new ProductRequest();
    request.setName("古いバージョンで更新");
    request.setPrice(2000);
    request.setVersion(999);

    assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
      productService.updateProduct(targetProductId, request);
    });
  }
}