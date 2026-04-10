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

@SpringBootTest
public class ProductServiceConcurrencyTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  private Long targetProductId;

  @BeforeEach
  void setUp() {
    // テスト用のデータを1件作成
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

    // 開始タイミングを合わせるためのラッチ
    CountDownLatch startLatch = new CountDownLatch(1);
    // 全終了を待つためのラッチ
    CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    for (int i = 0; i < numberOfThreads; i++) {
      final String newName = "商品更新-" + i;
      executorService.execute(() -> {
        try {
          startLatch.await(); // 全スレッドがここで待機

          ProductRequest request = new ProductRequest();
          request.setName(newName);
          request.setPrice(2000);

          productService.updateProduct(targetProductId, request);
          successCount.incrementAndGet();
        } catch (ObjectOptimisticLockingFailureException e) {
          // 楽観的ロックエラーを検知
          failureCount.incrementAndGet();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown(); // 2つのスレッドを一斉に動かす
    doneLatch.await(); // 両方の処理が終わるまで待つ

    // 検証：1人は成功し、もう1人は楽観的ロック失敗（version不整合）になるはず
    assertEquals(1, successCount.get(), "一方は成功すること");
    assertEquals(1, failureCount.get(), "もう一方はロック失敗すること");

    executorService.shutdown();
  }
}