# Project Conventions

## Purpose

Tài liệu này định nghĩa các quy ước bắt buộc cho dự án trong giai đoạn migration và phát triển tiếp theo.

Mục tiêu:

- Giữ cấu trúc backend nhất quán.
- Giảm khác biệt giữa code cũ và code mới.
- Làm rõ cách sử dụng Entity, DTO, `record`, service, repository và controller.
- Giữ API contract ổn định và dễ theo dõi cho Frontend.
- Đảm bảo AI coding agent và developer cùng tuân theo một bộ quy tắc thống nhất.

Khi có xung đột giữa ví dụ trong AI skill và quy định trong tài liệu này, **quy định của dự án trong file này được ưu tiên**.

---

# 1. Scope

Các quy định trong file này áp dụng cho:

- Java backend.
- Spring Boot.
- REST API.
- JPA/Hibernate nếu được sử dụng.
- Request/Response DTO.
- Service layer.
- Repository layer.
- Controller layer.
- Các thay đổi API trong giai đoạn migration.

---

# 2. Project Authority

Trong giai đoạn migration cần tách rõ **behavioral compatibility** và **Java implementation style**.

## 2.1 Behavioral Compatibility Authority

Khi quyết định hệ thống mới phải giữ hành vi nào của backend cũ, thứ tự ưu tiên là:

```text
Executable legacy C# behavior
        ↓
Actual PostgreSQL schema
        ↓
Observed legacy API contract / OpenAPI
        ↓
API_MIGRATION.md
        ↓
Legacy migration files
        ↓
Other legacy documentation / changelog
```

Không được thay đổi behavior cũ chỉ vì convention Java/Spring mới đẹp hoặc phổ biến hơn.

## 2.2 Java Implementation Style Authority

Khi quyết định **cách triển khai Java** mà không làm thay đổi behavior/contract, thứ tự ưu tiên là:

```text
PROJECT_CONVENTIONS.md
        ↓
AGENTS.md / project instructions
        ↓
Existing Java project architecture
        ↓
Installed Java / Spring skills
        ↓
General best practices
```

Không được thay đổi convention của dự án chỉ vì một external skill sử dụng pattern khác.

Nếu convention mới xung đột với behavioral compatibility, phải giữ compatibility trước và ghi lại khác biệt/decision trong tài liệu migration.

---

# 3. Entity Rules

## 3.1 JPA Entity phải là class

JPA entity không sử dụng Java `record`.

Good:

```java
@Entity
public class User extends BaseEntity {
    // fields
}
```

Avoid:

```java
@Entity
public record User(...) {
}
```

Lý do:

- Entity có lifecycle do JPA/Hibernate quản lý.
- Entity có thể cần proxy/lazy loading.
- Entity không phải API contract.
- Entity không nên được thiết kế như immutable DTO.

---

## 3.2 BaseEntity

Các entity có các thuộc tính dùng chung phải kế thừa `BaseEntity`.

Cấu trúc chuẩn cho ZPantry phải phản ánh schema legacy dùng **UUID** và audit/soft-delete fields.

Ví dụ định hướng:

```java
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    private Instant createdAt;

    private UUID createdBy;

    private Instant updatedAt;

    private UUID updatedBy;

    private Instant deletedAt;

    private UUID deletedBy;

    private boolean isDeleted;
}
```

Các field dùng chung như:

