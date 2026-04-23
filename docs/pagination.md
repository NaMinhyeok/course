# 페이지네이션 메모

이 프로젝트의 목록 API는 외부로는 `offset`, `limit`를 받지만, 내부에서는 Spring Data `PageRequest`를 사용해 페이지 단위로 조회합니다.

## 적용 대상

- `GET /api/v1/courses`
- `GET /api/v1/enrollments`

## 어떻게 해석하는가

- `limit`은 한 번에 가져올 개수입니다.
- `offset`은 절대 row offset이라기보다, 페이지 시작 위치로 들어온다고 보고 처리합니다.
- 서버 내부에서는 `offset / limit`로 페이지 번호를 계산해 조회합니다.

예를 들어 아래 두 요청은 같은 페이지로 해석합니다.

```text
GET /api/v1/courses?offset=10&limit=10
GET /api/v1/courses?offset=15&limit=10
```

둘 다 내부적으로는 `page = 1`, `size = 10`으로 처리합니다.

## 클라이언트에서 맞춰서 보내는 방식

가장 단순한 사용 방식은 페이지 경계에 맞춰 `offset`을 넘기는 것입니다.

```text
offset = 0, 10, 20, 30, ...
limit = 10
```

또는 아래처럼 생각해도 됩니다.

```text
offset = page * limit
```

## 응답 형식

두 목록 API는 모두 아래 형태를 사용합니다.

```json
{
  "result": "SUCCESS",
  "data": {
    "content": [],
    "hasNext": false
  },
  "error": null
}
```
