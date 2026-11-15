# Đề nghị thuê và thương lượng Sang Connect

Hoàn tất ngày 28/10/2026. Ứng dụng đã build và chạy lại đúng JAR của dự án trên `http://localhost:8081`, Java PID **14180**, bind `127.0.0.1:8081`.

## Phạm vi đã thực hiện

Khách hàng gửi đề nghị trực tiếp đến chuyên gia; chuyên gia chấp thuận, điều chỉnh hoặc từ chối. Khách hàng nhận điều chỉnh có thể chấp thuận, điều chỉnh lại hoặc từ chối. Các bên tải lại trang để thấy phản hồi; không thêm email, polling, WebSocket, chat hoặc thông báo đẩy.

Luồng mới độc lập với `YeuCau`/`DeXuat`. Các handler và service của đề xuất/hợp đồng cũ được giữ nguyên. Nút “Tạo yêu cầu từ dịch vụ” cũ vẫn còn; bổ sung nút “Gửi đề nghị thuê” để vào chiều thương lượng mới. Hồ sơ chuyên gia cũng có nút này cho khách hàng hợp lệ khác chính mình.

Luồng mới dừng ở **“Đã thống nhất — Chờ chốt mốc nghiệm thu”**. Có thông báo bước chốt mốc chưa được triển khai, không có liên kết đến trang tương lai. Không lập/ký hợp đồng, tạo mốc, ký quỹ, thanh toán hoặc giải ngân từ đề nghị thuê.

## Route

| Mục đích | Route |
| --- | --- |
| Mở form gửi đến chuyên gia | GET `/de-nghi-thue/gui/chuyen-gia/{expertId}` |
| Điền sẵn từ dịch vụ | GET route trên với `?dichVu={serviceId}` |
| Gửi bản đầu tiên | POST `/de-nghi-thue/gui` |
| Khách hàng xem bản đã gửi | GET `/de-nghi-thue/da-gui` |
| Chuyên gia xem bản nhận được | GET `/de-nghi-thue/nhan` |
| Admin tra cứu, chỉ đọc | GET `/de-nghi-thue/tat-ca` |
| Xem chi tiết và lịch sử | GET `/de-nghi-thue/{id}` |
| Xem bản xác nhận chấp thuận | GET `/de-nghi-thue/{id}/chap-thuan?phienBan={n}` |
| Mở form điều chỉnh | GET `/de-nghi-thue/{id}/dieu-chinh?phienBan={n}` |
| Rà soát điều chỉnh, chưa đổi database | POST `/de-nghi-thue/{id}/dieu-chinh/ra-soat` |
| Mở form lý do từ chối | GET `/de-nghi-thue/{id}/tu-choi?phienBan={n}` |
| Rà soát lý do, chưa lưu quyết định | POST `/de-nghi-thue/{id}/tu-choi/ra-soat` |
| Lưu quyết định đã rà soát | POST `/de-nghi-thue/{id}/xac-nhan` |

Không thêm mục menu chính. Không gian làm việc có các liên kết riêng **“Đề nghị thuê đã gửi”**, **“Đề nghị thuê nhận được”** và số lượng; tài khoản hai vai trò có cả hai phần rõ ràng. Lịch sử đã gửi/nhận vẫn có liên kết nếu tài khoản bỏ vai trò sau khi hết nghĩa vụ. Các danh sách không trộn với đề xuất tìm dự án.

Danh sách phân trang server 12 bản ghi, thứ tự ngày cập nhật rồi ID giảm dần. Lịch sử phân trang 10 phiên bản và luôn hiển thị riêng phiên bản hiện tại; không bỏ mất bản cũ.

## Migration và dữ liệu

Thêm **`V4__de_nghi_thue_va_phien_ban.sql`**, không sửa V1/V2/V3, không reset database.

