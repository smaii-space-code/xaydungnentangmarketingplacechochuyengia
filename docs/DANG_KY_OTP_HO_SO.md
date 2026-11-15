# Đăng ký, OTP email và hồ sơ Sang Connect

Phạm vi: UC01, hồ sơ khách hàng, đổi email, vai trò hồ sơ và lời chào tài khoản. Không đổi trang chủ, video, logo hoặc nghiệp vụ hợp đồng/thanh toán. Dependency bổ sung: Spring Mail, phiên bản do Spring Boot 3.5.16 hiện tại quản lý.

## Dữ liệu và tương thích

Migration mới `V3__ho_so_va_email_otp.sql`; V1/V2 giữ nguyên. Giữ database BIZ04 và dữ liệu cũ. `NguoiDung` có trạng thái email, thời điểm xác thực và ngày tạo/cập nhật UTC kiểu DATETIMEOFFSET(6). Hồ sơ 1–1 `HoSoKhachHang` lưu điện thoại, loại khách hàng, tổ chức, địa chỉ, giới thiệu, CCCD mã hóa và 4 số cuối. Không sao chép email/họ tên sang hồ sơ.

Tài khoản cũ giữ trạng thái hoạt động/khóa vốn có, email là `CHUYEN_TIEP`, chưa được xem là đã xác thực. Ngày tạo của tài khoản cũ là thời điểm migration vì dữ liệu trước đây không có ngày đăng ký. Điện thoại hồ sơ cũ cho phép NULL. Demo chỉ tạo khi bảng người dùng trống theo chính sách có sẵn; đánh dấu `DEMO`, không giả xác thực email. Đăng ký công khai luôn tạo `CHO_XAC_THUC` và không có lối kích hoạt demo.

Email được trim/lowercase thống nhất, chỉ mục computed `EmailChuan` dùng collation không phân biệt hoa thường. Migration dừng nếu có trùng sau chuẩn hóa; không tự ghép/xóa tài khoản.

`MaXacThucOTP` hỗ trợ DANG_KY, DAT_LAI_MAT_KHAU, CAP_NHAT_THONG_TIN; chưa xây luồng quên mật khẩu. OTP 6 số, 5 phút, SecureRandom, HMAC-SHA-256 gắn tài khoản/email/mục đích/challenge. Chỉ challenge ngẫu nhiên trong phiên mới có quyền gửi lại/sửa email chờ. Database chỉ lưu digest challenge và hash OTP. Chỉ mục filtered bảo đảm tối đa một mã chờ/tài khoản/mục đích. `OtpDieuPhoi` khóa tuần tự các giao dịch OTP; `OtpGioiHan` lưu hạn mức theo giờ cho tài khoản, email, IP (địa chỉ kết nối trực tiếp, không tin X-Forwarded-For). Giới hạn vẫn tồn tại sau khởi động lại/gửi lại.

CCCD dùng AES-256-GCM, nonce ngẫu nhiên mới và AAD gắn tài khoản/phiên bản. Cột VARCHAR(255) chứa phiên bản, nonce, ciphertext/tag; chỉ trả 4 số cuối tới giao diện. DTO dùng `soCccdMoi`; thiếu/trống giữ nguyên ciphertext. Không có chức năng xóa. Thiếu khóa báo lỗi, không lưu rõ. Giữ khóa CCCD ổn định và sao lưu an toàn khi chuyển máy; không tạo lại khóa đã dùng.

## Cấu hình Gmail trên máy

1. Đăng nhập tài khoản email người gửi, bật Xác minh 2 bước.
2. Mở https://myaccount.google.com/apppasswords, tạo mật khẩu ứng dụng cho Sang Connect. Tùy chính sách tài khoản, Google có thể không cung cấp lựa chọn này. Không gửi mật khẩu qua chat.
3. Dừng đúng phiên Sang Connect trước, mở PowerShell 7 tại thư mục có pom.xml rồi chạy.

