# Hello Spring JPA Shop
> 인프런 실전 스프링 부트와 JPA 활용 1,2 강의를 학습하면서 정리하는 Repo

## Gradle 의존관계 보기
```
./gradlew dependencies --configuration compileClasspath
```

## Docker MySQL 설치
```
docker run -d --name test_mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=admin007! mysql:5.7 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

## 엔티티 설계시 주의점
1. 가급적 <code>Setter</code>를 사용하지 말자
2. 모든 연관관계는 <code>지연로딩(Lazy)</code>으로 설정하자
    - Fetch Join, 엔티티 그래프로 최적화
    - XToOne 매핑은 기본 패치 전략이 Eager 기본값
3. 컬렉션은 필드에서 초기화 하자
4. 테이블, 컬럼명 생성 전략

## 준영속 엔티티 수정 방법
준영속 엔티티는 영속성 컨택스트가 관리하지 않는 엔티티를 말한다

- 변경 감지(Dirty Checking) 기능 사용
```java
@Transactional
public void update(Long id, int newPrice) {
    A a = em.find(A.class, id); // 식별자로 엔티티 조회
    a.changePrice(newPrice); // 데이터 수정
}
```

- 병합(Merge) 사용
병합은 준영속 상태의 엔티티를 영속 상태로 변경할 때 사용하는 기능
```java
@Transactional
public void udpate(Item newItem) {
    Item mergeItem = em.merge(newItem);
}
```

> 변경 감지 기능을 사용하면 원하는 속성만 선택해서 변경할 수 있지만, 병합을 사용하면 모든 속성이 변경된다.
병합시 값이 없으면 <code>null</code>로 업데이트 될 위험이 있다. 병합은 모든 필드를 교체하기 때문이다.

<hr/>

# API 개발 고급

## 지연로딩과 조회 성능 최적화

### Jackson-DataType-Hibernate5 Module
지연로딩으로 설정된 엔티티를 API에서 내려줄 때, Jackson이 데이터를 변환하다가 알 수 없는 타입이라는 에러가 발생한다. 이 때,
<code>jackson-datatype-hibernate5</code> 모듈을 추가하고 Bean 등록을 하게 되면, 지연로딩으로 설정된 프록시 엔티티를 null 값으로
설정한다.

이 모듈을 사용하기 전에 앞서서 엔티티를 API에서 DTO 형태로 사용했다는 점은 큰 문제점이다. API에서 클라이언트로 내려주는 
데이터는 DTO로 변경해서 내려보내야 한다. 엔티티를 사용하게 되면, 장애가 발생하거나 사이드 이펙트 문제를 일으키기 때문이다.

#### Maven
```
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5</artifactId>
    <version>2.10.2</version>
