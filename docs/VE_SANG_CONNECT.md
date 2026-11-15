# Menu và trang Về Sang Connect

Hoàn tất ngày 27/10/2026. Ứng dụng đã đóng gói và chạy lại ở cổng 8081.

## Đường dẫn

- Trang chủ duy nhất: http://localhost:8081/ — logo và mục Trang chủ đều dẫn về đây; giữ video và nội dung hiện có.
- Tìm chuyên gia: http://localhost:8081/trang/trangchu — giữ đúng đường dẫn danh sách dịch vụ đã xác nhận trước đó.
- Giới thiệu: http://localhost:8081/ve-sang-connect — xem được khi chưa đăng nhập.

Menu công khai có thứ tự Trang chủ → Tìm chuyên gia → Về Sang Connect. Mục hiện tại dùng aria-current="page", gạch chân nhẹ và chữ đậm vừa phải. Menu sau đăng nhập giữ các mục hiện có theo quyền, lời chào và Đăng xuất. Mobile dùng cơ chế xuống dòng hiện có, không thêm menu tương tác mới.

Trang giới thiệu dùng nội dung được cung cấp, gồm giới thiệu, lý do hình thành, người sáng lập, định hướng, hai nút điều hướng và ghi chú mô phỏng. Chỉ ghi tên Mai Phước Sang; bỏ mã lớp theo yêu cầu không công khai lớp. Avatar MS được tạo bằng chữ/CSS, không dùng ảnh chân dung. Ẩn riêng dòng MSSV/lớp vốn có trong footer khi xem trang giới thiệu; footer ở các trang khác giữ nguyên. Không thêm thành tích, đối tác hoặc số liệu.

## Tệp sửa/thêm

1. src/main/java/vn/biz04/web/Web.java: thêm GET /ve-sang-connect render about.html.
2. src/main/java/vn/biz04/web/Global.java: cung cấp currentPath để đánh dấu menu/ẩn thông tin footer ở trang giới thiệu.
3. src/main/java/vn/biz04/security/SecurityConfig.java: cho truy cập công khai route giới thiệu.
4. src/main/resources/templates/layout.html: thêm hai mục menu, đánh dấu mục hiện tại, tải CSS mới, điều kiện hiển thị dòng thông tin học tập ở footer.
5. src/main/resources/templates/about.html: trang giới thiệu mới dùng layout chung.
6. src/main/resources/static/css/about.css: chỉ áp dụng nội dung giới thiệu và menu mới.
7. src/test/java/vn/biz04/LandingPageTest.java: kiểm tra nội dung, route, trạng thái active, quyền và thông tin không được công khai.

Bản sao lưu trước sửa: `.work-backup/about-nav-20261027/`. Thư mục dự án không có Git.

## Kiểm chứng thực tế

- `mvn test`: **73 ca đạt, 0 lỗi**; 7 ca SQL Server mặc định bỏ qua, không chạy trong lượt giao diện này.
- `mvn -DskipTests package`: thành công sau khi kiểm thử; đã dừng đúng tiến trình JAR cũ và chạy lại qua chay.ps1, Windows Authentication, cổng 8081. Java PID tại thời điểm kiểm tra: 14804.
- Trình duyệt Edge: kiểm tra 27 tổ hợp (3 trang × 3 kích thước × 3 trạng thái). Các trang: `/`, `/trang/trangchu`, `/ve-sang-connect`. Kích thước: 1440, 768, 390 px. Trạng thái: chưa đăng nhập, khách hàng, quản trị viên.
- Tất cả HTTP 200; menu đúng thứ tự, mục active đúng route. Nhấn các liên kết menu đến đúng trang. Lời chào/Đăng xuất vẫn hiện sau đăng nhập; quản trị viên vẫn có mục Quản trị, người dùng thường không thấy mục này.
- Không tràn ngang, không có liên kết/nút header ra ngoài viewport, không chồng chữ hoặc nút. Đã xem ảnh desktop/mobile và mobile quản trị viên.
- Video trang chủ phát được. about.html không có ảnh người, MSSV, lớp, email cá nhân hoặc nội dung trình bày dạng trích dẫn.
- Đối chiếu SHA-256 xác nhận home.html, CSS cũ, home.js, video/poster, mọi migration và toàn bộ service nghiệp vụ/tài khoản không đổi. Không thêm hoặc sửa migration, không sửa dữ liệu người dùng trong kiểm tra trình duyệt.

Bằng chứng: `.run/about-scope-audit.json`, `.run/about-tests.log`, `.run/about-package.log`, `.run/about-qa/report.json`, `target/surefire-reports/`.

Ảnh: [desktop chưa đăng nhập](../.run/about-qa/public-about-1440.png), [mobile chưa đăng nhập](../.run/about-qa/public-about-390.png), [mobile quản trị viên](../.run/about-qa/admin-about-390.png).