Lệnh khởi động và nhập cấu hình:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081 -EmailSetup
```

Script hỏi mật khẩu ứng dụng bằng ô nhập ẩn. SMTP password chỉ ở biến môi trường của tiến trình, không ghi file. Hai khóa 32 byte được sinh bằng CSPRNG một lần nếu chưa có, lưu vào biến môi trường người dùng và nạp vào tiến trình; không in giá trị. Script tái sử dụng khóa đã có. Muốn tự quản lý khóa: đặt Base64 của 32 byte ngẫu nhiên vào hai biến khóa trước khi chạy. Không dùng giá trị khóa trong kiểm thử cho môi trường thật.

| Biến | Ý nghĩa |
|---|---|
| BIZ04_SMTP_HOST | Máy chủ SMTP; Gmail: smtp.gmail.com |
| BIZ04_SMTP_PORT | 587, STARTTLS bắt buộc và xác thực chứng chỉ |
| BIZ04_SMTP_USERNAME | Tài khoản gửi |
| BIZ04_SMTP_FROM | Địa chỉ gửi; tên hiển thị Sang Connect |
| BIZ04_SMTP_PASSWORD | Mật khẩu ứng dụng nhập tại terminal |
| BIZ04_OTP_HMAC_KEY | Khóa HMAC, Base64 32 byte |
| BIZ04_CCCD_KEY | Khóa AES-GCM, Base64 32 byte |
| BIZ04_OTP_COOLDOWN_SECONDS | Tối thiểu 60 giây |
| BIZ04_OTP_SEND_ACCOUNT_HOUR | Mặc định 5; áp dụng cho cả tài khoản và email |
| BIZ04_OTP_SEND_IP_HOUR | Mặc định 20 |
| BIZ04_OTP_TRY_ACCOUNT_HOUR | Mặc định 20; áp dụng cho cả tài khoản và email |
| BIZ04_OTP_TRY_IP_HOUR | Mặc định 100 |

SQL Server tiếp tục dùng BIZ04_DB_URL/USER/PASSWORD hoặc Windows Authentication của chay.ps1; không cần đổi database hoặc cài dịch vụ khác.

Thiếu SMTP/khóa OTP thì đăng ký vẫn ở trạng thái chờ và báo chưa gửi được mã, không hiển thị OTP thay thế. Sau khi cấu hình và khởi động lại, phiên trình duyệt cũ có thể mất. Mở Đăng ký → “Đã đăng ký nhưng chưa xác thực?”, nhập email và mật khẩu đã đăng ký để nhận challenge/mã mới. Luồng này chỉ chấp nhận tài khoản đang chờ và mật khẩu hiện có, áp dụng hạn mức thử/gửi; không sửa mật khẩu/hồ sơ hay kích hoạt tài khoản đã tồn tại. Không thể tiếp tục chỉ bằng email hoặc mã người dùng.

SMTP chấp nhận chỉ có nghĩa máy chủ nhận thư để xử lý. Không chứng minh thư đã tới hoặc hộp thư tồn tại. Chỉ thông báo không tồn tại khi có SMTPAddressFailedException ở RCPT TO và phản hồi SMTP chuẩn 550 5.1.1; 550 chung, lỗi xác thực, timeout và lỗi không rõ đều dùng thông báo gửi thất bại. Không triển khai đọc hộp thư/bounce.

Nguồn chính thức đã đối chiếu: [Google: mật khẩu ứng dụng](https://support.google.com/accounts/answer/185833), [Google: cấu hình SMTP](https://support.google.com/mail/answer/7104828), [Angus: lỗi địa chỉ SMTP](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/SMTPAddressFailedException.html).

## Vai trò và khóa

Yêu cầu DANG_MO/DANG_THUONG_LUONG còn nghĩa vụ; DA_DONG đã đóng. Đề xuất DANG_GUI trên yêu cầu DANG_MO còn hiệu lực; DA_RUT/BI_TU_CHOI không chặn. DA_CHON được kiểm tra qua hợp đồng. Hợp đồng CHO_XAC_NHAN, DANG_THUC_HIEN, TAM_DUNG, CHO_CHAM_DUT, CHO_TIEP_TUC còn nghĩa vụ; HOAN_THANH, DA_HUY, DA_CHAM_DUT là trạng thái kết thúc, vẫn phải hết ký quỹ và tranh chấp. Tranh chấp chỉ đóng khi NgayDong có giá trị; giao dịch DANG_XU_LY hoặc số dư ký quỹ còn lại tiếp tục chặn bỏ vai trò đúng quan hệ khách/chuyên gia. Mốc sửa/bàn giao/giải ngân chưa xong thuộc hợp đồng chưa kết thúc. Giữ nguyên vai trò không chạy điều kiện bỏ vai trò.

Hồ sơ khóa NguoiDung trước kiểm tra và lưu. Tạo yêu cầu/đề xuất cũng khóa tài khoản và đọc quyền mới. Chọn đề xuất tạo hợp đồng khóa hai tài khoản theo ID tăng dần rồi khóa yêu cầu; kiểm tra vai trò hiện tại cả hai bên. Không đổi kết quả nghiệp vụ khác. Sau lưu hồ sơ, phiên hiện tại nạp lại quyền; các phiên khác vẫn bị kiểm tra quyền hiện tại ở backend khi tạo nghĩa vụ. Lịch sử không bị xóa, quyền xem vẫn theo quan hệ sở hữu.

## Cách thử

Đăng ký cá nhân/tổ chức → nhận OTP email → xác thực → đăng nhập → Hồ sơ của tôi. Sửa địa chỉ/giới thiệu không cần mật khẩu/OTP. CCCD tùy chọn; sau lưu chỉ còn nhãn che. Thử cập nhật trống để giữ nguyên, hoặc nhập mới đúng 12 số. Chọn vai trò rồi lưu; nghĩa vụ đang mở chặn việc bỏ vai trò liên quan. Đổi tên cập nhật lời chào ngay. Đổi email cần mật khẩu hiện tại, xác thực OTP gửi tới email mới, sau đó đăng nhập lại.

Email nhận lấy từ form, không giới hạn gmail.com. Chỉ thử gửi thật bằng đăng ký/gửi lại có kiểm soát sau khi cấu hình; không gửi hàng loạt.

## Kết quả kiểm chứng

Ngày 27/10/2026: `mvn package` thành công với **76 kiểm thử, 0 lỗi, 0 bỏ qua** trong lượt kiểm chứng có bật SQL Server. Gồm 39 ca tài khoản/OTP (H2, email giả lập), 28 ca nghiệp vụ hồi quy, 2 ca trang công khai, 5 ca tài khoản/OTP SQL Server và 2 ca SQL Server chỉ mục/ký quỹ đồng thời. Các kiểm thử SQL Server chỉ chạy trên BIZ04_Test qua Windows Authentication. Các giá trị khóa trong cấu hình kiểm thử chỉ dùng ở test, không được đưa vào cấu hình chạy thật.

Migration V3 đã áp dụng thành công trên BIZ04 thật; Hibernate validate và khởi động ứng dụng thành công. Đối chiếu trước/sau: NguoiDung 5, YeuCau 1, DeXuat 0, HopDong 0, GiaoDich 0; 5 tài khoản cũ đều CHUYEN_TIEP, thời điểm xác thực NULL. Không reset dữ liệu. Bằng chứng: `.run/biz04-before-v3.txt`, `.run/biz04-after-v3.txt`, `.run/account-verification.log`, `target/surefire-reports/`.

Đã kiểm tra bằng Edge headless ở 1440, 768, 390 px: đăng ký cá nhân/tổ chức, lỗi form, hồ sơ tên dài và OTP thông báo dài; không tràn ngang, trường/nút không vượt khung. Đã kiểm tra ẩn/hiện tổ chức, hủy CCCD xóa ô nhập, dữ liệu an toàn được giữ sau lỗi và mật khẩu không được đổ lại. Trang đăng ký/lỗi form chụp từ ứng dụng thật; đăng nhập tài khoản khách hàng mẫu và mở hồ sơ thật cũng đã thành công ở cả ba kích thước (ảnh trong `.run/account-qa/live/`); ảnh hồ sơ/OTP dùng HTML do MockMvc render với dữ liệu test để không sửa tài khoản thật. Ảnh trước chỉ có trang đăng ký; chưa có ảnh hồ sơ trước thay đổi.

Ảnh: [trước, đăng ký 390px](../.run/account-qa/before/register-390.png), [sau, đăng ký 390px](../.run/account-qa/after/register-390.png), [hồ sơ 390px](../.run/account-qa/after/profile-390.png), [OTP 768px](../.run/account-qa/after/otp-768.png). Báo cáo hình học: `.run/account-qa/after/report.json`.

**Chưa thử SMTP thật:** phiên chạy hiện tại chưa có cấu hình SMTP/khóa riêng được người dùng nhập. Không gửi thư thật, không tạo OTP hiển thị thay thế. Cần chạy `-EmailSetup` trong terminal cá nhân để thử đăng ký → nhận OTP thật → đăng nhập. Các kết quả gửi thành công/thất bại trong kiểm thử đều là giả lập có kiểm soát.

Không thực hiện gửi thư thật hoặc sửa hồ sơ tài khoản cũ trong kiểm tra trình duyệt.

## Danh sách tệp thay đổi

- Sửa: `src/main/java/vn/biz04/config/DemoData.java`
- Mới: `src/main/java/vn/biz04/domain/HoSoKhachHang.java`
- Mới: `src/main/java/vn/biz04/domain/MaXacThucOTP.java`
- Sửa: `src/main/java/vn/biz04/domain/NguoiDung.java`
- Mới: `src/main/java/vn/biz04/domain/OtpDieuPhoi.java`
- Mới: `src/main/java/vn/biz04/domain/OtpGioiHan.java`
- Sửa: `src/main/java/vn/biz04/security/SecurityConfig.java`
- Sửa: `src/main/java/vn/biz04/service/Access.java`
- Mới: `src/main/java/vn/biz04/service/AccountCrypto.java`
- Mới: `src/main/java/vn/biz04/service/AccountForm.java`
- Sửa: `src/main/java/vn/biz04/service/AccountService.java`
- Sửa: `src/main/java/vn/biz04/service/CatalogService.java`
- Sửa: `src/main/java/vn/biz04/service/ContractService.java`
- Mới: `src/main/java/vn/biz04/service/OtpMailGateway.java`
- Mới: `src/main/java/vn/biz04/service/OtpService.java`
- Mới: `src/main/java/vn/biz04/service/RoleObligations.java`
- Mới: `src/main/java/vn/biz04/service/SmtpOtpMailGateway.java`
- Mới: `src/main/java/vn/biz04/web/AccountWeb.java`
- Sửa: `src/main/java/vn/biz04/web/Format.java`
- Sửa: `src/main/java/vn/biz04/web/Web.java`
- Sửa: `src/main/resources/application.properties`
- Mới: `src/main/resources/db/migration/V3__ho_so_va_email_otp.sql`
- Mới: `src/main/resources/static/css/account.css`
- Mới: `src/main/resources/static/js/account.js`
- Sửa: `src/main/resources/templates/layout.html`
- Mới: `src/main/resources/templates/otp.html`
- Sửa: `src/main/resources/templates/profile.html`
- Sửa: `src/main/resources/templates/register.html`
- Mới: `src/test/java/vn/biz04/AccountOtpTest.java`
- Mới: `src/test/java/vn/biz04/AccountSqlServerTest.java`
- Sửa: `src/test/java/vn/biz04/BusinessFlowTest.java`
- Sửa: `src/test/java/vn/biz04/SqlServerIntegrationTest.java`
- Mới: `src/test/java/vn/biz04/TestAccounts.java`
- Sửa: `pom.xml`
- Sửa: `chay.ps1`
- Mới: `cau-hinh-email.ps1`
- Sửa: `README.md`
- Mới: `docs/DANG_KY_OTP_HO_SO.md`

