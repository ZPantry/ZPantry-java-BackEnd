# API Migration Guide

## Purpose

File này là nguồn theo dõi chính thức cho mọi thay đổi API trong giai đoạn migration của dự án.

Mục tiêu là giúp Frontend xác định nhanh:

- endpoint nào đã thay đổi;
- endpoint cũ là gì;
- endpoint mới là gì;
- HTTP method có thay đổi hay không;
- request parameters/body có thay đổi hay không;
- response structure có thay đổi hay không;
- Frontend cần sửa gì;
- endpoint cũ còn hoạt động hay đã bị loại bỏ.

---

## Mandatory Rule

Trong giai đoạn migration:

> Bất kỳ thay đổi nào ảnh hưởng đến API contract đều PHẢI được ghi vào file này.

Bao gồm nhưng không giới hạn:

- đổi endpoint path;
- đổi HTTP method;
- đổi path variable;
- đổi query parameter;
- đổi request body;
- đổi request DTO;
- đổi response DTO;
- đổi tên field;
- thêm/xóa field;
- đổi kiểu dữ liệu;
- đổi status code;
- đổi authentication/authorization requirement;
- đổi pagination;
- đổi sorting/filtering;
- đổi error response;
- endpoint bị deprecated;
- endpoint bị xóa;
- endpoint được thay thế bằng endpoint khác.

Không được merge thay đổi API nếu file này chưa được cập nhật.

---

# Migration Entries

Mỗi thay đổi API phải dùng format sau.

## [MIG-XXX] Short description

**Status:** `PENDING | MIGRATED | DEPRECATED | REMOVED`

**Breaking Change:** `YES | NO`

**Date:** `YYYY-MM-DD`

### Old API

```http
METHOD /old/path
```

### New API

```http
METHOD /new/path
```

### Changes

- Change 1
- Change 2
- Change 3

### Old Request

```json
{}
```

### New Request

```json
{}
```

### Old Response

```json
{}
```

### New Response

```json
{}
```

### Frontend Action Required

Frontend cần:

1. Replace old endpoint with new endpoint.
2. Update request mapping if necessary.
3. Update response mapping if necessary.
4. Update error/status-code handling if necessary.

### Compatibility

Old endpoint:

```text
STILL AVAILABLE | DEPRECATED | REMOVED
```

Removal target:

```text
YYYY-MM-DD | N/A
```

### Notes

Additional migration information if needed.

---

# Example

## [MIG-001] Replace legacy user detail endpoint

**Status:** `MIGRATED`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### Old API

```http
GET /api/getUserById?id={id}
```

### New API

```http
GET /api/v1/users/{id}
```

### Changes

- Endpoint changed from action-based naming to REST resource naming.
- `id` changed from query parameter to path variable.
- API version `/v1` was added.
- Response now uses `UserResponse` instead of returning the persistence entity directly.

### Old Request

```http
GET /api/getUserById?id=10
```

### New Request

```http
GET /api/v1/users/10
```

### Old Response

```json
{
  "id": 10,
  "username": "khang",
  "email": "example@email.com",
  "passwordHash": "..."
}
```

### New Response

```json
{
  "id": 10,
  "username": "khang",
  "email": "example@email.com"
}
```

### Frontend Action Required

Frontend cần:

1. Replace:

```text
/api/getUserById?id={id}
```

with:

```text
/api/v1/users/{id}
```

2. Pass `id` as part of the URL path instead of query parameters.

3. Remove any dependency on `passwordHash`.

### Compatibility

Old endpoint:

```text
DEPRECATED
```

Removal target:

```text
2026-10-01
```

---

# Example — Request DTO Change

## [MIG-002] Rename registration request fields

**Status:** `PENDING`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### API

```http
POST /api/v1/users
```

### Old Request

```json
{
  "userName": "khang",
  "mail": "example@email.com",
  "pwd": "password"
}
```

### New Request

```json
{
  "username": "khang",
  "email": "example@email.com",
  "password": "password"
}
```

### Changes

```text
userName -> username
mail     -> email
pwd      -> password
```

### Frontend Action Required

Replace request object fields:

```text
userName → username
mail     → email
pwd      → password
```

### Compatibility

Old request fields:

```text
REMOVED
```

---

# Example — Response Field Change

## [MIG-003] Rename product price response field

**Status:** `MIGRATED`

**Breaking Change:** `YES`

**Date:** `2026-09-16`

### API

```http
GET /api/v1/products/{id}
```

### Old Response

```json
{
  "id": 1,
  "name": "Keyboard",
  "priceValue": 500000
}
```

### New Response

```json
{
  "id": 1,
  "name": "Keyboard",
  "price": 500000
}
```

### Frontend Action Required

Replace:

```text
product.priceValue
```

with:

```text
product.price
```

---

# Migration Status Index

| ID | API | Breaking | Status | Frontend Action |
|---|---|---:|---|---|
| MIG-001 | `GET /api/v1/users/{id}` | Yes | Migrated | Replace old user endpoint |
| MIG-002 | `POST /api/v1/users` | Yes | Pending | Rename request fields |
| MIG-003 | `GET /api/v1/products/{id}` | Yes | Migrated | Rename response field |

---

# Rules for Backend Developers and AI Agents

Before changing an API:

1. Inspect the existing API contract.
2. Determine whether the change affects Frontend.
3. If the contract changes, create or update a migration entry in this file.
4. Assign the next `MIG-XXX` identifier.
5. Document both the old and new contract.
6. Clearly describe the exact Frontend replacement required.
7. State whether the change is breaking.
8. State whether the old contract remains temporarily compatible.

After changing an API:

1. Update the migration entry with the final implementation.
2. Verify example requests and responses against the implementation.
3. Mark the correct migration status.
4. Never mark an API as `MIGRATED` if the documented contract does not match the implementation.

---

# Definition of an API Contract Change

Treat a modification as an API contract change whenever a consumer may need to change its code.

For example:

```text
Controller method renamed internally
→ NOT necessarily an API contract change

Service implementation changed
→ NOT an API contract change

/api/users/{id}
→ /api/v1/users/{id}
→ API contract change

email
→ emailAddress
→ API contract change

HTTP 200
→ HTTP 204
→ API contract change

response List<User>
→ Page<UserResponse>
→ API contract change
```

When uncertain, document the change.