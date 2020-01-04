# Hello Spring JPA Shop
> 인프런 실전 스프링 부트와 JPA 활용 강의를 학습하면서 정리하는 Repo

## Gradle 의존관계 보기
```
./gradlew dependencies --configuration compileClasspath
```

## 엔티티 설계시 주의점
1. 가급적 <code>Setter</code>를 사용하지 말자
2. 모든 연관관계는 <code>지연로딩(Lazy)</code>으로 설정하자
    - Fetch Join, 엔티티 그래프로 최적화
    - XToOne 매핑은 기본 패치 전략이 Eager 기본값
3. 컬렉션은 필드에서 초기화 하자
4. 테이블, 컬럼명 생성 전략
