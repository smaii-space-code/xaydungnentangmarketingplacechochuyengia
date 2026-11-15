> Cập nhật kiểm tra trên máy người dùng: 30 kiểm thử đạt, 2 kiểm thử SQL Server chuyên biệt bỏ qua; JAR chạy SQL Server thật và đã kiểm tra Chrome ba kích thước. Xem [CAP_NHAT_SANG_CONNECT.md](CAP_NHAT_SANG_CONNECT.md). Nội dung bên dưới lưu kết quả bản đầu.

# Kết quả kiểm tra bản đầu Sang Connect

Ngày bàn giao: 27/10/2026. Môi trường tạo mã: Java 17, Maven 3.9.11, compiler Eclipse tùy chọn. Máy sinh viên theo ảnh: Java 25, Maven 3.10.0, MSSQLSERVER Running. Hai môi trường không phải cùng một máy.

- Đã biên dịch, đóng gói JAR và chạy 28 ca kiểm thử nghiệp vụ/trang bằng H2: 28 đạt, 0 lỗi, 0 thất bại trong lần build cuối trước bàn giao.
- Đã kiểm tra luồng HTTP trên JAR chạy thật với cơ sở dữ liệu H2 dành riêng cho kiểm tra: đăng nhập bằng phiên/CSRF, yêu cầu → đề xuất → hợp đồng, hoàn tất hai mốc, giữ kín đánh giá đầu tiên, công khai sau đánh giá thứ hai và mở trang quản trị đều đạt. Chưa kiểm tra trực quan bằng trình duyệt trên máy sinh viên.
- Có 2 ca SQL Server thật, mặc định bỏ qua nếu chưa chỉ định môi trường và database BIZ04_Test. Chưa chạy được hai ca đó trong môi trường tạo mã này.
- Chưa xác nhận tên máy chủ, kiểu Authentication, cổng TCP và quyền đăng nhập SQL Server trên máy sinh viên. Chưa tuyên bố migration, chỉ mục có điều kiện hoặc khóa đồng thời SQL Server đã đạt.
- Giữ nguyên dữ liệu ERD cơ sở 12 bảng/106 thuộc tính/21 FK. Các thay đổi vật lý của V2 được ghi riêng, không sửa âm thầm ERD gốc.
- Không đưa target, mật khẩu, proxy của môi trường tạo mã hoặc ảnh chụp máy sinh viên vào ZIP mã nguồn.

Trước khi coi bản chạy SQL Server hoàn tất: cấu hình máy chủ/tài khoản, chạy Flyway và ứng dụng, xác minh chỉ mục, chạy kịch bản người dùng và các ca SQL Server riêng. Nếu có lỗi, sửa dựa trên lỗi thật, không đổi sang H2 để gọi là đã chạy SQL Server.

Nguồn tương thích đã tra cứu:
- Spring Boot 3.5.16 hỗ trợ Java 17 đến 25: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- JDBC 13.4 hỗ trợ JDK 25: https://learn.microsoft.com/en-us/sql/connect/jdbc/system-requirements-for-the-jdbc-driver


