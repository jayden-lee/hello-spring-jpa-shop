package com.jayden.shop.api;

import com.jayden.shop.domain.*;
import com.jayden.shop.repository.OrderRepository;
import com.jayden.shop.repository.order.OrderFlatDto;
import com.jayden.shop.repository.order.OrderItemQueryDto;
import com.jayden.shop.repository.order.OrderQueryDto;
import com.jayden.shop.repository.order.OrderQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * 엔티티를 직접 노출 V1 API, 지연 로딩을 강제 초기화
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }

        return all;
    }

    @GetMapping("/api/v2/orders")
    public Result ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> data = orders.stream().map(OrderDto::new)
            .collect(toList());

        return new Result(data, data.size());
    }

    @GetMapping("/api/v3/orders")
    public Result ordersV3() {
        List<Order> orders = orderRepository.findAllWithItems();
        List<OrderDto> data = orders.stream().map(OrderDto::new)
            .collect(toList());

        return new Result(data, data.size());
    }

    @GetMapping("/api/v4/orders")
    public Result ordersV4(@RequestParam(value = "offset", defaultValue = "0") int offset,
                           @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAll(offset, limit);
        List<OrderDto> data = orders.stream().map(OrderDto::new)
            .collect(toList());

        return new Result(data, data.size());
    }

    @GetMapping("/api/v5/orders")
    public Result ordersV5() {
        List<OrderQueryDto> data = orderQueryRepository.findOrderQueryDtos();
        return new Result(data, data.size());
    }

    @GetMapping("/api/v6/orders")
    public Result ordersV6() {
        List<OrderQueryDto> data = orderQueryRepository.findAll();
        return new Result(data, data.size());
    }

    @GetMapping("/api/v7/orders")
    public Result ordersV7() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDtoFlat();

        List<OrderQueryDto> data = flats.stream()
            .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                    o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                    o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
            )).entrySet().stream()
            .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                e.getKey().getAddress(), e.getValue()))
            .collect(toList());

        return new Result(data, data.size());
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
        private int totalCount;
    }

    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream().map(OrderItemDto::new)
                .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;
        private int orderPrice;
        private int count;

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

}