- `id`
- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`
- `deletedAt`
- `deletedBy`
- `isDeleted`

nên được quản lý tập trung trong `BaseEntity` thay vì khai báo lặp lại ở từng entity.

Không được tự đổi UUID legacy sang `Long`/`IDENTITY`.

Cơ chế generate UUID cụ thể phải tương thích với schema và behavior hiện có. Nếu database hoặc code Java hiện tại đã có chiến lược UUID rõ ràng, giữ nhất quán với chiến lược đó trước khi thay đổi.

---

## 3.3 Entity không được expose trực tiếp qua REST API

Không trả JPA entity trực tiếp từ Controller.

Avoid:

```java
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.getById(id);
}
```

Required:

```java
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userService.getById(id);
}
```

Entity là persistence model.

DTO là API contract.

Hai khái niệm này phải được tách riêng.

---

# 4. DTO Rules

## 4.1 Request và Response DTO ưu tiên dùng record

DTO chỉ đóng vai trò data carrier nên mặc định sử dụng Java `record`.

Request:

```java
public record UserCreateRequest(
    String username,
    String email,
    String password
) {
}
```

Response:

```java
public record UserResponse(
    Long id,
    String username,
    String email
) {
}
```

Không tạo getter/setter thủ công cho DTO nếu `record` đáp ứng được yêu cầu.

### Migration compatibility

Việc dùng `record` không được làm thay đổi JSON/API contract legacy.

Không được tự ý:

- rename field;
- remove field;
- merge field;
- split field;
- đổi data type;
- thay đổi nullability/required behavior;

chỉ để DTO Java trông sạch hơn.

Nếu cần thay đổi contract, phải cập nhật `API_MIGRATION.md`.

---

## 4.2 Naming

Không dùng các tên mơ hồ như:

```text
UserDTO
UserData
UserModel
UserInfo
```

Ưu tiên tên thể hiện chính xác direction và use case.

Examples:

```text
UserCreateRequest
UserUpdateRequest
UserPatchRequest
UserResponse
UserSummaryResponse
UserDetailResponse
LoginRequest
LoginResponse
```

---

## 4.3 Không dùng một DTO cho mọi operation

Không dùng một class duy nhất cho create, update và response nếu contract của chúng khác nhau.

Avoid:

```text
UserDTO
```

dùng đồng thời cho:

```text
POST
PUT
PATCH
GET
```

Preferred:

```text
UserCreateRequest
UserUpdateRequest
UserPatchRequest
UserResponse
```

---

# 5. Mapping Rules

Mapping giữa Entity và DTO phải rõ ràng.

Flow chuẩn:

```text
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Entity / Domain
    ↓
Repository

Repository
    ↓
Entity / Domain
    ↓
Service
    ↓
Response DTO
    ↓
Controller
```

Controller không tự chứa business mapping phức tạp.

Nếu mapping đơn giản có thể dùng method riêng.

Nếu mapping lớn hoặc lặp lại nhiều nơi, tạo mapper riêng.

Example:

```text
UserMapper
OrderMapper
ProductMapper
```

---

# 6. Controller Rules

Controller chỉ chịu trách nhiệm về HTTP boundary.

Controller có thể:

- nhận request;
- validate input;
- đọc path/query parameters;
- gọi service;
- chuyển kết quả thành HTTP response;
- trả status code phù hợp.

Controller không nên:

- chứa business logic;
- gọi repository trực tiếp;
- chứa query phức tạp;
- xử lý transaction;
- expose entity.

Required dependency direction:

```text
Controller
    ↓
Service
    ↓
Repository
```

Avoid:

```text
Controller
    ↓
Repository
```

---

# 7. Service Rules

Service chịu trách nhiệm cho business logic và orchestration.

Naming:

```text
UserService
OrderService
ProductService
```

Method nên mô tả intent nghiệp vụ.

Preferred:

```java
findById(...)
createUser(...)
updateUser(...)
deleteUser(...)
activateUser(...)
cancelOrder(...)
```

Tránh method mơ hồ:

```java
process(...)
handle(...)
execute(...)
doSomething(...)
```

trừ khi domain thực sự yêu cầu tên đó.

---

# 8. Repository Rules

Repository chỉ chịu trách nhiệm truy cập persistence.

Ví dụ:

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

Không đặt business logic trong repository.

Custom query phải:

- có tên rõ ràng;
- phản ánh chính xác điều kiện query;
- tránh duplicate query nếu đã có API phù hợp từ Spring Data.

---

# 9. REST Endpoint Conventions

Endpoint phải đại diện cho resource, không đại diện cho method Java.

Preferred:

```text
GET    /api/v1/users
GET    /api/v1/users/{id}
POST   /api/v1/users
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}
```

Avoid:

```text
GET  /getAllUsers
GET  /getUserById
POST /createUser
POST /updateUser
GET  /deleteUser
```

## 9.1 Migration Compatibility Exception

Các REST convention trong section này là mặc định cho **API mới**.

Trong giai đoạn behavioral migration, không được tự động đổi API legacy chỉ để phù hợp REST convention mới.

Không tự động đổi:

- `/api/users` thành `/api/v1/users`;
- `PUT` thành `PATCH`;
- field name;
- response wrapper;
- status code;
- pagination format;
- authentication/authorization requirement.

Chỉ thay đổi public API khi thay đổi đó được ghi nhận rõ trong `API_MIGRATION.md` và migration task cho phép thực hiện.

---

# 10. Resource Naming

Endpoint path:

- dùng noun;
- ưu tiên plural resource name;
- dùng lowercase;
- không dùng Java method name;
- không dùng camelCase trong path;
- giữ format nhất quán trong toàn bộ API.

Preferred:

```text
/users
/orders
/products
/payment-methods
```

Avoid:

```text
/getUsers
/userList
/paymentMethods
```

Nested resource chỉ dùng khi relationship thật sự quan trọng:

```text
/users/{userId}/orders
/orders/{orderId}/items
```

---

# 11. HTTP Method Semantics

Sử dụng HTTP method theo semantics:

```text
GET
→ đọc resource

