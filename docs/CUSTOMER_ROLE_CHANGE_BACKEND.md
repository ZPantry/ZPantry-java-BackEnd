# User Profile (Health & Diet) Module

Tài liệu này mô tả các API và Entity mới được tạo ra để quản lý thông tin cá nhân mở rộng của người dùng (sức khoẻ, chế độ ăn, dị ứng,...).

## 1. Entity và Database
Entity `UserProfileEntity` ánh xạ với bảng `user_profiles` được tạo ở file migration `V3`. Bảng này có mối quan hệ 1-1 với bảng `users` thông qua trường `user_id`.

**Các trường dữ liệu:**
- `userId` (UUID) - Khoá ngoại trỏ tới `users.id`
- `age` (Integer) - Tuổi
- `gender` (String) - Giới tính
- `height` (BigDecimal) - Chiều cao (cm)
- `weight` (BigDecimal) - Cân nặng (kg)
- `goal` (String) - Mục tiêu (VD: Giảm cân, Tăng cơ)
- `dietPreference` (String) - Chế độ ăn và khẩu vị (VD: Keto, Ăn chay)
- `allergies` (String) - Dị ứng thực phẩm (VD: Đậu phộng, Hải sản)

## 2. API Endpoints

### 2.1 Xem thông tin Profile
**Endpoint**: `GET /api/users/{userId}/profile`
**Mô tả**: Lấy thông tin profile mở rộng của một người dùng.
**Xác thực**: Yêu cầu người dùng đang đăng nhập chính là chủ sở hữu (owner) của profile này.

**Response (Thành công - 200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "f1111111-1111-1111-1111-111111111111",
    "userId": "22222222-2222-2222-2222-222222222222",
    "age": 25,
    "gender": "Female",
    "height": 165.5,
    "weight": 55.0,
    "goal": "Weight loss",
    "dietPreference": "Keto",
    "allergies": "Peanuts"
  }
}
```

### 2.2 Cập nhật (hoặc Tạo mới) Profile
**Endpoint**: `PUT /api/users/{userId}/profile`
**Mô tả**: Cập nhật thông tin profile. Nếu profile chưa tồn tại trong database, hệ thống sẽ tự động tạo mới (Upsert).
**Xác thực**: Yêu cầu người dùng đang đăng nhập chính là chủ sở hữu (owner) của profile này.

**Request Body:**
```json
{
  "age": 25,
  "gender": "Female",
  "height": 165.5,
  "weight": 55.0,
  "goal": "Weight loss",
  "dietPreference": "Keto",
  "allergies": "Peanuts"
}
```

**Response (Thành công - 200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "f1111111-1111-1111-1111-111111111111",
    "userId": "22222222-2222-2222-2222-222222222222",
    "age": 25,
    "gender": "Female",
    "height": 165.5,
    "weight": 55.0,
    "goal": "Weight loss",
    "dietPreference": "Keto",
    "allergies": "Peanuts"
  }
}
```

## 3. Danh sách các file code được thêm vào:
- **Domain**: `com.zpantry.user.domain.UserProfileEntity`
- **Persistence**: `com.zpantry.user.persistence.UserProfileRepository`
- **DTOs**: `com.zpantry.user.api.UserProfileResponse`, `com.zpantry.user.api.UserProfileUpdateRequest`
- **Service**: `com.zpantry.user.service.UserProfileService`
- **Controller**: `com.zpantry.user.api.UserProfileController`