- **DeNghiThue**: khách hàng, chuyên gia, dịch vụ nguồn tùy chọn, mã gửi chống lặp, tiêu đề, trạng thái, phiên bản hiện tại, bên chờ phản hồi, phiên bản thống nhất, người quyết định/thời điểm/lý do từ chối, ngày gửi/cập nhật.
- **PhienBanDeNghiThue**: liên kết đề nghị, số phiên bản, tác giả, mã gửi thao tác, phạm vi, sản phẩm mong muốn, giá, số ngày, giải thích điều chỉnh, thời điểm gửi. Snapshot chỉ thêm mới; entity dùng `@Immutable`.
- FK giữ liên kết tới tài khoản/dịch vụ và lịch sử. Unique `(MaKhachHang,MaGui)`, `(MaDeNghi,SoPhienBan)`, `(MaDeNghi,MaGui)`. CHECK cho hai bên khác nhau, trạng thái/phiên bản/quyết định, giá dương và ngày 1–730. Chỉ thêm hai bảng mở rộng; không sửa 12 bảng/106 thuộc tính/21 FK trong ERD gốc.
- Thời gian lưu **DATETIMEOFFSET(6), UTC**; hiển thị bằng formatter hiện có theo **Asia/Ho_Chi_Minh**.
- Dịch vụ nguồn chỉ điền sẵn tiêu đề/phạm vi/giá. Sản phẩm mong muốn và số ngày để khách hàng nhập vì dịch vụ chưa có các trường riêng đó. Khách hàng được sửa nội dung đề nghị trước khi gửi; giá gói không được coi là hợp đồng đã ký.

V4 đã chạy thành công trên **BIZ04_Test**, sau đó trên **BIZ04** khi khởi động JAR mới. Flyway hiện ở phiên bản 4. Dữ liệu cũ không được cập nhật để phục vụ luồng mới.

## Ánh xạ trạng thái và phiên bản

| Trạng thái lưu | Điều kiện | Nhãn hiển thị |
| --- | --- | --- |
| `CHO_PHAN_HOI` | Phiên bản 1, chờ chuyên gia | Chờ chuyên gia phản hồi |
| `CHO_PHAN_HOI` | Chuyên gia vừa gửi bản n>1, chờ khách hàng | Chuyên gia đề nghị điều chỉnh — Chờ khách hàng phản hồi |
| `CHO_PHAN_HOI` | Khách hàng vừa gửi bản n>1, chờ chuyên gia | Chờ chuyên gia phản hồi điều chỉnh |
| `DA_THONG_NHAT` | Đúng bên nhận chấp thuận phiên bản hiện tại | Đã thống nhất — Chờ chốt mốc nghiệm thu |
| `BI_TU_CHOI` | Đúng bên nhận từ chối có lý do | Bị từ chối |

Tái dùng tên trạng thái chờ/từ chối phù hợp trong aggregate riêng; không đổi trạng thái `DeXuat` để biểu diễn đề nghị thuê. Trạng thái chờ dùng màu vàng nhạt, thống nhất xanh lá, từ chối đỏ và luôn có chữ.

Mỗi điều chỉnh thêm số phiên bản mới và chuyển bên chờ sang đối tác. Bên vừa gửi không tự chấp thuận. Tiêu đề và quan hệ hai bên/dịch vụ nguồn được giữ nguyên; điều chỉnh chỉ đổi giá, ngày, phạm vi và sản phẩm, có giải thích bắt buộc. Màn rà soát hiển thị nội dung hiện tại và bản sẽ gửi, kèm nhãn trường thay đổi.

Chấp thuận lưu `PhienBanDaThongNhat`, người chấp thuận và thời điểm. Từ chối lưu người, thời điểm và lý do; chấm dứt thương lượng, không mở lại/rút/hoàn tác, không xóa lịch sử.

## Quyền, chống lặp và đồng thời

- Lấy người gửi từ Principal và tài khoản còn hoạt động trong database. Bên nhận/dịch vụ nguồn được kiểm tra khi mở form rồi giữ trong intent ở session; POST không lấy ID người gửi/người nhận từ các trường client.
- Khi gửi, khóa tài khoản hai bên theo ID tăng dần, đọc lại trạng thái/vai trò; với dịch vụ nguồn, khóa và kiểm tra lại đúng chủ, còn `DANG_HIEN`. Chặn tự thuê, nguồn sai chủ/ẩn, người gửi không có quyền khách hàng và người nhận không còn chuyên gia.
- Nội dung: tiêu đề 5–255, phạm vi 10–10000, sản phẩm 5–5000, giải thích/lý do 5–2000 ký tự. Giá nguyên dương, tối đa 1.000 tỷ VND; thời gian 1–730 ngày.
- Quyết định khóa hai tài khoản rồi đề nghị, đọc mới dữ liệu, kiểm tra đúng bên đang chờ, trạng thái và phiên bản. Chấp thuận/từ chối chạy đồng thời chỉ một kết quả hợp lệ.
- Màn xác nhận lưu ticket ở session ràng buộc actor, ID, phiên bản, loại thao tác và nội dung đã rà soát. POST cuối chỉ dùng ticket đó; sửa hidden giá/lý do/phiên bản không thay nội dung server đã giữ. Nếu phiên bản cũ, yêu cầu tải lại, không tự chấp thuận bản mới.
- Mã gửi cho bản đầu và từng điều chỉnh kết hợp khóa/unique giúp bấm lặp không tạo bản trùng. Gửi lại cùng quyết định thành công không thêm audit hoặc quyết định lần nữa; hai thao tác khác nhau không được ghi đè nhau.
- GET chỉ xem và giữ dữ liệu form trong session. Gửi, rà soát và xác nhận đều POST có CSRF; mở “Không chấp thuận” chỉ mở lựa chọn bên trong trang. Hủy trở về chi tiết, không ghi quyết định.
- Chỉ hai bên và admin đang hoạt động xem được chi tiết. Admin ngoài hai bên chỉ đọc; người ngoài đổi ID bị chặn. Không hiển thị email riêng, mật khẩu, CCCD, ciphertext hoặc OTP.
- Mở rộng **RoleObligations** đúng quan hệ khách hàng/chuyên gia: `CHO_PHAN_HOI` và `DA_THONG_NHAT` chặn bỏ vai trò tương ứng; `BI_TU_CHOI` không còn chặn chỉ vì bản ghi đó. Dùng cùng khóa tài khoản với cập nhật hồ sơ để tránh bỏ vai trò đồng thời với tạo đề nghị.

