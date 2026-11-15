# Đăng ký và xét duyệt chuyên gia — 04/11/2026

## Đã triển khai và vận hành

Web: http://localhost:8081/ . Đúng Maven root `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace`.
PID ứng dụng tại kiểm tra: **12896**. JAR `target/marketplace-1.0.0.jar`, SHA-256:
`A4280BCBC581D5A09AB8A2AB7F818660985D48B13B5161ADFE8FBCC77DB3B885`.

| Mục | Route / cách sử dụng |
|---|---|
| Chọn loại tài khoản | `/dang-ky` |
| Khách hàng | GET `/dang-ky/khach-hang`; POST `/dang-ky` giữ tương thích luồng cũ |
| Chuyên gia mới | GET/POST `/dang-ky/chuyen-gia` |
| OTP đăng ký | `/xac-thuc-email`, tiếp tục/gửi lại theo luồng hiện có |
| Admin xét duyệt | Quản trị → Xét duyệt chuyên gia; `/quan-tri/chuyen-gia` |
| Chi tiết và lịch sử admin | GET `/quan-tri/chuyen-gia/{id}`, POST `/quan-tri/chuyen-gia/{id}/duyet` |
| Trạng thái riêng | `/dang-ky/chuyen-gia/trang-thai`, chỉ sau mật khẩu hợp lệ |
| Đăng ký lại | POST `/dang-ky/chuyen-gia/dang-ky-lai` → `/xac-thuc-lai` → `/ho-so`, dưới cùng prefix `/dang-ky/chuyen-gia` |

Không có thao tác gửi email thông báo duyệt mới; người dùng xem kết quả khi đăng nhập. Câu thông báo chờ duyệt giữ đúng nội dung được yêu cầu.

## Luồng và ràng buộc

- Khách hàng mới vẫn xác thực email rồi đăng nhập. Nhánh chuyên gia không được tạo thành khách hàng hoạt động.
- Họ tên: 1–100 ký tự. Email: đúng định dạng theo AccountService, tối đa 254 ký tự và duy nhất. Mật khẩu: từ 8 ký tự, tối đa 72 byte UTF-8, xác nhận phải khớp.
- Điện thoại bắt buộc: 10 chữ số Việt Nam, đầu 03/05/07/08/09. Lĩnh vực tự nhập: 2–100 ký tự; giới thiệu: 20–2.000 ký tự. Không thêm ảnh, kỹ năng, CCCD hoặc chứng chỉ.
- Chuyên gia ban đầu không có cờ khách hàng/chuyên gia/admin. Email OTP hợp lệ mới tạo bản chụp đầu tiên và chuyển chờ xét duyệt; email được đổi hợp lệ trước OTP sẽ được chụp đúng email đã xác thực.
- Admin duyệt đúng trạng thái, email và phiên bản trong một transaction với cấp duy nhất quyền chuyên gia. Từ chối bắt buộc lý do 1–2.000 ký tự. Khóa hàng theo thứ tự, quyết định duy nhất mỗi phiên bản, chống bản cũ, xung đột và bấm lặp. Có hàng đợi, hồ sơ đã xử lý, bản chụp và quyết định cũ.
- Lý do từ chối chỉ xem được sau đúng mật khẩu; sai mật khẩu/đoán email chỉ báo lỗi chung. Không tạo SecurityContext ứng dụng cho hồ sơ chờ/từ chối. Đổi session ID khi cấp bằng chứng mật khẩu.
- Bằng chứng mật khẩu 15 phút. Đăng ký lại cần OTP gửi đến chính email tài khoản; OTP 5 phút, tối đa 5 lần sai, cooldown ít nhất 60 giây và giữ quota theo tài khoản/email/IP hiện có. Gửi lại ở nút cùng luồng, không sửa email qua luồng này.
- Sau OTP hợp lệ: ticket 15 phút chỉ sửa họ tên, điện thoại, lĩnh vực và giới thiệu. Không bind email, vai trò, trạng thái, người duyệt hoặc trường quản trị. Gửi lại tạo phiên bản mới, giữ nguyên bản và lý do cũ. Ticket cũ không tạo thêm phiên bản khi gửi lặp.
- CSRF, DTO và allowlist được giữ. Form mới loại HTTP header khỏi binding đúng cơ chế Spring hiện có. Intent đăng ký phía server 30 phút, email duy nhất, thao tác lặp không tạo thêm tài khoản.
- Bỏ khả năng tự thêm vai trò qua hồ sơ; form hồ sơ giữ vai trò hiện tại và service chặn tự cấp thêm vai trò. RoleObligations và việc kiểm tra nghĩa vụ khi bỏ vai trò trong service vẫn nguyên.
- Đích nội bộ từ khám phá vẫn giữ trong session; chỉ dùng allowlist route chi tiết cũ, không nhận URL ngoài.
- Duyệt chuyên gia độc lập duyệt dịch vụ: còn phải có dịch vụ chính chủ được duyệt và công khai theo PublicCatalogPolicy mới xuất hiện trong khám phá.

