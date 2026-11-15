# Sang Connect — Nền tảng marketplace dịch vụ chuyên gia

Đồ án cá nhân của **MAI PHƯỚC SANG — 1250080159 — 12_CNPM2**.

Ứng dụng web Java/Spring Boot 3.5.16 + Thymeleaf + SQL Server. Chạy cục bộ tại `http://localhost:8081`. Không cần Node.js, Redis hoặc Docker để chạy bản này. Thanh toán chỉ mô phỏng nhưng nghiệp vụ, tài khoản, giao dịch và lịch sử được xử lý/lưu ở máy chủ.

Khẩu hiệu: **Bạn mang ý tưởng, Sang Connect kết nối chuyên gia.**

Bản cập nhật đã chạy trên máy này bằng Windows Authentication. Chạy lại từ thư mục có pom.xml (sau khi dừng đúng phiên Sang Connect đang chạy):

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081
```

Video 720p và poster được đóng gói trong `src/main/resources/static/videos/`. Tệp nguồn độ phân giải cao và video build được loại khỏi repository.

## 1. Trạng thái bản bàn giao

- Có mã cho UC01–UC15: tài khoản/vai trò, dịch vụ/tìm kiếm, yêu cầu, đề xuất, hợp đồng/phiên bản/mốc, ký quỹ, bàn giao/tệp, nghiệm thu/sửa, tất toán, đánh giá ẩn, tranh chấp, bằng chứng và quản trị.
- Có mở rộng chấm dứt đồng thuận/chuyển tranh chấp, phân bổ tiền, phí minh họa và nhật ký.
- Đã biên dịch và kiểm thử nghiệp vụ bằng H2 ở test scope. Xem `docs/KET_QUA_KIEM_TRA.md` cho số liệu và các giới hạn.
- Đã chạy JAR trên SQL Server thật qua Windows Authentication tại cổng 8081; Flyway xác nhận hai migration hợp lệ. Hai kiểm thử chỉ mục/khóa đồng thời trên BIZ04_Test vẫn bỏ qua. Xem docs/CAP_NHAT_SANG_CONNECT.md cho bằng chứng mới nhất.

## 2. Chuẩn bị trên Windows

Máy người dùng đã có Java 25, Maven 3.10.0 và MSSQLSERVER đang chạy theo ảnh cung cấp. Phiên bản SQL Server, TCP/IP và tài khoản kết nối cần được xác nhận.

1. Giải nén, mở thư mục có `pom.xml` trong VS Code.
2. Mở SSMS, kết nối SQL Server. Chạy `sql/01_tao_database.sql` để tạo database mới `BIZ04` nếu chưa có và lấy thông tin máy chủ. Lệnh không xóa dữ liệu.
3. Chuẩn bị tên đăng nhập **SQL Server Authentication** có quyền trên database BIZ04 để Flyway tạo bảng/chỉ mục. Tài khoản riêng cho đồ án được ưu tiên. Nếu hiện chỉ đăng nhập bằng Windows Authentication, hãy gửi kiểu đăng nhập và tên máy chủ cho giảng viên hướng dẫn; đừng tự đổi cấu hình hay cài lại SQL Server.
4. Xác nhận cổng TCP thật. `localhost:1433` là cấu hình khởi đầu cho instance mặc định; không phải kết quả đã kiểm tra trên máy em. SSMS có thể kết nối bằng Shared Memory trong khi JDBC/TCP vẫn chưa hoạt động.

## 3. Chạy lần đầu

Mở PowerShell tại thư mục có `pom.xml`, chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -ServerName localhost -Port 1433 -Demo
```

Thay `localhost` và cổng khi thông tin máy chủ thực tế khác. Script build bằng Maven, hỏi tên đăng nhập SQL Server và hỏi mật khẩu ở chế độ ẩn; không lưu mật khẩu vào mã nguồn. Lần build đầu cần Internet để Maven tải các thư viện. JDBC được tải bằng Maven, không cần tự chép JAR.

Đợi dòng `Started MarketplaceApplication`, sau đó mở `http://localhost:8081`. Nếu có lỗi trước dòng này thì ứng dụng chưa khởi động thành công; chụp đoạn lỗi cuối, không gửi mật khẩu.

`-Demo` chỉ tạo dữ liệu mẫu khi bảng người dùng đang trống. Không reset hoặc ghi đè tài khoản đã có.

Khi chạy với `-Demo`, ứng dụng tạo các tài khoản mẫu sau:

| Vai trò | Email |
|---|---|
| Quản trị viên | admin@biz04.local |
| Khách hàng | khach@biz04.local |
| Chuyên gia thiết kế | chuyengia@biz04.local |
| Chuyên gia nội dung | noidung@biz04.local |
| Người ngoài hợp đồng | ngoai@biz04.local |