## Cách thử ba hướng

1. Đăng nhập khách hàng A. Mở hồ sơ chuyên gia B hoặc chi tiết dịch vụ của B, bấm **Gửi đề nghị thuê**, rà soát và nhập đủ sản phẩm/thời gian rồi gửi. A xem tại “Đề nghị thuê đã gửi”; B đăng nhập xem tại “Đề nghị thuê nhận được”.
2. **Chấp thuận:** B mở chi tiết → Chấp thuận → kiểm tra phiên bản và nội dung → Xác nhận chấp thuận. Hai bên thấy trạng thái thống nhất chờ chốt mốc; chưa có hợp đồng hoặc tiền.
3. **Điều chỉnh:** B chọn Không chấp thuận → Đề nghị điều chỉnh → sửa giá/ngày/phạm vi/sản phẩm và giải thích → Rà soát thay đổi → Xác nhận gửi. A mở bản mới, chấp thuận điều chỉnh hoặc điều chỉnh lại để B phản hồi tiếp. Mở lịch sử để đối chiếu từng phiên bản/tác giả/thời điểm.
4. **Từ chối:** bên đang có lượt chọn Không chấp thuận → Từ chối đề nghị thuê → nhập lý do → Rà soát → Xác nhận từ chối. Cả hai thấy nhãn đỏ và lý do, lịch sử còn nguyên, không còn thao tác thương lượng.

## Kiểm thử và triển khai thực tế

- **146 kiểm thử H2 đạt**, gồm **25 ca mới** của `HireOfferTest` và hồi quy OTP/hồ sơ/khám phá/tìm dự án/đề xuất/hợp đồng/thanh toán/tranh chấp.
- **80 kiểm thử SQL Server đạt** trên database riêng **BIZ04_Test**, gồm cùng **25 ca đề nghị thuê**. Các ca khóa thật kiểm tra tạo/trùng điều chỉnh, chấp thuận và từ chối đồng thời, bỏ vai trò khách hàng/chuyên gia đồng thời với tạo đề nghị. Chốt chặn test từ chối URL database thật.
- Kiểm tra thêm lịch sử 12 phiên bản có phân trang, đúng chủ sở hữu/dịch vụ nguồn, dữ liệu bị ẩn trước submit, giả mạo trường đối tác, ticket sai URL hoặc phiên bản cũ, người ngoài/admin chỉ đọc, bắt buộc lý do, bước rà soát không quyết định, CSRF, các bản đã từ chối không mở lại.
- Đã build `mvn -q -DskipTests package`, khởi động đúng JAR bằng `chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081`. Đối chiếu 17 entry class/template/CSS/migration trong JAR với output vừa build: `.run/hire-build-verification.json`.
- **45 ảnh/trạng thái fixture test** trên Edge tại **1366×768**, **1920×1080**, thêm **390×844**. Không tràn body/chồng nút, các POST form có CSRF; trạng thái từ chối màu đỏ. Form, gửi/nhận, lựa chọn không chấp thuận, rà soát hai cột, thống nhất/từ chối và dashboard đã được chụp/đo.
- **16 ảnh trên ứng dụng thật** ở hai kích thước máy tính: dashboard khách hàng/chuyên gia, danh sách gửi/nhận rỗng, form nguồn điền sẵn, hồ sơ/dịch vụ có CTA, tìm dự án. Chỉ GET và đăng nhập demo hiện có, **không gửi/chấp thuận/từ chối đề nghị trong BIZ04**. Video phát thật, thời gian tăng, không có lỗi; menu công khai vẫn 3 mục.
- Đối chiếu sau khởi động: dấu vân tay SHA-256 và số bản ghi của **17 bảng dữ liệu cũ giống trước**; hai bảng mới **0 bản ghi**. Flyway thêm đúng bản ghi V4, lịch sử hiện 4 migration. Không tự tạo đề nghị, phiên bản, hợp đồng hay giao dịch thật để thử.
- Lint CSS/template mới: 0 FAIL/0 WARN. Inspector màn rà soát ở 1366/1920: 0 FAIL, 2 WARN. Đã mở cả 4 ảnh inspector. Cảnh báo khoảng trống trong cột “Nội dung hiện tại” được giữ để hai bản so sánh có đáy đều; cảnh báo rộng canvas ở 1920 do layout chung giới hạn 1440 và căn giữa, giữ theo yêu cầu không sửa shell/header/footer.

