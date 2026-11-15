# Khám phá chuyên gia và dịch vụ Sang Connect

Hoàn tất 28/10/2026. Build và chạy tại `http://localhost:8081` từ JAR `target/marketplace-1.0.0.jar` của chính dự án này. Tiến trình Java sau khởi động: **16908**, bind `127.0.0.1:8081`. Đối chiếu nội dung template/CSS/class bên trong JAR với nguồn và lớp vừa biên dịch: `.run/catalog-build-verification.json`.

## Đường dẫn và dữ liệu

| Trang | Route |
| --- | --- |
| Trang chủ có video | `/` |
| Tìm chuyên gia — nhóm chuyên gia | `/trang/trangchu#chuyen-gia` |
| Tìm chuyên gia — nhóm dịch vụ | `/trang/trangchu#dich-vu` |
| Hồ sơ chuyên gia để xem | `/chuyen-gia/{id}` |
| Chi tiết dịch vụ duy nhất | `/dich-vu/xem/{id}` |
| Hồ sơ tài khoản để chỉnh sửa, giữ nguyên | `/ho-so` |
| Tìm dự án dành cho chuyên gia, giữ nguyên | `/tim-du-an` |

`/dich-vu` tiếp tục chuyển đến route danh sách hiện có, giữ từ khóa như trước. Không tạo hệ thống hồ sơ/dịch vụ hoặc trang chi tiết thứ hai.

- Chuyên gia: `NguoiDung.LaChuyenGia=true`, `TrangThai=HOAT_DONG`. Mỗi người một bản ghi, không join nhân bản theo số dịch vụ. Hồ sơ độc lập chưa có dịch vụ vẫn được liệt kê. Chính sách cũ không có cờ xuất bản hồ sơ riêng.
- Chuyên ngành: các `DichVu.LinhVuc` khác nhau của chính chuyên gia, chỉ dịch vụ `DANG_HIEN`. Cùng nguồn danh mục cho hai bộ kết quả. Không suy diễn chức danh hoặc kỹ năng.
- Giới thiệu: tái dùng `HoSoKhachHang.GioiThieu` của tài khoản đó, là trường giới thiệu sẵn có; không sửa form tài khoản. DTO công khai không chứa email, điện thoại, địa chỉ, CCCD, ciphertext, mật khẩu, OTP hoặc thông tin quản trị.
- Dịch vụ: join `DichVu.MaChuyenGia` đến người cung cấp còn hoạt động, còn vai trò chuyên gia; chỉ `DANG_HIEN`. Hồ sơ lọc **`d.maChuyenGia=:viewedExpert`**, không lấy theo người đăng nhập. Chi tiết xác định chuyên gia từ **dịch vụ được mở**, bỏ qua ID chủ sở hữu gửi riêng từ client.
- Khung phải lọc cùng `MaChuyenGia`, loại ID dịch vụ đang xem, tối đa **4** mục. “Xem tất cả dịch vụ” mở đúng hồ sơ đó tại `#dich-vu`.
- Đánh giá: thống kê và nội dung chỉ từ `DanhGia.TrangThai=CONG_KHAI`. Không kích hoạt công khai trong truy vấn; không đọc nội dung `DANG_AN`/`BI_AN` để hiển thị. Giữ nguyên Blind Review.
- Công việc hoàn thành: đếm hợp đồng `HOAN_THANH` qua đề xuất của chuyên gia; không đếm dịch vụ là công việc hoàn thành. Chỉ hiển thị khi có dữ liệu lớn hơn 0.

## Tìm kiếm, phân trang, dữ liệu thiếu

- Từ khóa tối đa 200 ký tự; tìm tên chuyên gia, giới thiệu, tên/mô tả dịch vụ tương ứng. Escape ký tự wildcard của LIKE, bind tham số.
- `linhVuc` tối đa 100 ký tự và phải thuộc tập danh mục công khai hiện tại. Tham số không hợp lệ trả 400.
- `trangChuyenGia` và `trangDichVu`: số nguyên 1–100000, **6 mục/phần/trang**, cố định ở server. ID giảm dần cho thứ tự ổn định. COUNT và OFFSET/LIMIT tại database; không tải toàn bộ thực thể rồi phân trang ở trình duyệt.
- Liên kết trang giữ từ khóa, chuyên ngành và trang hiện tại của phần còn lại. Form tìm kiếm mới không gửi hai số trang nên về trang 1. Trang vượt kết quả được kẹp về trang cuối hợp lệ.
- Hồ sơ: dịch vụ và đánh giá cũng phân trang server, 6 mục. Trang chủ chỉ lấy tối đa **3 chuyên gia + 3 dịch vụ**; danh mục chip và “Xem tất cả” giữ chuyên ngành. Tìm kiếm trang chủ giữ đích tìm dự án cho chuyên gia như trước.
- Schema chưa có ảnh chuyên gia, ảnh dịch vụ, chức danh, kỹ năng, giá giờ hay thời gian gói riêng: dùng avatar chữ, không thêm ảnh hoặc dữ liệu giả; không thêm trường/migration. Chuyên ngành trống thì không gán nhãn giả.
- Không có giới thiệu: “Chuyên gia chưa bổ sung giới thiệu.” Không có đánh giá: “Chưa có đánh giá”. Không có dịch vụ/dịch vụ khác: thông báo đúng yêu cầu. Có xóa bộ lọc và trạng thái kết quả rỗng. Hồ sơ/dịch vụ không tồn tại, bị ẩn hoặc chủ sở hữu không đủ điều kiện: 404, không thay bằng người khác.