Trước lần tạo dữ liệu mẫu, đặt `BIZ04_DEMO_PASSWORD` thành mật khẩu riêng tại máy cục bộ (tối thiểu 8 ký tự); cùng mật khẩu này được dùng cho các tài khoản mẫu. Không lưu mật khẩu vào repository. Đăng ký công khai không được tạo quản trị viên.

Các lần sau, nếu mã không thay đổi:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -ServerName localhost -Port 1433 -SkipBuild
```

Nhấn Ctrl+C để dừng. Dữ liệu nghiệp vụ vẫn ở SQL Server. Tệp bàn giao/bằng chứng ở `du-lieu/tep`; giữ thư mục này cùng cơ sở dữ liệu khi sao lưu/chuyển máy.

## 4. Cách chạy bằng biến môi trường

Nếu cần tự chạy lệnh, ví dụ sau chỉ dùng giá trị mẫu và nhập mật khẩu cục bộ:

```powershell
$env:BIZ04_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=BIZ04;encrypt=true;trustServerCertificate=true;loginTimeout=10;'
$env:BIZ04_DB_USER = Read-Host 'Nhap ten dang nhap SQL Server'
$biz04Secret = Read-Host 'Nhap mat khau SQL Server' -AsSecureString
$env:BIZ04_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $biz04Secret).Password
$biz04DemoSecret = Read-Host 'Nhap mat khau rieng cho tai khoan demo (toi thieu 8 ky tu)' -AsSecureString
$env:BIZ04_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', $biz04DemoSecret).Password
$env:BIZ04_DEMO = 'true'
mvn spring-boot:run
```

Ứng dụng bind `127.0.0.1` cho phạm vi cục bộ. `trustServerCertificate=true` ở ví dụ phục vụ máy phát triển; chưa phải cấu hình triển khai Internet.

## 5. Kịch bản trình diễn nhanh

1. Đăng nhập khách hàng, đăng yêu cầu độc lập hoặc chọn dịch vụ có sẵn để tạo yêu cầu.
2. Đăng nhập chuyên gia ở một trình duyệt/phiên riêng, gửi đề xuất. Với yêu cầu xuất phát từ dịch vụ, chỉ chuyên gia đăng dịch vụ đó gửi đề xuất.
3. Khách chọn đề xuất, tạo hợp đồng. Thêm các mốc, tổng tiền bằng tổng hợp đồng. Hai bên đọc và xác nhận cùng phiên bản; sửa nội dung/mốc sẽ hủy hiệu lực xác nhận cũ.
4. Khách ký quỹ mốc 1. Thử ký quỹ mốc 2 trước tất toán mốc 1 sẽ bị chặn.
5. Chuyên gia nộp tệp/đường dẫn. Khách có thể yêu cầu sửa, chuyên gia nộp lại, rồi khách nghiệm thu. Chọn kết quả giải ngân mô phỏng Thất bại để thấy mốc chưa tất toán, sau đó thử lại Thành công.
6. Làm tiếp các mốc. Hợp đồng chỉ Hoàn thành khi tất cả mốc đã tất toán.
7. Khách gửi đánh giá. Đối tác chưa thấy nội dung/điểm. Chuyên gia gửi đánh giá thứ hai thì hai đánh giá hợp lệ được công khai. Nếu chỉ một bên gửi, tác vụ hết hạn công khai sau 14 ngày từ kết thúc.
8. Với hợp đồng khác đang có ký quỹ, mở tranh chấp, gửi bằng chứng. Quản trị viên xem, yêu cầu bổ sung hoặc kết luận giải ngân/hoàn/chia. Chọn mô phỏng hoàn tiền thất bại để thấy hồ sơ chưa đóng; thử lại không giải ngân lặp phần đã thành công.
9. Với hợp đồng nhiều mốc, thử đề nghị chấm dứt và phân bổ ký quỹ mốc hiện tại. Bên còn lại đồng ý thì thực thi; không đồng ý khi còn tiền thì chuyển tranh chấp. Mốc chưa nạp không có khoản để hoàn.

Tác vụ nội bộ kiểm tra nghiệm thu/đánh giá hết hạn mỗi 60 giây. Nó không đổi hạn 14 ngày để chạy demo nhanh. Không tự hoàn tiền chỉ vì hết thời hạn phản hồi chấm dứt; khi chưa thống nhất phải giữ tiền/giải quyết tiếp.

## 6. Quy tắc và mở rộng dữ liệu

ERD cơ sở giữ đủ **12 bảng, 106 thuộc tính, 21 FK** trong `docs/ERD_BIZ04_CoSo.json`. `V1__erd_co_so.sql` hiện thực mô hình đó bằng kiểu SQL Server phù hợp tiếng Việt.

`V2__phi_va_cham_dut.sql` bổ sung rõ: `HopDong.TyLePhi`, `GiaoDich.PhiNenTang`, `GiaoDich.ThucNhan`, `TranhChap.HanNopBangChung` và bảng `YeuCauChamDut` 12 thuộc tính/4 FK. Bản hiện thực có 13 bảng miền, 122 thuộc tính, 25 FK; không gọi ERD cũ là đã có phần mở rộng.

Tiền dùng BigDecimal, VND nguyên. Phí 10% được chốt trên hợp đồng; tính trên phần trước phí phân bổ cho chuyên gia, làm tròn HALF_UP đến đồng. Ký quỹ/hoàn khách không thu phí. Thời hạn phản hồi chấm dứt 3 ngày và bằng chứng 5 ngày là quy ước minh họa của đồ án. Nghiệm thu và đánh giá đều 14 ngày nhưng tính từ hai sự kiện khác nhau.

Thời điểm lưu UTC bằng DATETIME2; giao diện đổi sang giờ Việt Nam. Chỉ giao dịch THÀNH CÔNG tham gia số dư/phí/thu nhập. Lần thử thất bại được giữ trong bản ghi thao tác và nhật ký, thử lại cùng khóa không ghi nhận tiền lặp.

Flyway quản lý lược đồ, Hibernate chỉ validate. Không dùng ddl-auto=update/create với database thật. Nếu BIZ04 đã có bảng không do migration này tạo, dừng để đối chiếu, không xóa/reset/baseline tùy tiện.

## 7. Kiểm thử

```powershell
mvn test
```

Các ca nghiệp vụ dùng H2 trong test scope; H2 không nằm trong JAR chạy thật. Có kiểm tra trang, CSRF, quyền đối tượng và không rò rỉ đánh giá. Đây không thay cho kiểm chứng SQL Server.

Để chạy kiểm thử riêng với SQL Server, tạo database **BIZ04_Test** tách biệt và cấp quyền phù hợp. Không dùng BIZ04 đang demo:

```powershell
$env:BIZ04_TEST_SQLSERVER = 'true'
$env:BIZ04_TEST_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=BIZ04_Test;encrypt=true;trustServerCertificate=true;loginTimeout=10;'
# BIZ04_DB_USER va BIZ04_DB_PASSWORD da dat cuc bo.
mvn '-Dtest=SqlServerIntegrationTest' test
```

Hai ca kiểm tra chỉ mục unique có điều kiện/NULL và ký quỹ đồng thời trên SQL Server. Chúng tạo dữ liệu có tên riêng trong BIZ04_Test và không tự xóa database. Tệp kiểm thử chặn URL không chỉ rõ BIZ04_Test.

## 8. Cấu trúc mã

- `domain`: 12 lớp miền cơ sở và lớp mở rộng YeuCauChamDut; ánh xạ tên bảng/cột PascalCase.
- `data/Store`: truy xuất ORM và khóa bản ghi cha.
- `service`: tài khoản, dịch vụ/yêu cầu/đề xuất, hợp đồng, tiền, bàn giao, đánh giá, tranh chấp, chấm dứt, tệp.
- `security`: đăng nhập, mã hóa mật khẩu, phiên, CSRF và quyền truy cập trang.
- `web`: controller, dữ liệu hiển thị, định dạng và thông báo lỗi.
- `config`: dữ liệu mẫu và tác vụ hết hạn.
- `templates`/`static`: giao diện Thymeleaf và CSS cục bộ, không phụ thuộc CDN.
- `db/migration`: migration SQL Server có phiên bản.

Mở rộng còn có thể làm sau: thông báo email thật, nhắn tin, tìm kiếm nâng cao, giao diện tiện dụng hơn và triển khai Internet. Chưa tích hợp cổng tiền thật hoặc ví/rút ngân hàng.



## Đăng ký OTP và hồ sơ khách hàng

Bản cập nhật dùng migration V3, giữ dữ liệu cũ. Xem [hướng dẫn cấu hình Gmail, khóa OTP/CCCD và kết quả kiểm tra](docs/DANG_KY_OTP_HO_SO.md). Để nhập mật khẩu ứng dụng Gmail trực tiếp trên máy và chạy web, dùng `pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081 -EmailSetup` sau khi dừng đúng tiến trình đang chạy.

## Đường dẫn truy cập

Trang chủ có video: http://localhost:8081/ . Danh sách dịch vụ đã đổi sang http://localhost:8081/trang/trangchu ; /dich-vu chuyển hướng đến đường dẫn mới. Xem [bằng chứng kiểm tra trang chủ](docs/KIEM_TRA_TRANG_CHU.md).