</dependency>
```

#### Gradle
```
implementation 'com.fasterxml.jackson.datatype:jackson-datatype-hibernate5'
```

#### Hibernate5Module Bean 등록
```java
@Bean
Hibernate5Module hibernate5Module() {
    Hibernate5Module hibernate5Module = new Hibernate5Module();
    // 강제로 지연로딩 해서 엔티티 정보를 가져오도록 설정 한다.
    hibernate5Module.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true);
    return hibernate5Module;
}
```

### N+1 문제
첫 번째 목록을 조회하는 1번의 쿼리와 그 외 정보를 가져오기 위해서 N번의 쿼리가 실행되는 문제를 <b>N+1</b> 문제라고 한다.

아래의 코드 경우에는 주문 조회 1번, 회원 조회 N번, 배송 조회 N번이 발생한다. 주문 조회 결과 값이 2라고 가정한다면, 최악의 경우 <code>1(주문) + 2(회원) + 2(배송) = 5</code> 총 5번의 쿼리가 실행된다.

앞에서 최악의 경우라고 말한 것은 지연로딩은 기본적으로 영속성 컨텍스트에서 먼저 조회하므로 이미 조회된 경우에 쿼리를 생략한다. 만약 가져온 회원 정보가 이미 있는 경우 데이터베이스에 회원 정보를 조회하는 쿼리를 실행하지 않지만, 최악의 경우에는 매번 데이터베이스에 회원 정보를 조회하는 쿼리를 실행할 수 있다. 

```java
@GetMapping("/api/v2/simple-orders")
public List<SimpleOrderDto> ordersV2() {
    // Order 조회 SQL 1번 실행 -> 2개 주문서 반환
    List<Order> orders = orderRepository.findAllByString(new OrderSearch());
    
    // 2개의 주문서가 있으므로 2번 루프를 반복함
    // 하나의 SimpleOrderDto 생성할 때마다 Member, Delivery 쿼리 2번 실행
    return orders.stream().map(SimpleOrderDto::new)
        .collect(toList());
}
```

> 이러한 문제가 발생한 이유는 Order 엔티티와 연관 관계가 있는 Member, Delivery 엔티티가 <code>FetchType.LAZY</code>으로 설정되어 있기 때문이다. 그렇다면
<code>FetchType.EAGER</code>로 변경하면 문제를 임시적으로 해결한 것처럼 보일 수 있다. 가장 좋은 해결 방법으로는 Fetch 조인을 사용해서 튜닝을 해야 한다.

### Fetch Join
SQL 조인을 활용해서 연관된 엔티티를 한번의 SQL로 모두 조회하는 기능이다.

```java
public List<Order> findAll() {
    return em.createQuery(
        "select o from Order o" +
            "join fetch o.member m" +
            "join fetch o.delivery d",Order.class
    ).getResultList();
}
```

실제 수행되는 쿼리는 다음과 같다.

```sql
select
    order0_.order_id as order_id1_6_0_,
    member1_.member_id as member_i1_4_1_,
    delivery2_.delivery_id as delivery1_2_2_,
    order0_.delivery_id as delivery4_6_0_,
    order0_.member_id as member_i5_6_0_,
    order0_.order_date as order_da2_6_0_,
    order0_.status as status3_6_0_,
    member1_.city as city2_4_1_,
    member1_.street as street3_4_1_,
    member1_.zip_code as zip_code4_4_1_,
    member1_.name as name5_4_1_,
    delivery2_.city as city2_2_2_,
    delivery2_.street as street3_2_2_,
    delivery2_.zip_code as zip_code4_2_2_,
    delivery2_.status as status5_2_2_ 
from
    orders order0_ 
inner join
    member member1_ 
        on order0_.member_id=member1_.member_id 
inner join
    delivery delivery2_ 
        on order0_.delivery_id=delivery2_.delivery_id
```

### JPA에서 DTO로 바로 조회
쿼리 실행 결과 값을 받을 DTO 클래스 <code>OrderSimpleQueryDto</code>를 생성한다. 

```java
@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }
}
```

생성자를 이용해서 쿼리 결과 값을 받는다. 이 메서드의 단점은 API에서 사용할 필드 값들이 노출되고 포함되어 있다는 점이다. 따라서 API 변경에 따라 DTO 클래스와 쿼리를 수정해야 한다. 복잡한 쿼리를 조회하는 경우에는 새로운 <code>Repository</code> 클래스를 생성해서 그 곳에 메서드를 모아 놓는 것이 유지보수 또는 코드 파악에 도움이 된다.

```java
public List<OrderSimpleQueryDto> findOrderDtos() {
    return em.createQuery(
        "select new com.jayden.shop.repository.order.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
            "from Order o " +
            "join o.member m " +
            "join o.delivery d", OrderSimpleQueryDto.class
    ).getResultList();
}
```

실제 수행되는 쿼리는 다음과 같다.

```sql
select
    order0_.order_id as col_0_0_,
    member1_.name as col_1_0_,
    order0_.order_date as col_2_0_,
    order0_.status as col_3_0_,
    delivery2_.city as col_4_0_,
    delivery2_.street as col_4_1_,
    delivery2_.zip_code as col_4_2_ 
from
    orders order0_ 
inner join
    member member1_ 
        on order0_.member_id=member1_.member_id 
inner join
    delivery delivery2_ 
        on order0_.delivery_id=delivery2_.delivery_id