POST
→ tạo resource / operation không idempotent

PUT
→ thay thế toàn bộ representation

PATCH
→ cập nhật một phần

DELETE
→ xóa resource
```

Không dùng `GET` để thực hiện mutation.

---

# 12. HTTP Status Codes

Ưu tiên:

```text
200 OK
→ request thành công và có response body

201 Created
→ tạo resource thành công

204 No Content
→ thành công nhưng không cần response body

400 Bad Request
→ malformed/invalid request

401 Unauthorized
→ chưa authentication hợp lệ

403 Forbidden
→ đã authentication nhưng không có quyền

404 Not Found
→ resource không tồn tại

409 Conflict
→ conflict với state hiện tại

500 Internal Server Error
→ unexpected server error
```

Không trả `200 OK` cho mọi tình huống.

---

# 13. Validation Rules

Validation phải thực hiện tại API boundary khi có thể.

Example:

```java
public record UserCreateRequest(

    @NotBlank
    String username,

    @NotBlank
    @Email
    String email,

    @NotBlank
    @Size(min = 8)
    String password

) {
}
```

Controller:

```java
@PostMapping
public ResponseEntity<UserResponse> create(
    @Valid @RequestBody UserCreateRequest request
) {
    ...
}
```

Business validation vẫn thuộc service nếu rule phụ thuộc domain/state/database.

---

# 14. Error Handling

Không xử lý exception riêng lẻ và lặp lại trong từng controller nếu có thể xử lý tập trung.

Ưu tiên:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

API error phải có contract ổn định.

Không expose:

- stack trace;
- database exception;
- internal implementation details;
- secret;
- credential.

---

# 15. Package / Layer Naming

Ưu tiên naming thống nhất:

```text
controller
service
repository
entity
dto
mapper
exception
config
security
```

Nếu project dùng feature-based architecture thì phải giữ nhất quán theo convention hiện hữu của project.

Không tự ý chuyển toàn bộ package structure chỉ vì external skill đề xuất architecture khác.

---

# 16. API Versioning

Nếu project đã sử dụng URI versioning, phải giữ nhất quán:

```text
/api/v1/...
```

Không trộn lẫn tùy ý:

```text
/api/v1/users
/users
/api/users
/v2/orders
```

trong cùng một public API surface nếu không có lý do migration rõ ràng.

---

# 17. Migration Phase Rule

Trong giai đoạn migration, mọi thay đổi public API contract phải được ghi vào:

```text
API_MIGRATION.md
```

Một API contract change bao gồm:

- endpoint path;
- HTTP method;
- path variable;
- query parameter;
- request field;
- request DTO;
- response field;
- response DTO;
- data type;
- status code;
- error response;
- pagination;
- sorting;
- filtering;
- authentication requirement;
- authorization requirement;
- deprecated endpoint;
- removed endpoint.

---

# 18. Mandatory Frontend Migration Documentation

Nếu Backend thay đổi API từ:

```text
GET /api/getUserById?id=10
```

sang:

```text
GET /api/v1/users/10
```

thì task chưa hoàn thành cho đến khi `API_MIGRATION.md` ghi rõ:

```text
OLD
GET /api/getUserById?id={id}

