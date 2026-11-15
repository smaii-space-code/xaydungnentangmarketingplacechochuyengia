# Menu theo vai trò và Tìm dự án

Hoàn tất ngày 27/10/2026, build và chạy lại đúng Sang Connect tại http://localhost:8081.

## Route và menu

| Mục | Route |
|---|---|
| Logo / Trang chủ | `/` — trang chủ có video |
| Tìm chuyên gia | `/trang/trangchu` — danh sách dịch vụ hiện có |
| Tìm dự án | `/tim-du-an` — yêu cầu đang nhận đề xuất, chỉ chuyên gia |
| Về Sang Connect | `/ve-sang-connect` |
| Yêu cầu của tôi | `/yeu-cau` — chỉ yêu cầu do chính tài khoản đăng |
| Không gian làm việc | `/bang-dieu-khien` |
| Quản trị | `/quan-tri` — giữ menu/quyền quản trị hiện có |

Khách chưa đăng nhập: đúng Trang chủ, Tìm chuyên gia, Về Sang Connect; bên phải Đăng nhập/Đăng ký.

Khách hàng: Trang chủ, Tìm chuyên gia, Về Sang Connect, Yêu cầu của tôi, Không gian làm việc; bên phải lời chào/Đăng xuất.

Chuyên gia: Trang chủ, Tìm dự án, Về Sang Connect, Không gian làm việc; bên phải lời chào/Đăng xuất.

ADMIN: giữ Trang chủ, Tìm chuyên gia, Về Sang Connect, Yêu cầu công việc, Không gian làm việc, Quản trị. Không dùng ADMIN làm quyền bỏ qua kiểm tra vai trò chuyên gia của route mới.

Người dùng đã xác nhận không có tài khoản dùng cả hai vai trò; không cần chốt thêm menu hai vai trò trong lượt này. Không thay cơ chế cập nhật vai trò hoặc dữ liệu hồ sơ. Các cờ vai trò hiện hữu vẫn được giữ, không xóa quyền hoặc lịch sử.

Các nút tìm kiếm chính trên trang chủ của chuyên gia dẫn đến `/tim-du-an`, dùng từ khóa tiêu đề/mô tả yêu cầu. Với khách truy cập/khách hàng/ADMIN, giữ tìm dịch vụ. Video, poster, khẩu hiệu và cấu trúc trang chủ giữ nguyên.

## Điều kiện dữ liệu và nghiệp vụ

Tìm dự án dùng bảng YeuCau hiện có. Chỉ `DANG_MO` là trạng thái còn nhận đề xuất trong luồng hiện tại. Chủ yêu cầu phải HOAT_DONG và còn vai trò khách hàng; tài khoản chờ xác thực/bị khóa không xuất hiện. Loại yêu cầu của chính người xem ra khỏi danh sách.

Yêu cầu độc lập mở cho chuyên gia hợp lệ. Yêu cầu gắn một dịch vụ chỉ hiện với chuyên gia đăng dịch vụ đó, đúng quy tắc gửi đề xuất hiện có. Không tự đổi nghĩa trạng thái hoặc tạo luồng công khai riêng. DA_DONG, DANG_THUONG_LUONG và các trạng thái khác không lọt vào danh sách.

Schema không có cột lĩnh vực/ngày đăng riêng cho YeuCau. Lĩnh vực lấy từ dịch vụ liên kết nếu có; ngày đăng lấy thời điểm đầu tiên trong nhật ký DANG_YEU_CAU của chính yêu cầu. Nếu thiếu dữ liệu thì hiển thị Chưa cung cấp, không suy đoán ngày hoặc lĩnh vực. Ngày giờ dùng formatter giờ Việt Nam hiện có.

Tìm kiếm tiêu đề/mô tả, lọc lĩnh vực và ngân sách; kiểm tra độ dài, số nguyên, khoảng ngân sách và số trang ở backend. Sắp xếp ngày đăng mới trước, ID giảm dần khi cùng ngày hoặc thiếu ngày. Phân trang 9 thẻ/trang, giữ q/linhVuc/tu/den trong URL. Có trạng thái không kết quả, lỗi bộ lọc và Xóa bộ lọc.

DTO chỉ có dữ liệu công khai: tiêu đề/mô tả, lĩnh vực, ngân sách, hạn dự kiến, ngày đăng, tên khách hàng/tổ chức và cờ đã gửi đề xuất. Không đưa email, điện thoại, CCCD hoặc ciphertext vào thẻ/API. Tên tổ chức được dùng khi hồ sơ là TO_CHUC và có tên; nếu không dùng họ tên hiển thị.

Chi tiết và gửi/cập nhật đề xuất tiếp tục dùng `/yeu-cau/{id}` và CatalogService.proposal. Giữ khóa tài khoản/chính yêu cầu, kiểm tra lại DANG_MO và chủ dịch vụ lúc gửi; bổ sung kiểm tra khách hàng còn đủ điều kiện. Gửi lại khi đang DANG_GUI cập nhật chính bản ghi hiện có, không tạo bản ghi trùng. DA_RUT/BI_TU_CHOI giữ quy tắc gửi lại sẵn có; DA_CHON không được ghi đè. Lịch sử đề xuất của người dùng vẫn xem được theo quan hệ sở hữu, kể cả khi không còn quyền gửi mới.

