# Google Authentication Integration

Tài liệu này ghi lại quá trình cấu hình và thay đổi liên quan đến tính năng Đăng nhập bằng Google cho dự án ZPantry (bao gồm Frontend Mobile/Web và Backend Java Spring Boot).

## 1. Ứng dụng Frontend (ZPantry-Mobile)

### Cập nhật phương thức đăng nhập
Đã cấu hình đồng thời 2 phương thức để đảm bảo Google Sign-In hoạt động trơn tru trên mọi nền tảng:
- **Native (Android/iOS)**: Tích hợp thư viện @react-native-google-signin/google-signin. Thư viện này tương tác với SDK native của thiết bị, cho trải nghiệm nhanh, bảo mật và chính thống.
- **Web**: Tích hợp expo-auth-session/providers/google qua cơ chế chuyển hướng trình duyệt (do phương thức Native không hỗ trợ Web).

### Chi tiết thay đổi trong LoginScreen.tsx
- **Khởi tạo cấu hình (Configuration)**:
  - Khai báo Platform từ 
eact-native để chia nhánh logic.
  - Đối với Native: Gọi GoogleSignin.configure trong useEffect. Sử dụng webClientId (Lấy từ biến môi trường của Web, bắt buộc phải dùng mã Web Client ID để có thể lấy được ID Token trên Android) và bật offlineAccess: true.
  - Đối với Web: Dùng Google.useAuthRequest(googleConfig) của expo-auth-session.
- **Thực thi đăng nhập (handleGoogleLogin)**:
  - Với Web: Gọi promptGoogleAsync() để lấy xác thực OAuth2. Kết quả trả về được theo dõi bằng một useEffect để trích xuất idToken.
  - Với Native: Chạy GoogleSignin.hasPlayServices() và GoogleSignin.signIn() lấy trực tiếp userInfo.data.idToken.
- **Trích xuất idToken**:
  - Token hiện đã lấy thành công và được console.log() ra. 
  - Code đã được dọn sẵn chỗ để gọi API kết nối đến Backend: etch('http://<ip>:8080/api/auth/google', ...) gửi idToken xuống cho Spring Boot xác thực. Tạm thời ứng dụng đang gọi mock signIn(...) nội bộ để giao diện không bị kẹt.

## 2. Dịch vụ Backend (ZPantry-java-BackEnd)

### Thay đổi cấu hình
- Thêm biến môi trường chứa Google Client ID vào src/main/resources/application.properties để server có dữ liệu đối chiếu token:
  `properties
  # Google client Id
  google.client.id=
  `

### Các bước tiếp theo cần triển khai (Next Steps)
1. Thêm dependency xác thực Google (như google-api-client) vào pom.xml.
2. Tạo mới endpoint (VD: /api/auth/google) tại AuthenticationController.java.
3. Xây dựng service sử dụng GoogleIdTokenVerifier để kiểm tra độ chính xác và an toàn của idToken nhận được từ Frontend.
4. Trích xuất thông tin người dùng (email, tên, ...). Tra cứu xem email đã có trong database chưa. Nếu chưa thì tiến hành tự động tạo tài khoản (đăng ký), nếu đã có thì tiến hành cấp quyền.
5. Trả về cho Frontend JWT nội bộ của ZPantry để bắt đầu phiên đăng nhập.
6. Thay thế code mock ở Frontend bằng logic gọi API thật đến endpoint vừa tạo.