NEW
GET /api/v1/users/{id}

FRONTEND ACTION
Replace the old endpoint with the new resource path.
Move id from query parameter to path variable.
```

Tương tự, nếu đổi:

```text
priceValue
```

thành:

```text
price
```

thì tài liệu phải ghi rõ:

```text
Frontend replace:

product.priceValue
→
product.price
```

---

# 19. Migration Completion Rule

Không coi API migration task là hoàn thành nếu:

- backend code đã đổi;
- test đã pass;

nhưng:

```text
API_MIGRATION.md
```

chưa được cập nhật.

Code và migration documentation phải nằm trong cùng task/change.

---

# 20. Backward Compatibility

Khi thay đổi breaking API, phải xác định rõ:

```text
OLD API:
ACTIVE | DEPRECATED | REMOVED
```

Nếu endpoint cũ được giữ tạm thời, phải ghi:

- deprecation status;
- replacement endpoint;
- target removal date nếu đã xác định.

Không xóa silent một endpoint đang được Frontend sử dụng.

---

# 21. AI Agent Rules

AI coding agent phải đọc file này trước khi:

- tạo Entity;
- tạo DTO;
- tạo Controller;
- tạo REST endpoint;
- refactor REST API;
- sửa request/response;
- đổi API contract;
- migration code cũ sang architecture mới.

AI agent không được tự ý:

- đổi naming convention;
- thay `record` bằng mutable DTO class nếu không có lý do kỹ thuật;
- trả Entity trực tiếp từ Controller;
- bỏ `BaseEntity` cho entity mới nếu entity thuộc nhóm sử dụng shared persistence fields;
- đổi endpoint mà không cập nhật migration documentation;
- áp dụng sample từ external skill nếu sample xung đột với convention của project.

---

# 22. Required Architecture Pattern

Default request flow:

```text
HTTP Request
    ↓
Controller
    ↓
Request DTO (record)
    ↓
Service
    ↓
Entity / Domain
    ↓
Repository
    ↓
Database
```

Default response flow:

```text
Database
    ↓
Repository
    ↓
Entity / Domain
    ↓
Service
    ↓
Response DTO (record)
    ↓
Controller
    ↓
HTTP Response
```

---

# 23. Definition of Done

Một backend task chỉ được coi là hoàn thành khi phù hợp:

```text
[ ] Naming tuân thủ project convention
[ ] Entity không bị expose trực tiếp
[ ] Entity dùng class
[ ] Shared entity fields dùng BaseEntity khi phù hợp
[ ] Request/Response DTO dùng record khi phù hợp
[ ] Controller không chứa business logic
[ ] Controller không gọi Repository trực tiếp
[ ] HTTP method đúng semantics
[ ] Endpoint dùng resource naming
[ ] Validation nằm đúng layer
[ ] Error handling nhất quán
[ ] Test liên quan đã được cập nhật
[ ] API_MIGRATION.md đã cập nhật nếu contract thay đổi
[ ] Frontend replacement instruction đã được ghi nếu có breaking change
[ ] docs/MIGRATION_STATUS.md đã cập nhật
[ ] docs/CHANGELOG.md đã cập nhật cho thay đổi đáng kể
[ ] docs/KNOWN_RISKS.md đã cập nhật nếu phát hiện inconsistency/risk mới
[ ] docs/ARCHITECTURE_DECISIONS.md đã cập nhật nếu có architecture decision mới
```

---

# 24. Final Rule

Trong giai đoạn migration:

> Không chỉ làm code chạy được. Mọi thay đổi phải đưa code mới về cùng một convention rõ ràng và phải giữ cho Frontend biết chính xác API contract đã thay đổi như thế nào.

Khi có nghi ngờ giữa:

```text
external skill example
```

và:

```text
project convention
```

thì:

```text
PROJECT_CONVENTIONS.md
```

là nguồn ưu tiên.
