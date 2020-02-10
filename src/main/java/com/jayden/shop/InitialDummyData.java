package com.jayden.shop;

import com.jayden.shop.domain.*;
import com.jayden.shop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

@Component
@RequiredArgsConstructor
public class InitialDummyData {

    private final InitialDummyDataService initialDummyDataService;

    @PostConstruct
    public void init() {
        initialDummyDataService.initDbData1();
        initialDummyDataService.initDbData2();
    }

    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitialDummyDataService {
        private final EntityManager em;

        public void initDbData1() {
            Member member = createMember("User A", "서울", "마곡", "12345");
            em.persist(member);

            Book book1 = createBook("JPA BOOK 1", 10000, 100);
            em.persist(book1);

            Book book2 = createBook("JPA BOOK 2", 20000, 100);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        public void initDbData2() {
            Member member = createMember("User B", "경기도", "양주", "54321");
            em.persist(member);

            Book book1 = createBook("SPRING BOOK 1", 20000, 200);
            em.persist(book1);

            Book book2 = createBook("SPRING BOOK 2", 25000, 300);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 20000, 5);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 25000, 10);

            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());

            Order order = Order.createOrder(member, delivery, orderItem1, orderItem2);
            em.persist(order);
        }

        private Book createBook(String name, int price, int stockQuantity) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(stockQuantity);
            return book1;
        }

        private Member createMember(String name, String city, String street, String zipCode) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(new Address(city, street, zipCode));
            return member;
        }
    }
}