Kết quả/log: `.run/hire-test-results.json`, `hire-all-tests.log`, `hire-sql-tests.log`, `hire-package.log`, `hire-scope-audit.json`, `hire-database-comparison.json`. Ảnh và số đo: `.run/hire-qa/after/`, `.run/hire-qa/live/`. Backup trước sửa: `.work-backup/hire-offers-20261028/`.

## Tệp thêm/sửa

Thêm 17 tệp:

- `src/main/java/vn/biz04/domain/DeNghiThue.java`
- `src/main/java/vn/biz04/domain/PhienBanDeNghiThue.java`
- `src/main/java/vn/biz04/service/HireOfferForm.java`
- `src/main/java/vn/biz04/service/HireOfferService.java`
- `src/main/java/vn/biz04/web/HireOfferWeb.java`
- `src/main/java/vn/biz04/web/HireWorkspaceAdvice.java`
- `src/main/resources/db/migration/V4__de_nghi_thue_va_phien_ban.sql`
- `src/main/resources/static/css/hire.css`
- `src/main/resources/templates/hire-form.html`
- `src/main/resources/templates/hire-detail.html`
- `src/main/resources/templates/hire-list.html`
- `src/main/resources/templates/hire-edit.html`
- `src/main/resources/templates/hire-reject.html`
- `src/main/resources/templates/hire-confirm.html`
- `src/main/resources/templates/hire-parts.html`
- `src/test/java/vn/biz04/HireOfferTest.java`
- `src/test/java/vn/biz04/HireOfferSqlServerTest.java`

Sửa 5 tệp hiện có:

- `src/main/java/vn/biz04/service/RoleObligations.java`: thêm nghĩa vụ của luồng thuê.
- `src/main/java/vn/biz04/web/DiscoveryWeb.java`: cờ hiển thị CTA khách hàng trên hồ sơ chuyên gia.
- `src/main/resources/templates/expert.html`: thêm CTA thuê.
- `src/main/resources/templates/service-detail.html`: thêm CTA thuê bên cạnh luồng yêu cầu cũ.
- `src/main/resources/templates/dashboard.html`: liên kết gửi/nhận/tra cứu riêng, giữ các panel cũ.

Trang chủ/video/poster, layout/menu/footer, giới thiệu/ảnh founder, CSS chung và CSS khám phá, security/OTP/hồ sơ/CCCD, các service đề xuất/hợp đồng/tiền/tranh chấp và V1–V3 không sửa. Có kiểm toán hash phạm vi.

## Phần chưa triển khai/chưa kiểm chứng

- Chốt mốc, tạo/ký hợp đồng, ký quỹ và thanh toán từ đề nghị thuê **chưa triển khai đúng giới hạn yêu cầu**. Bản đã thống nhất vẫn là nghĩa vụ mở cho tới bước tiếp theo được bổ sung sau này.
- Không thử quyết định bằng dữ liệu thật; các trạng thái đầy đủ và đồng thời được kiểm chứng trên H2/BIZ04_Test. Bản đang chạy chỉ kiểm tra đọc/form/liên kết/đăng nhập.
- Chưa kiểm tra Safari/Firefox hoặc thiết bị vật lý; kiểm tra trình duyệt lần này bằng Edge.
- Intent và ticket rà soát giữ tối đa 32 mục trong session 60 phút hiện có. Hết session cần mở lại form/rà soát; đề nghị và phiên bản đã gửi vẫn lưu SQL Server và mở lại từ danh sách.