```

### 쿼리 방식 선택 순서
1. 엔티티를 DTO로 변환한 정보를 내려준다.
2. 성능 최적화가 필요하면 페치 조인을 사용한다.
3. DTO로 직접 조회하는 방법을 통해 조회하는 컬럼 개수를 줄인다.
4. JPA가 제공하는 네이티브 SQL 또는 JDBC Template을 사용해서 SQL을 직접 사용한다.

## 컬렉션 조회 최적화

### Fetch Join
OneToMany 관계의 엔티티들을 <code>Fetch Join</code> 하게 되면, 결과값으로는 One에 해당하는 엔티티 정보가 Many 개수만큼 나오게 된다.

<code>orders</code>와 <code>order_item</code> 두 테이블을 조인하면 4개의 레코드가 출력된다. 현재 데이터는 orders 테이블에는 행 2개, order_items 테이블에는 행 4개가 있다.

<img width="800" alt="order_join_orderitem" src="https://user-images.githubusercontent.com/43853352/74144425-0bee3580-4c40-11ea-8208-4b9cb6d6ae16.png">

다음과 같이 작성한 코드를 실행하면 반환값으로 Order 엔티티 4개를 갖고 있는 리스트가 반환된다. 실제 필요한 Order 엔티티는 2개임에도 불구하고 중복을 포함해서 4개의 엔티티가 반환된다.

```java
public List<Order> findAllWithItems() {
    return em.createQuery(
        "select o from Order o " +
            "join fetch o.member m " +
            "join fetch o.delivery d " +
            "join fetch o.orderItems oi " +
            "join fetch oi.item i", Order.class
    ).getResultList();
}
```

이를 해결하기 위해서 queryString 값에 <code>distinct</code> 키워드를 추가한다. jpa에는 distinct 키워드가 있으면 중복 엔티티를 제거해주고, DB에 날리는 쿼리에도 distinct 문장을 추가해준다.

```java
public List<Order> findAllWithItems() {
    return em.createQuery(
        "select distinct o from Order o " +
            "join fetch o.member m " +
            "join fetch o.delivery d " +
            "join fetch o.orderItems oi " +
            "join fetch oi.item i", Order.class
    ).getResultList();
}
```

> 일대다 관계에서 Fetch Join 하게 되면 페이징 쿼리가 불가능한 단점이 있다. 페이징 쿼리를 날리기 위해서 offset, limit을 설정하면 하이버네이트는 경고 로그를 남기고 메모리에서 페이징을 한다. 실제 DB에 실행되는 쿼리에도 페이징 정보가 담기지 않고 모든 정보를 가져온다.

### 페이징과 한계 돌파
컬렉션은 지연 로딩으로 조회해서 페이징 처리를 한다. 지연 로딩 최적화와 페이징을 위해서 <code>hibernate.default_batch_fetch_size</code> 또는 <code>@BatchSize</code>를 적용한다.

주문과 주문 목록은 일대다 관계이므로 지연 로딩을 통해 조회한다. 객체 그래프로 탐색하는 순간에 주문 목록에 해당하는 쿼리를 실행하는데, 위에서 말한 옵션을 활성화하면 주문 목록 아이템을 정해진 크기만큼 한번에 조회하는 쿼리를 실행한다.

```sql
# order_item 조회
select
    orderitems0_.order_id as order_id5_5_1_,
    orderitems0_.order_item_id as order_it1_5_1_,
    orderitems0_.order_item_id as order_it1_5_0_,
    orderitems0_.count as count2_5_0_,
    orderitems0_.item_id as item_id4_5_0_,
    orderitems0_.order_id as order_id5_5_0_,
    orderitems0_.order_price as order_pr3_5_0_ 
from
    order_item orderitems0_ 
where
    orderitems0_.order_id in (
        ?, ?
    );

# item 조회
select
    item0_.item_id as item_id2_3_0_,
    item0_.name as name3_3_0_,
    item0_.price as price4_3_0_,
    item0_.stock_quantity as stock_qu5_3_0_,
    item0_.actor as actor6_3_0_,
    item0_.director as director7_3_0_,
    item0_.artist as artist8_3_0_,
    item0_.etc as etc9_3_0_,
    item0_.author as author10_3_0_,
    item0_.isbn as isbn11_3_0_,
    item0_.dtype as dtype1_3_0_ 
from
    item item0_ 
where
    item0_.item_id in (
        ?, ?, ?, ?
    )
```

> xToOne 관계는 <code>Fetch Join</code>으로 조회 최적화를 적용하고, xToMany 관계에서는 지연 로딩과 <code>hibernate.default_batch_fetch_size</code> 또는 <code>@BatchSize</code>를 적용해서 조회 성능을 최적화한다.