## Migration và dữ liệu thật

V9 `V9__Expert_applications.sql` đã áp dụng thành công trên **BIZ04_Test và BIZ04**; checksum **894853000**. Không sửa V1–V8 hoặc checksum đã chạy.

- Thêm cột nullable `NguoiDung.NguonDangKy`, chỉ gán cho đăng ký mới. Không cập nhật/backfill tài khoản cũ.
- Thêm `HoSoChuyenGia`, `PhienBanHoSoChuyenGia`, `QuyetDinhChuyenGia`; FK, unique phiên bản/thao tác/quyết định và check trạng thái/lý do.
- Mở rộng check mục đích OTP thêm `DANG_KY_LAI_CG`; không thay khóa hay cơ chế mã hóa.
- Ba bảng mới trên BIZ04 thật vẫn **0 bản ghi**. Không đăng ký/duyệt/xác thực người dùng thật để thử.
- Đối chiếu toàn bộ cột có trước V9 của **26 bảng nghiệp vụ/hệ thống cũ** bằng số dòng và SHA-256: **không đổi**. Flyway đối chiếu riêng, V1–V8 giữ nguyên.
- ID 3: hoạt động, chuyên gia; ID 4: chuyên gia/chờ tiếp quản; ID 5: hai cờ khách hàng và chuyên gia/chờ tiếp quản. Nguồn đăng ký cũ NULL. Không đổi tên/email/khóa/quyền/trạng thái/dịch vụ của các ID này.
- Chuyên gia hiện hữu NULL nguồn và chưa có hồ sơ mới giữ điều kiện quyền cũ. Đây là chuyển tiếp quyền hiện hữu, không ghi quyết định giả. Admin có danh sách phân biệt rõ. ID 3 cùng dịch vụ hợp lệ tiếp tục hiển thị theo chính sách cũ; tài khoản mẫu/chờ tiếp quản vẫn không được tự công khai.

## Kiểm thử thực tế

- **350 ca H2 đạt**: hồi quy đầy đủ; sau đó chạy lại nhóm chuyên gia/đề nghị thuê cho sửa đổi cuối. Một fixture cũ dùng thao tác tự cấp vai trò đã được đổi thành fixture legacy đa vai trò trực tiếp trong test; kiểm tra nghĩa vụ vẫn giữ nguyên.
- **172 ca SQL Server riêng biệt đạt**, BIZ04_Test qua SafeTestDatabase: chuyên gia 29, OTP tài khoản 5, hồ sơ 22, khám phá 19, đề nghị thuê 25, xác lập mốc 43, hợp đồng 29. Không tính lặp cùng ca giữa các lượt.
- Kiểm thử form Thymeleaf được render thật rồi gửi qua MockMvc, gồm session, CSRF, hidden intent/ticket và tên nút quyết định. Đã kiểm tra đăng ký khách hàng/chuyên gia, sai trường/giả quyền, email/điện thoại/giới thiệu, OTP sai/hết hạn/gửi lỗi/tiếp tục/đổi email trước xác thực, chờ duyệt không login, sai mật khẩu không lộ trạng thái, admin duyệt, từ chối → OTP → ticket → phiên bản mới, giữ lịch sử, ticket hết hạn/dùng lại, tranh chấp hai admin/bản cũ, không tự công khai dịch vụ và giữ quyền legacy.
- Edge headless: trang chọn, form khách hàng và chuyên gia ở **1440/768/390px**, cả bản render test lẫn JAR thật (9 bố cục mỗi lượt), không tràn ngang hoặc đưa nút ra ngoài khung. Đã xem ảnh màn 390px.
- JAR thật: **11 route** trả đúng HTTP/chuyển hướng, các trang admin/ticket không có quyền chuyển về đăng nhập. Trang chủ và video còn nguyên; video Range trả **206**.
- Build cuối thành công. Lần đầu Windows giữ tệp JAR đang chạy; đã dừng đúng ứng dụng, đóng gói lại thành công rồi mới xác nhận bản mới. Không có build/migration đang dở.
- Hash xác nhận 19 tệp/tài nguyên được bảo vệ giữ nguyên, gồm home/layout/about/video, CSS chung, RoleObligations, application.properties và V1–V8. Không sửa nghiệp vụ đề xuất/hợp đồng/mốc/tiền/tranh chấp.

