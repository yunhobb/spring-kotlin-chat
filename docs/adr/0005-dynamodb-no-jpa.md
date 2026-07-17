# ADR-0005: DynamoDB 유지 · JPA 불사용

- 상태: 확정 (2026-07-17)
- 맥락: 기존 시스템이 DynamoDB 운영 중. 서버 교체이므로 저장소는 유지.

## 결정
**기존 DynamoDB 테이블을 그대로 재사용**한다(신규 스키마 설계 아님). 접근은 AWS SDK v2 **Enhanced Client(동기)**. **JPA/RDS 도입하지 않는다.**

## 근거
- 채팅 워크로드는 DynamoDB에 이상적: 쓰기 중심, `roomId`+`messageId` 단순 키 접근, 무한 수평 확장.
- 이미 운영 노하우가 있는 저장소 유지가 RDS 신규 도입보다 안전(서버 교체 원칙). 데이터 이관 자체가 없음.
- 동기 SDK의 블로킹은 가상 스레드가 흡수([ADR-0001](0001-mvc-over-webflux.md)) → 리액티브 R2DBC 불필요.

## 규칙 / 제약
- **추측 매핑 금지**: 실제 테이블 스키마(PK/SK·GSI·숨은 필드, §6-2) 확보 전까지 리포지토리는 `TODO` 스텁. 상세 [../data-model.md](../data-model.md).
- 도메인(`chat-core`)은 AWS 무의존. DynamoDB 아이템 클래스·애노테이션은 `storage-dynamo` 어댑터에만.
- 임의 쿼리(검색·통계)가 필요해지면 DynamoDB → OpenSearch/분석 파이프라인을 별도로 붙인다(테이블 스키마를 그것 때문에 바꾸지 않음).
