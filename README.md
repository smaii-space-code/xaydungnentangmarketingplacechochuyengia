# Sang Connect — Nền tảng marketplace dịch vụ chuyên gia

Đồ án cá nhân của **MAI PHƯỚC SANG — 1250080159 — 12_CNPM2**.

Sang Connect kết nối khách hàng cần thuê dịch vụ với chuyên gia. Ứng dụng hỗ trợ từ đăng ký, tìm kiếm và thương lượng đến lập hợp đồng, theo dõi từng mốc, bàn giao và giải quyết tranh chấp. Ký quỹ, giải ngân và hoàn tiền dùng dữ liệu mô phỏng bằng VND.

## Chức năng đã triển khai

| Nhóm chức năng | Nội dung |
| --- | --- |
| Tài khoản và xác thực | Đăng ký khách hàng, đăng ký chuyên gia, xác thực email bằng OTP, tiếp tục xác thực từ trang đăng nhập, cập nhật hồ sơ và ảnh đại diện chuyên gia. |
| Tìm kiếm và thuê chuyên gia | Khách hàng tìm chuyên gia hoặc dịch vụ, đăng yêu cầu hoặc gửi đề nghị thuê trực tiếp. Chuyên gia tìm dự án, gửi đề xuất, thương lượng và quản lý dịch vụ. |
| Hợp đồng điện tử | Hai bên thống nhất kế hoạch mốc, rà soát điều khoản, vẽ chữ ký và xác nhận cùng một phiên bản. Hệ thống lưu lịch sử hợp đồng và các lần sửa đổi. |
| Thực hiện hợp đồng | Ký quỹ theo mốc, đính kèm sản phẩm bàn giao, yêu cầu chỉnh sửa, nghiệm thu, tất toán và đánh giá hai chiều. Hai bên có thể đề nghị chấm dứt hợp đồng và xử lý khoản đã ký quỹ. |
| Quản trị | Tạo và quản lý tài khoản, xét duyệt hồ sơ chuyên gia, ghi lý do từ chối và xử lý hồ sơ nộp lại. Quản trị viên duyệt dịch vụ, kiểm duyệt đánh giá, xử lý tranh chấp, theo dõi bằng chứng và nhật ký. |

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Máy chủ | JDK 25, Spring Boot 3.5.16, Spring MVC, Spring Security, Bean Validation |
| Giao diện | Thymeleaf, HTML, CSS, JavaScript |
| Cơ sở dữ liệu | Microsoft SQL Server, Spring Data JPA, Hibernate, Microsoft JDBC Driver 13.4.0, Flyway |
| Email và ảnh | Spring Boot Mail, Gmail SMTP, TwelveMonkeys ImageIO cho ảnh WebP |
| Build và kiểm thử | Apache Maven, JUnit 5, Spring Boot Test, Spring Security Test, H2 dùng riêng cho kiểm thử |

Các thư viện Java được khai báo trong [pom.xml](pom.xml). Maven tự tải chúng khi build lần đầu.

## Nội dung trên GitHub

| Đường dẫn | Nội dung |
| --- | --- |
| [src/main](src/main) | Mã Java, cấu hình ứng dụng, giao diện, CSS/JS, ảnh, video trang chủ và [migration V1 đến V11](src/main/resources/db/migration). V10 bổ sung ảnh đại diện chuyên gia, V11 bổ sung chữ ký hợp đồng. |
| [src/test](src/test) | Kiểm thử nghiệp vụ, phân quyền, luồng OTP, quản trị tài khoản và hợp đồng. |
| [sql](sql) | Script tạo database và câu truy vấn kiểm tra sau khi chạy. |
| [chay.ps1](chay.ps1) và [cau-hinh-email.ps1](cau-hinh-email.ps1) | Script build, chạy ứng dụng và cấu hình Gmail SMTP. |
| [anhtrongbaocao](anhtrongbaocao), [sodotrongbaocao](sodotrongbaocao) và [sourceanh](sourceanh) | Ảnh, sơ đồ Draw.io và tài liệu minh họa giao diện, đăng ký, hợp đồng và tranh chấp. |

## Cài đặt và chạy trên Windows