### Phần chưa kiểm chứng / còn lại

- Không gửi OTP/email thật hoặc thử xét duyệt dữ liệu thật. OTP trong test dùng mail gateway giả để giữ mã trong bộ nhớ, không in ra log.
- Môi trường khởi chạy hiện không thấy cấu hình SMTP ở Process/User. Không tự đổi cấu hình SMTP hoặc khóa. Gửi OTP thật cần cấu hình SMTP hợp lệ qua công cụ hiện có nếu đang báo không gửi được; phần này chưa kiểm chứng với hộp thư thật.
- Chưa triển khai chuyển khách hàng hiện hữu sang chuyên gia. Bước sau cần đồng ý rõ ràng, kiểm tra RoleObligations, giữ ID/lịch sử và không bỏ sót nghĩa vụ. Không tự áp dụng đợt này.
- Còn chờ quyết định tiếp quản/xử lý ID 4/5; không tự gỡ vai trò kép, chuyển chính thức hoặc duyệt bài mẫu.

## Bằng chứng, sao lưu và chạy lại

- Sao lưu: `.work-backup/expert-registration-20261104/` (nguồn trước sửa, manifest hash, JAR cũ).
- Bằng chứng: `.run/expert-registration-20261104/` — source-scope.json, h2-final-results.json, sql-initial-results.json, sql-final-results.json, sql-combined-results.json, live-http.json, runtime.json, database-after.txt, database-preservation.json, legacy-before/after.txt, ảnh responsive và log build/start.
- Thư mục upload hiện có vẫn `du-lieu/tep`, không thêm upload trong lượt này.
- Khi cổng 8081 trống, tại Maven root chạy: `powershell -NoProfile -ExecutionPolicy Bypass -File .\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081`.
- Nếu build lại trên Windows, dừng đúng JAR ứng dụng trước để tránh khóa tệp; không dừng hàng loạt Java. Không chạy lại hoặc sửa migration đã áp dụng.

## Danh sách tệp nguồn/test thay đổi hoặc bổ sung

- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/domain/NguoiDung.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/security/AccountSessionFilter.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/security/SecurityConfig.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/Access.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/AccountService.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/OtpService.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/ProjectDiscovery.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/PublicCatalogPolicy.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/web/AccountWeb.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/web/DiscoveryRegistrationAdvice.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/web/Format.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/admin.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/profile.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/register.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/AccountOtpTest.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/AccountSqlServerTest.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/HireOfferTest.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/ProfileFormBindingTest.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/domain/HoSoChuyenGia.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/domain/PhienBanHoSoChuyenGia.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/domain/QuyetDinhChuyenGia.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/security/ExpertLoginFailure.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/ExpertApplications.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/ExpertEligibility.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/service/ExpertRegistrationForm.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/java/vn/biz04/web/ExpertRegistrationWeb.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/db/migration/V9__Expert_applications.sql`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/static/css/expert-admission.css`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-edit.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-queue.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-register.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-retry-otp.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-review.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/expert-status.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/main/resources/templates/register-choice.html`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/ExpertRegistrationSqlServerTest.java`
- `C:/Users/user/Downloads/BIZ04_Marketplace_v1/BIZ04_Marketplace/src/test/java/vn/biz04/ExpertRegistrationTest.java`
