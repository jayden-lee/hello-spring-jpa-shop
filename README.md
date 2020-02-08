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