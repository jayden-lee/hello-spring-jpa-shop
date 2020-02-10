package com.jayden.shop.api;

import com.jayden.shop.domain.Address;
import com.jayden.shop.domain.Order;
import com.jayden.shop.domain.OrderSearch;
import com.jayden.shop.domain.OrderStatus;
import com.jayden.shop.repository.OrderRepository;
import com.jayden.shop.repository.order.OrderSimpleQueryDto;
import com.jayden.shop.repository.order.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * xToOne(ManyToOne, OneToOne) 성능 최적화
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * 순환 참조하면서 무한 루프에 빠지게 되는 API
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        return orderRepository.findAllByString(new OrderSearch());
    }

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // Order 조회 SQL 1번 실행 -> 2개 주문서 반환
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // 2개의 주문서가 있으므로 2번 루프를 반복함
        // 하나의 SimpleOrderDto 생성할 때마다 Member, Delivery 쿼리 2번 실행
        return orders.stream().map(SimpleOrderDto::new)
            .collect(toList());
    }

    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream().map(SimpleOrderDto::new)
            .collect(toList());
    }

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
        }
    }

}
