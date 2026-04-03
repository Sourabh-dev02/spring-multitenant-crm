package com.crm.multitenant.controller;

import com.crm.multitenant.repository.CustomerRepository;
import com.crm.multitenant.repository.OrderRepository;
import com.crm.multitenant.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quick-and-dirty benchmark endpoint so you can see the timing difference between
 * a naive loop (N+1) vs fetch join / projection.
 *
 * Not something you'd ship in production as-is, but useful for the POC demo:
 * GET /api/benchmark?runs=5
 *
 * It measures:
 * 1. customerRepository.findAll() - triggers lazy order loading -> N+1
 * 2. customerRepository.findAllWithOrders() - fetch join, 1 query
 * 3. customerRepository.findAllSummaries() - projection, fewer columns
 */
@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final CustomerRepository customerRepository;
    private final OrderRepository    orderRepository;
    private final ProductRepository  productRepository;

    @GetMapping
    public Map<String, Object> run(@RequestParam(defaultValue = "3") int runs) {
        Map<String, Object> results = new LinkedHashMap<>();

        results.put("runs", runs);
        results.put("naiveN+1_ms",       measure(runs, this::naiveNPlusOne));
        results.put("fetchJoin_ms",       measure(runs, this::withFetchJoin));
        results.put("projection_ms",      measure(runs, this::withProjection));
        results.put("orderSummary_ms",    measure(runs, this::orderSummary));

        log.info("Benchmark complete: {}", results);
        return results;
    }

    // deliberately bad - iterates customers and accesses orders, causing N+1
    private void naiveNPlusOne() {
        customerRepository.findAll().forEach(c -> {
            int size = c.getOrders().size(); // each call fires a SELECT
        });
    }

    private void withFetchJoin() {
        customerRepository.findAllWithOrders(); // single query
    }

    private void withProjection() {
        customerRepository.findAllSummaries(PageRequest.of(0, 100)); // 6 columns, no joins
    }

    private void orderSummary() {
        orderRepository.findAllSummaries(PageRequest.of(0, 100)); // projection with join
    }

    private Map<String, Object> measure(int runs, Runnable action) {
        long[] timings = new long[runs];
        for (int i = 0; i < runs; i++) {
            long start = System.currentTimeMillis();
            action.run();
            timings[i] = System.currentTimeMillis() - start;
        }

        long total = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (long t : timings) {
            total += t;
            if (t < min) min = t;
            if (t > max) max = t;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("avg", total / runs);
        stats.put("min", min);
        stats.put("max", max);
        return stats;
    }
}