## Đăng ký và đích quay lại

- Khách xem trang chủ/video, giới thiệu, tìm kiếm/lọc/phân trang bình thường. Hai endpoint chi tiết được chặn bằng Spring Security, không chỉ bằng href.
- HTML chi tiết khi chưa đăng nhập chuyển `/dang-ky?chiTiet=1`; yêu cầu JSON đến cùng endpoint trả **401**, không trả dữ liệu chi tiết. Hiện dự án không có API chi tiết song song.
- Trang đăng ký từ luồng này hiện “Đăng ký tài khoản để xem chi tiết chuyên gia và dịch vụ.” và “Đã có tài khoản? Đăng nhập”.
- Session chỉ lưu đường dẫn đúng mẫu `/chuyen-gia/{id}` hoặc `/dich-vu/xem/{id}`, ID dương, không query/hash. Không nhận tham số `next` tùy ý. Từ chối URL ngoài, `//domain`, traversal, route quản trị và đường dẫn mã hóa lạ.
- Luồng đăng ký/OTP cũ giữ nguyên. Sau đăng nhập, kiểm tra lại đích còn công khai, chuyển đến đó và xóa đích một lần. Đích không hợp lệ hoặc đã bị ẩn dùng `/bang-dieu-khien`. Các endpoint khác tiếp tục chuyển đăng nhập như trước.
- Chi tiết đọc lại trạng thái tài khoản từ database; session của tài khoản đã khóa không mở được chi tiết. Tài khoản cũ `CHUYEN_TIEP` còn hoạt động vẫn đăng nhập được. Quyền xem không cấp vai trò hoặc quyền tạo yêu cầu; nút tạo yêu cầu chỉ hiện cho khách hàng khác chủ dịch vụ, nghiệp vụ POST và CSRF cũ giữ nguyên.

## Tệp nguồn thay đổi

| Tệp | Thay đổi |
| --- | --- |
| `src/main/java/vn/biz04/service/CatalogDiscovery.java` | Projection, lọc, phân trang, thống kê công khai, truy vấn đúng chủ |
| `src/main/java/vn/biz04/web/DiscoveryWeb.java` | Route đọc trang chủ/danh sách/hồ sơ/dịch vụ |
| `src/main/java/vn/biz04/web/Web.java` | Chuyển bốn handler khám phá sang controller trên; giữ nghiệp vụ khác |
| `src/main/java/vn/biz04/security/DiscoveryLoginFlow.java` | Chặn khách, đích quay lại giới hạn và login success |
| `src/main/java/vn/biz04/security/SecurityConfig.java` | Bảo vệ đúng hai nhóm endpoint chi tiết, giữ entry point đăng nhập ở phần còn lại |
| `src/main/java/vn/biz04/web/DiscoveryRegistrationAdvice.java` | Cờ thông báo đăng ký, không sửa AccountWeb |
| `src/main/resources/templates/services.html` | Hai nhóm thẻ, bộ lọc và phân trang riêng |
| `src/main/resources/templates/expert.html` | Hồ sơ xem và dịch vụ/đánh giá đúng người |
| `src/main/resources/templates/service-detail.html` | Nội dung trái, chuyên gia và dịch vụ khác phải |
| `src/main/resources/templates/discovery-cards.html` | Thẻ chuyên gia/dịch vụ và phân trang dùng chung |
| `src/main/resources/templates/home.html` | Chỉ thay phần khám phá dưới video |
| `src/main/resources/templates/register.html` | Thông báo theo luồng chi tiết và liên kết đăng nhập |
| `src/main/resources/static/css/discovery.css` | CSS giới hạn `.discovery-page`, không sửa selector toàn cục |
| `src/test/java/vn/biz04/CatalogDiscoveryTest.java` | 19 ca dữ liệu, quyền, OTP/đích quay lại, trạng thái và render |
| `src/test/java/vn/biz04/CatalogDiscoverySqlServerTest.java` | Chạy lại 19 ca trên BIZ04_Test có chốt chặn database |
| `src/test/java/vn/biz04/BusinessFlowTest.java` | Cập nhật kỳ vọng hồ sơ chuyên gia cần đăng nhập |