## Tệp sửa/thêm

- src/main/java/vn/biz04/service/ProjectDiscovery.java: truy vấn cơ hội, DTO công khai, bộ lọc, phân trang và metadata từ dữ liệu có sẵn.
- src/main/java/vn/biz04/web/ProjectWeb.java: route /tim-du-an, quyền thực tế và lỗi bộ lọc.
- src/main/java/vn/biz04/service/CatalogService.java: kiểm tra khách hàng còn hoạt động/còn vai trò khi nhận đề xuất.
- src/main/java/vn/biz04/web/Global.java: cờ chọn hành vi tìm kiếm theo vai trò hiện tại.
- src/main/java/vn/biz04/web/Web.java: danh sách yêu cầu của chính người dùng và quyền/metadata chi tiết.
- src/main/resources/templates/layout.html: menu theo từng nhóm, trạng thái active và CSS trang mới.
- src/main/resources/templates/home.html: chỉ đổi đích/nhãn các điều khiển tìm kiếm theo vai trò.
- src/main/resources/templates/requests.html: tiêu đề danh sách cá nhân/ADMIN.
- src/main/resources/templates/request-detail.html: dữ liệu công khai, điều kiện hiển thị form đề xuất.
- src/main/resources/templates/projects.html: trang tìm dự án, thẻ, lọc, phân trang.
- src/main/resources/static/css/projects.css: CSS giới hạn trong project-board.
- src/test/java/vn/biz04/ProjectDiscoveryTest.java: quyền/menu, dữ liệu, bộ lọc, phân trang, riêng tư và khóa đồng thời trên H2.
- src/test/java/vn/biz04/ProjectDiscoverySqlServerTest.java: chạy cùng các ca mới trên BIZ04_Test.

Không sửa Store, entity, schema hoặc migration. Bản sao lưu trước sửa tại `.work-backup/project-discovery-20261027/`. Thư mục hiện không có Git.

## Kiểm tra thực tế

- `mvn test`: 87 ca H2/trang/nghiệp vụ đạt, 0 lỗi. 21 ca SQL được bỏ qua mặc định vì chỉ chạy khi bật biến môi trường riêng.
- Lượt SQL Server riêng: 14 ca ProjectDiscoverySqlServerTest và 2 ca SqlServerIntegrationTest đạt trên BIZ04_Test (Microsoft SQL Server 16.0, Windows Authentication). Có kiểm tra phân trang, từ khóa chứa ký tự %, ngày đăng, dữ liệu nhiều khách hàng, tài khoản khóa, đề xuất đồng thời không trùng và giữ lọc.
- Kiểm tra bổ sung lĩnh vực: cả trường hợp lĩnh vực có thật và chưa cung cấp đều đạt trên H2 và SQL Server.
- `mvn -DskipTests package`: thành công sau kiểm thử; dừng đúng JAR rồi chạy lại qua chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081.
- Edge headless: 38 trạng thái ảnh ở 1440/768/390 px, gồm public/customer/expert/admin, trang chủ/giới thiệu/menu liên quan, Tìm dự án thật, không kết quả, bộ lọc sai và thẻ có phân trang. Không tràn ngang/chồng liên kết/nút.
- Đăng nhập tài khoản thật mẫu, menu đúng thứ tự; khách hàng mở /tim-du-an nhận HTTP 403; chưa đăng nhập được chuyển tới /dang-nhap. Video trang chủ vẫn phát, ảnh người sáng lập và avatar MS giữ nguyên.
- Thẻ nhiều kết quả trong ảnh phân trang dùng HTML do MockMvc render từ BIZ04_Test, để không tạo dữ liệu giả trên BIZ04. Tìm dự án với 1 yêu cầu hiện hữu, lỗi bộ lọc và trạng thái rỗng được kiểm tra trực tiếp trên BIZ04 đang chạy.

Chưa gửi đề xuất bằng tài khoản thật trên BIZ04 trong kiểm tra trình duyệt; các ca gửi/cập nhật/rút/gửi lại/đồng thời được kiểm chứng tự động trên H2 và SQL Server BIZ04_Test. Không gửi email thật hoặc sửa hồ sơ trong lượt này.

Đối chiếu hash xác nhận trang giới thiệu, ảnh chân dung, video, home.js, migration và service OTP/hồ sơ/CCCD/vai trò/hợp đồng/thanh toán/đánh giá/tranh chấp không đổi. Không cần migration mới.

Bằng chứng: `.run/project-all-tests.log`, `.run/project-sql-tests.log`, `.run/project-field-tests.log`, `.run/project-package.log`, `.run/project-scope-audit.json`, `.run/project-qa/after/report.json`.

Ảnh: [Tìm dự án thật desktop](../.run/project-qa/after/expert-projects-1440.png), [Tìm dự án thật mobile](../.run/project-qa/after/expert-projects-390.png), [phân trang dữ liệu kiểm thử](../.run/project-qa/after/expert-projects-cards-1440.png), [menu khách hàng mobile](../.run/project-qa/after/customer-about-390.png).
