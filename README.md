Sang Connect — Nền tảng marketing/kết nối chuyên gia và khách hàng

Đồ án môn học · Sinh viên: Mai Phước Sang · MSSV: 1250080159

Sang Connect (mã dự án BIZ-04) là website marketplace cho phép khách hàng tìm và thuê chuyên gia: đăng yêu cầu, nhận đề xuất, thương lượng, ký hợp đồng điện tử, ký quỹ theo từng mốc, nghiệm thu, giải quyết tranh chấp và đánh giá hai chiều.

⚠️ Thanh toán chỉ là mô phỏng. Hệ thống dùng tiền mô phỏng (VND nguyên, BigDecimal), không kết nối ví, ngân hàng hay cổng thanh toán thật.

1. Công nghệ sử dụng
Thành phần	Công nghệ
Ngôn ngữ / Framework	Java 25, Spring Boot 3.5.16
Giao diện	Thymeleaf, HTML/CSS/JS
Truy cập dữ liệu	Spring Data JPA / Hibernate
Cơ sở dữ liệu	Microsoft SQL Server (chạy thật)
Quản lý phiên bản CSDL	Flyway (migration V1 → V9)
Bảo mật	Spring Security (đăng nhập, phân quyền theo vai trò, OTP qua email)
Gửi email	Spring Mail (SMTP, ví dụ Gmail)
Build	Maven
Kiểm thử	JUnit 5, Spring Boot Test, H2 (chỉ ở phạm vi test)
2. Chức năng chính
Tài khoản & vai trò: đăng ký khách hàng / chuyên gia, xác thực OTP qua email, hồ sơ, CCCD (mã hóa), menu theo vai trò, quản trị viên xét duyệt chuyên gia.
Khám phá: danh mục chuyên gia và dịch vụ, phân trang, xem chi tiết.
Yêu cầu – đề xuất: khách hàng đăng yêu cầu, chuyên gia gửi đề xuất, khách chấp thuận hoặc từ chối.
Đề nghị thuê: khách hàng đề nghị trực tiếp một chuyên gia, điều chỉnh qua nhiều phiên bản có lịch sử.
Hợp đồng điện tử: kiểm tra quyền theo từng hợp đồng, hai bên cùng một phiên bản mới được ký.
Ký quỹ theo mốc & tất toán: ký quỹ từng mốc, chỉ tất toán sau khi giao dịch thành công; phí nền tảng 10% (quy ước minh họa).
Chấm dứt & tranh chấp: phản hồi chấm dứt trong 3 ngày, bằng chứng trong 5 ngày, giữ tiền khi tranh chấp, chỉ đóng hợp đồng sau khi xử lý tiền.
Đánh giá hai chiều: ẩn đến khi đủ hai đánh giá hoặc hết 14 ngày kể từ khi kết thúc.
Xử lý đồng thời: khóa dữ liệu và đọc lại dữ liệu mới, chống bấm lặp.
3. Cấu trúc thư mục
├── anhtrongbaocao/        # Ảnh dùng trong báo cáo
├── sodotrongbaocao/       # Sơ đồ dùng trong báo cáo (ERD, luồng nghiệp vụ...)
├── sql/                   # Tài liệu / script SQL tham khảo
├── src/
│   ├── main/
│   │   ├── java/          # Mã nguồn Spring Boot (vn.biz04.*)
│   │   └── resources/     # Thymeleaf templates, static, Flyway migration (V1–V9)
│   └── test/              # Kiểm thử tự động
├── cau-hinh-email.ps1     # Script cấu hình SMTP để gửi OTP
├── chay.ps1               # Script build và chạy ứng dụng trên Windows
├── pom.xml                # Cấu hình Maven
└── README.md
4. Yêu cầu môi trường
Windows 10/11 và PowerShell (khuyến nghị PowerShell 7 – pwsh)
JDK 25
Apache Maven 3.9 trở lên
SQL Server (bản Express/Developer đều được) đang chạy, bật TCP/IP cổng 1433
Tùy chọn: tài khoản Gmail + Mật khẩu ứng dụng để gửi OTP thật

Kiểm tra nhanh:

powershell
java --version
mvn -v
Get-Service MSSQLSERVER      # Trạng thái phải là Running
5. Hướng dẫn cài đặt và chạy
Bước 1 — Tải mã nguồn
powershell
git clone https://github.com/smaii-space-code/xaydungnentangmarketingplacechochuyengia.git
cd xaydungnentangmarketingplacechochuyengia
Bước 2 — Tạo cơ sở dữ liệu

Mở SQL Server Management Studio (hoặc sqlcmd) và tạo một database trống tên BIZ04:

sql
CREATE DATABASE BIZ04;

Khi ứng dụng khởi động, Flyway tự chạy các migration V1 → V9 để tạo bảng. Không cần chạy tay và không chỉnh sửa các migration đã áp dụng.

Bước 3 — Chuẩn bị xác thực SQL Server

Chọn một trong hai cách:

Cách A — SQL Server Authentication (đơn giản nhất): dùng tài khoản SQL (ví dụ sa); script sẽ hỏi tên đăng nhập và mật khẩu khi chạy.

Cách B — Windows Authentication: cần thư viện mssql-jdbc_auth-13.4.0.x64.dll.

Tải gói Microsoft JDBC Driver 13.4 for SQL Server và lấy file DLL x64 trong thư mục auth.
Đặt vào: .tools\sqlserver\mssql-jdbc_auth-13.4.0.x64.dll
Bước 4 — (Tùy chọn) Cấu hình gửi OTP qua email
powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -EmailSetup ...