Không có Git. Bản sao lưu trước sửa: `.work-backup/catalog-discovery-20261027/`. Hash kiểm toán phạm vi: `.run/catalog-scope-audit.json`.

Đã đối chiếu và giữ nguyên khối hero trang chủ, video, poster, home.js, phần hợp tác, layout/menu/footer, giới thiệu/ảnh founder, CSS chung, AccountWeb, toàn bộ các service OTP/hồ sơ/vai trò/đề xuất/hợp đồng/thanh toán/tranh chấp và các migration.

## Kết quả thực tế

- **121 kiểm thử H2 đạt**, sau đó kiểm tra cuối 3 ca render/trang chủ có bộ lọc/menu chuyên gia cũng đạt.
- **55 kiểm thử SQL Server đạt** trên **BIZ04_Test**: khám phá 19, tìm dự án 14, quyết định đề xuất 15, tài khoản 5, nghiệp vụ tích hợp 2. Không reset database người dùng. Kết quả từng lớp lưu `.run/catalog-test-results.json`.
- Đã kiểm tra người B xem hồ sơ A chỉ có dịch vụ A; dịch vụ khác không lẫn chủ/không chứa dịch vụ hiện tại; ẩn/nháp/tài khoản khóa; đánh giá ẩn; tóm tắt không nhúng chi tiết; trang độc lập; GET/POST quyền/CSRF; đăng ký → OTP → đăng nhập và đích quay lại an toàn.
- Build `mvn -q -DskipTests package` thành công sau kiểm thử. Khởi động lại đúng JAR bằng `chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081`.
- **43 ảnh/trạng thái Edge** ở **1440, 768, 390px**: test fixtures từ H2 và bản đang chạy (trang chủ, danh sách, đăng ký từ chi tiết, hồ sơ, dịch vụ, dashboard, form tài khoản, giới thiệu, tìm dự án). Không tràn chiều rộng trang hoặc chồng nút; lưới 3/2/1 cột; chi tiết 2 cột ở 1440 và 1 cột ở 768/390. Bảng dashboard giữ vùng cuộn ngang cũ bên trong, không gây tràn body.
- Video phát thật: `paused=false`, `readyState=4`, thời gian phát tăng, không lỗi. Sau login thực tế quay lại `/dich-vu/xem/2`. Trên BIZ04 chỉ GET và đăng nhập, không POST đăng ký, lưu hồ sơ, quyết định đề xuất, tạo hợp đồng hoặc giao dịch.
- SHA-256 và số bản ghi của **18 bảng BIZ04 trước/sau giống nhau**, gồm `flyway_schema_history`. Dữ liệu thật được giữ nguyên. Dấu vân tay không chứa nội dung cá nhân: `.run/catalog-database-before.txt`, `.run/catalog-database-after.txt`.
- Skill `design-taste-frontend`: đọc tại `.agents/skills/design-taste-frontend/SKILL.md`; dùng Framed sheet, seed **1160398099**, giữ màu/font/header theo giới hạn người dùng. Lint **0 FAIL/0 WARN**; inspector danh sách **0 FAIL/2 WARN**, hồ sơ và dịch vụ **0 FAIL/0 WARN**. Đã mở tất cả 18 ảnh inspector cuối. Hai cảnh báo danh sách được giữ có lý do: nhóm thứ hai nằm dưới nhóm đầu có phân trang theo yêu cầu; thẻ thiếu giới thiệu vẫn giữ đáy nút đều với thẻ có giới thiệu, không bịa thêm nội dung. Nhận xét thiết kế trong `.taste-skill/catalog-critique.md`.

Ảnh thực tế: `.run/catalog-qa/after/live-public-home-1440.png`, `live-public-listing-390.png`, `live-customer-service-1440.png`. Toàn bộ số đo và kết quả trình duyệt: `.run/catalog-qa/after/report.json`.

## Phần còn thiếu/chưa kiểm chứng

- Tệp yêu cầu không kèm ảnh tham khảo riêng, nên không xác nhận được độ giống ảnh Upwork cụ thể; thực hiện theo mô tả bố cục và thương hiệu hiện có.
- Chưa có trường/tài nguyên ảnh chuyên gia/dịch vụ hoặc kỹ năng/chức danh riêng, nên không thể hiện các dữ liệu đó. Avatar chữ và dữ liệu giới thiệu/chuyên ngành thật đã xử lý được phần độc lập, không thêm migration.
- Kiểm thử OTP dùng mail gateway giả lập ở database test; không gửi mail thật hoặc xác nhận khả năng nhận thư SMTP thật trong lần này.
- Chỉ kiểm tra Edge trên máy hiện tại; chưa kiểm chứng Safari/iOS và thiết bị vật lý.