1. Cài JDK 25 và Apache Maven. Đặt biến môi trường `JAVA_HOME` trỏ đến thư mục JDK, rồi thêm `%JAVA_HOME%\bin` và thư mục `bin` của Maven vào `PATH`. Mở PowerShell mới và kiểm tra bằng `java -version` và `mvn -version`. [JDK 25](https://adoptium.net/temurin/releases/?version=25) · [Apache Maven](https://maven.apache.org/install.html)
2. Tải repository bằng Git hoặc **Code → Download ZIP**. Cài SQL Server và SSMS. Khởi động dịch vụ SQL Server, bật TCP/IP và xác nhận cổng của instance. Mở [sql/01_tao_database.sql](sql/01_tao_database.sql) trong SSMS và chạy để tạo database `BIZ04` nếu chưa có. Tài khoản kết nối cần quyền tạo và cập nhật lược đồ trong database này để Flyway chạy migration. [SQL Server](https://www.microsoft.com/sql-server/sql-server-downloads) · [SSMS](https://learn.microsoft.com/ssms/install/install)
3. Mở PowerShell ở thư mục có `pom.xml` và chạy lệnh dưới đây. Script build JAR rồi hỏi tên đăng nhập và mật khẩu SQL Server. Điều chỉnh `-ServerName` và `-Port` theo máy.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -ServerName localhost -Port 1433
```

Đợi dòng `Started MarketplaceApplication` rồi mở [Sang Connect](http://localhost:8081/). Nhấn **Ctrl+C** để dừng. Có thể dùng `-SkipBuild` khi `target/marketplace-1.0.0.jar` đã có và mã nguồn chưa thay đổi.

Nếu dùng **Windows Authentication**, đặt DLL `mssql-jdbc_auth-13.4.0.x64.dll` từ bộ Microsoft JDBC Driver 13.4.0 vào thư mục `.tools\sqlserver\`. Chạy cùng lệnh trên với tham số `-WindowsAuth`. [Microsoft JDBC Driver](https://learn.microsoft.com/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server)

### Tạo tài khoản trình diễn

Với database mới, nhập một mật khẩu riêng tối thiểu 8 ký tự qua PowerShell rồi chạy ở chế độ demo.

```powershell
$demoSecret = Read-Host 'Nhap mat khau tai khoan demo' -AsSecureString
$env:BIZ04_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $demoSecret).Password
try {
    powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -Demo
} finally {
    Remove-Item Env:BIZ04_DEMO_PASSWORD -ErrorAction SilentlyContinue
    $demoSecret.Dispose()
}
```

Các tài khoản dùng chung mật khẩu vừa nhập gồm `admin@biz04.local` (quản trị), `khach@biz04.local` (khách hàng), `chuyengia@biz04.local`, `noidung@biz04.local` (chuyên gia) và `ngoai@biz04.local` (người ngoài hợp đồng). Dữ liệu mẫu chỉ được tạo khi bảng người dùng còn trống.

## Cấu hình Gmail gửi mã OTP

1. Bật **Xác minh 2 bước** cho tài khoản Google dùng để gửi thư, rồi tạo **Mật khẩu ứng dụng**. Tài khoản do trường hoặc tổ chức quản lý có thể bị giới hạn tính năng này. [Hướng dẫn Mật khẩu ứng dụng](https://support.google.com/accounts/answer/185833?hl=vi)
2. Dừng phiên Sang Connect đang chạy rồi khởi động lại với `-EmailSetup`. Thêm `-WindowsAuth` nếu dùng xác thực Windows cho SQL Server.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -ServerName localhost -Port 1433 -EmailSetup
```

3. Nhập địa chỉ Gmail gửi thư và mật khẩu ứng dụng ở lời nhắc ẩn. Script dùng `smtp.gmail.com`, cổng `587` và STARTTLS. Khi thiếu, script tạo khóa HMAC cho OTP và khóa mã hóa CCCD trong biến môi trường người dùng Windows. Giữ lại các khóa khi sao lưu hoặc chuyển máy.
4. Mở **Đăng ký**, nhập email nhận thư và dùng OTP 6 chữ số trong vòng 5 phút. Kiểm tra cả mục Thư rác. Khách hàng có thể đăng nhập sau khi xác thực. Hồ sơ chuyên gia chuyển sang **Chờ xét duyệt**.

Mật khẩu SQL, mật khẩu ứng dụng Gmail và các khóa chỉ dùng trong môi trường chạy cục bộ. Cấu hình công khai đọc các giá trị này từ biến môi trường.

## Quy trình trình diễn ngắn

1. Đăng ký khách hàng và chuyên gia bằng hai email có thể nhận OTP. Đăng nhập quản trị viên, vào **Quản trị → Xét duyệt chuyên gia**. Có thể thử từ chối kèm lý do, xác thực lại, gửi hồ sơ phiên bản mới rồi duyệt.
2. Đăng nhập khách hàng, tìm dịch vụ rồi gửi đề nghị thuê. Với luồng dự án, khách hàng đăng yêu cầu và chuyên gia gửi đề xuất.
3. Thống nhất kế hoạch mốc, lập hợp đồng, rà soát và xác nhận cùng phiên bản. Khách hàng ký quỹ mô phỏng, chuyên gia bàn giao, sau đó khách hàng nghiệm thu hoặc yêu cầu sửa.
4. Thử tất toán và đánh giá hai chiều. Với mốc có bất đồng, mở tranh chấp, nộp bằng chứng rồi xem cách quản trị viên xử lý.

## Kiểm thử

```powershell
mvn test
mvn package
```

Bộ kiểm thử mặc định dùng H2. Kiểm thử tích hợp SQL Server cần database riêng `BIZ04_Test`, `BIZ04_TEST_SQLSERVER=true`, `BIZ04_TEST_DB_URL` và thông tin kết nối SQL đặt ở máy chạy. Tệp bàn giao và bằng chứng khi chạy ứng dụng được lưu dưới `du-lieu/tep`.