Hoặc chạy trực tiếp cau-hinh-email.ps1 và nhập Gmail cùng Mật khẩu ứng dụng khi được hỏi. Hệ thống dùng các biến môi trường:

Biến	Ý nghĩa
BIZ04_SMTP_HOST, BIZ04_SMTP_PORT	Máy chủ và cổng SMTP (Gmail: smtp.gmail.com, 587)
BIZ04_SMTP_USERNAME, BIZ04_SMTP_PASSWORD, BIZ04_SMTP_FROM	Tài khoản gửi và mật khẩu ứng dụng
BIZ04_OTP_HMAC_KEY	Khóa băm OTP
BIZ04_CCCD_KEY	Khóa mã hóa số CCCD

🔒 Không commit mật khẩu, khóa hay file chứa bí mật lên GitHub. Hãy đặt giá trị cho BIZ04_OTP_HMAC_KEY và BIZ04_CCCD_KEY một lần rồi giữ nguyên — đổi khóa sẽ làm dữ liệu đã mã hóa không đọc lại được.

Bước 5 — Chạy ứng dụng

Lần đầu (build rồi chạy), với Windows Authentication:

powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -AppPort 8081

Với SQL Server Authentication (bỏ -WindowsAuth, script sẽ hỏi tài khoản và mật khẩu):

powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -AppPort 8081

Chạy lại khi mã nguồn chưa thay đổi (bỏ qua bước build):

powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081

Đợi dòng Started MarketplaceApplication trong console, rồi mở trình duyệt: http://localhost:8081/

Các tham số của chay.ps1
Tham số	Mặc định	Ý nghĩa
-ServerName	localhost	Địa chỉ SQL Server
-Port	1433	Cổng SQL Server
-Database	BIZ04	Tên cơ sở dữ liệu
-DbUser	(hỏi khi chạy)	Tên đăng nhập SQL (khi không dùng Windows Auth)
-AppPort	8081	Cổng của website
-WindowsAuth	tắt	Dùng Windows Authentication
-SkipBuild	tắt	Bỏ qua mvn package
-Demo	tắt	Nạp dữ liệu demo — chỉ dùng cho CSDL mới, không dùng với dữ liệu đã có
-EmailSetup	tắt	Chạy cấu hình SMTP trước khi khởi động

Dừng ứng dụng bằng Ctrl + C. Nhật ký ghi tại .run/application.log.

6. Các trang chính
Đường dẫn	Mô tả
/ , /trang/trangchu	Trang chủ
/ve-sang-connect	Giới thiệu dự án / người sáng lập
/dang-ky	Chọn nhánh đăng ký (khách hàng / chuyên gia)
/bang-dieu-khien	Bảng điều khiển theo vai trò
/de-nghi-thue/da-gui, /de-nghi-thue/nhan	Đề nghị thuê đã gửi / đã nhận
/quan-tri/chuyen-gia	Quản trị: xét duyệt chuyên gia
7. Build và kiểm thử
powershell
mvn test        # Chạy kiểm thử (dùng H2 trong bộ nhớ)
mvn package     # Đóng gói target/marketplace-1.0.0.jar
Kiểm thử trên H2 chỉ để kiểm tra nhanh, không thay thế việc chạy thật trên SQL Server.
SqlServerIntegrationTest mặc định được bỏ qua. Muốn chạy trên SQL Server, dùng một database riêng tên BIZ04_Test và đặt biến môi trường BIZ04_TEST_SQLSERVER=true.
Nếu máy thiếu javac, có thể dùng profile tùy chọn: mvn -P compiler-eclipse test. Máy đã cài JDK thì dùng Maven bình thường.
8. Xử lý sự cố thường gặp
Hiện tượng	Cách xử lý
Cong 8081 dang duoc su dung	Đổi cổng: thêm -AppPort 8082, hoặc tắt tiến trình đang chiếm cổng
Thieu thu vien Windows Authentication	Thêm file mssql-jdbc_auth-13.4.0.x64.dll vào .tools\sqlserver\ hoặc bỏ -WindowsAuth
Không kết nối được SQL Server	Kiểm tra dịch vụ SQL Server đang Running, bật TCP/IP cổng 1433 trong SQL Server Configuration Manager, và database BIZ04 đã được tạo
Lỗi Flyway khi khởi động	Đọc .run/application.log; không xóa hay sửa migration đã áp dụng
Build chua thanh cong	Chạy mvn package thủ công để xem lỗi đầy đủ; kiểm tra phiên bản JDK
Không nhận được email OTP	Chạy lại với -EmailSetup, dùng Mật khẩu ứng dụng của Gmail (không phải mật khẩu đăng nhập Gmail)
Cảnh báo HTTP Status: 401 về flyway-community-db-support khi build	Cảnh báo vô hại, có thể bỏ qua
9. Quy ước nghiệp vụ minh họa

Các giá trị dưới đây là quy ước dùng cho đồ án, không phải quy định thực tế:

Phí nền tảng: 10%
Thời hạn phản hồi yêu cầu chấm dứt: 3 ngày
Thời hạn nộp bằng chứng: 5 ngày
Đánh giá hai bên được hiển thị khi đủ hai đánh giá hoặc sau 14 ngày kể từ khi kết thúc
10. Tác giả
Họ tên: Mai Phước Sang
MSSV: 1250080159
GitHub: smaii-space-code
