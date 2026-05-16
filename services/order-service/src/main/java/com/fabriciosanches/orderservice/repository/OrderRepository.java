package com.fabriciosanches.orderservice.repository;

import com.fabriciosanches.orderservice.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

