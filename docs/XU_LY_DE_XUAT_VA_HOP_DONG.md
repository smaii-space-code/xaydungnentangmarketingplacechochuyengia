# Xem và xử lý đề xuất, tiếp tục lập hợp đồng

Hoàn tất ngày 27/10/2026, đã build và chạy lại Sang Connect tại http://localhost:8081.

## Chức năng và cách thử

1. Chuyên gia gửi đề xuất qua luồng Tìm dự án → chi tiết yêu cầu → gửi/cập nhật đề xuất hiện có.
2. Khách hàng mở `/bang-dieu-khien` để xem ô Đề xuất nhận được hoặc `/yeu-cau` để xem số lượng theo từng yêu cầu. Nút Xem đề xuất đưa tới phần đề xuất của đúng yêu cầu.
3. Danh sách nhận: `/de-xuat/nhan`. Danh sách đã gửi của chuyên gia: `/de-xuat/da-gui`. Chi tiết riêng một đề xuất: `/de-xuat/{id}`.
4. Từ chối/Chấp thuận mở `/de-xuat/{id}/xac-nhan?hanhDong=tu-choi` hoặc `chap-thuan`. GET chỉ đọc; người dùng xác nhận bằng POST có CSRF.
5. Sau chấp thuận, chuyển tới `/de-xuat/{id}/lap-hop-dong`. Quyết định đã lưu nhưng chưa tạo hợp đồng. Có thể thoát rồi quay lại bằng Tiếp tục lập hợp đồng.
6. Khách rà soát giá, phương án và các bên lấy từ dữ liệu đã lưu, rồi POST Xác nhận tạo hợp đồng. Giá/ID các bên bị sửa trên client không được dùng. Nếu đã tạo thì mở đúng hợp đồng cũ, không tạo thêm.
7. Hợp đồng mới ở CHO_XAC_NHAN, phiên bản 1, chưa có xác nhận hai bên và chưa tự tạo mốc. Khách thiết lập/rà soát nội dung, tổng giá trị và các mốc qua trang hợp đồng hiện có; hai bên phải xác nhận cùng phiên bản. Không tự ký, kích hoạt, ký quỹ hoặc giải ngân.

Đề xuất cũ được đọc trực tiếp qua quan hệ DeXuat → YeuCau → MaKhachHang. Không dùng người gửi làm điều kiện nhận; không bắt gửi lại. Thời điểm gửi lấy lần GUI_DE_XUAT mới nhất trong nhật ký; bản ghi cũ thiếu nhật ký hiển thị dấu —, không bịa ngày.

Thống kê gửi/nhận dùng chính các danh sách tương ứng, gồm mọi trạng thái đã lưu. Đề xuất rút/từ chối/đã lập hợp đồng không biến mất khỏi lịch sử. Tài khoản có cả hai vai trò vẫn có hai số liệu riêng; không sửa menu hoặc cơ chế vai trò.

## Ánh xạ trạng thái

| Dữ liệu thực tế | Hiển thị |
|---|---|
| DeXuat.DANG_GUI, chưa có hợp đồng | Đã đề xuất, chữ xanh đậm/nền xanh nhạt |
| DeXuat.BI_TU_CHOI, chưa có hợp đồng | Đề xuất bị từ chối, chữ đỏ đậm/nền đỏ nhạt |
| DeXuat.DA_CHON, chưa có hợp đồng | Được chấp thuận — Chờ hợp đồng |
| Có HopDong liên kết thực tế với đề xuất | Đã lập hợp đồng; trạng thái hợp đồng hiển thị riêng |
| DeXuat.DA_RUT, chưa có hợp đồng | Đã rút đề xuất |
| HopDong.CHO_XAC_NHAN | Hợp đồng đã tạo, chờ hai bên xác nhận cùng phiên bản |

Kiểm tra quan hệ hợp đồng trước nhãn đề xuất, kể cả bản ghi cũ đã hủy hợp đồng và DeXuat đang BI_TU_CHOI. Không sửa lịch sử để ép trạng thái.

Chấp thuận dùng lại DA_CHON, đồng thời đưa yêu cầu sang DANG_THUONG_LUONG. Không thêm trạng thái trùng nghĩa. Chỉ một chuyên gia được chọn cho mỗi yêu cầu: đây là quy tắc đã có trong ContractService và ràng buộc unique của HopDong.MaYeuCau/MaDeXuat. Quyết định và tạo hợp đồng cùng khóa người dùng theo ID tăng dần rồi khóa yêu cầu, đọc mới đề xuất.

Chính sách có sẵn tự từ chối các đề xuất khác được giữ tại thời điểm tạo hợp đồng, không chạy ngay khi chấp thuận. Chỉ đổi các đề xuất còn DANG_GUI sang BI_TU_CHOI; giữ nguyên DA_RUT/BI_TU_CHOI và nội dung lịch sử. Gửi lại sau rút/từ chối vẫn theo CatalogService hiện có, không thêm nút hoàn tác. Backend ngăn đóng yêu cầu đang giữ quyết định chấp thuận chưa lập hợp đồng.

## Quyền và dữ liệu

Chỉ chủ yêu cầu có quyền hiện tại mới từ chối/chấp thuận/tạo hợp đồng. Chuyên gia xem đề xuất của chính mình. ADMIN giữ quyền đọc hợp lệ, không được xử lý thay chủ yêu cầu chỉ nhờ quyền ADMIN. Khách hàng khác không thể xem hoặc xử lý bằng đổi ID. Các thao tác POST kiểm tra lại trạng thái sau khi khóa; cùng thao tác lặp không ghi quyết định hoặc hợp đồng trùng.

Các danh sách/chi tiết dùng DTO thông tin công khai, không đưa email, điện thoại, CCCD hoặc ciphertext vào nội dung đề xuất. Không thêm email/chat/WebSocket/polling; tải lại là thấy trạng thái mới.

Không có migration mới: đã có trạng thái DA_CHON, quan hệ đề xuất/yêu cầu/hợp đồng, nhật ký và ràng buộc unique hợp đồng cần thiết. Không sửa entity hoặc migration V1/V2/V3.

## Tệp thay đổi

Backend:
- src/main/java/vn/biz04/service/ProposalService.java — dữ liệu nhận/gửi, nhãn theo quan hệ hợp đồng, đếm, quyền, quyết định và khóa dùng chung.
- src/main/java/vn/biz04/service/ContractService.java — tạo hợp đồng từ đề xuất đã chấp thuận; lặp trả về hợp đồng cũ; giữ luồng phiên bản/xác nhận.
- src/main/java/vn/biz04/service/CatalogService.java — chặn đóng yêu cầu đã giữ quyết định chấp thuận.
- src/main/java/vn/biz04/web/ProposalWeb.java — danh sách, chi tiết, xác nhận, POST quyết định và rà soát/tạo hợp đồng.
- src/main/java/vn/biz04/web/Web.java — số liệu dashboard, đếm theo yêu cầu, danh sách đề xuất được phép xem.

Giao diện:
- src/main/resources/templates/dashboard.html
- src/main/resources/templates/requests.html
- src/main/resources/templates/request-detail.html
- src/main/resources/templates/proposals.html — fragment danh sách đầy đủ và bảng dashboard.
- src/main/resources/templates/proposal-list.html
- src/main/resources/templates/proposal-detail.html
- src/main/resources/templates/proposal-confirm.html
- src/main/resources/templates/proposal-contract.html
- src/main/resources/static/css/proposals.css — chỉ phần đề xuất/số thống kê đề xuất; bảng cuộn ngang trong khung trên điện thoại.

Kiểm thử:
- src/test/java/vn/biz04/ProposalDecisionTest.java
- src/test/java/vn/biz04/ProposalDecisionSqlServerTest.java
- src/test/java/vn/biz04/BusinessFlowTest.java, SqlServerIntegrationTest.java, AccountOtpTest.java — chỉ cập nhật fixture tạo hợp đồng qua bước chấp thuận và assertion chống tạo trùng.

Bản sao lưu trước sửa: `.work-backup/proposal-decisions-20261027/`. Dự án không có Git. Đối chiếu hash trong `.run/proposal-scope-audit.json` xác nhận trang chủ/menu/layout/trang giới thiệu/ảnh/video/migration/OTP/hồ sơ/CCCD/vai trò và các service thanh toán/đánh giá/tranh chấp giữ nguyên.

## Kết quả kiểm chứng

- Bộ mặc định H2: **102 ca đạt, 0 lỗi**; 36 ca SQL mặc định bỏ qua, chỉ chạy riêng khi bật biến môi trường và BIZ04_Test.
- SQL Server riêng: **36 ca đạt, 0 lỗi, 0 bỏ qua**, gồm 15 ca xử lý đề xuất, 14 ca Tìm dự án, 5 ca tài khoản và 2 ca giao dịch/khóa có sẵn. Chạy trên BIZ04_Test, Microsoft SQL Server 16.0, Windows Authentication.
- Có kiểm tra GET không đổi dữ liệu; POST cần CSRF; chủ sở hữu/chuyên gia/ADMIN; thống kê gửi/nhận riêng; rút/từ chối không được chấp thuận; chấp thuận giữ bền trước tạo hợp đồng; không tự ký/khởi tạo mốc; liên kết hợp đồng cũ; bấm lặp; chấp thuận/từ chối đồng thời; chấp thuận/rút đồng thời; hai chấp thuận khác nhau và hai tạo hợp đồng đồng thời.
- Sau chỉnh bảng mobile đã chạy thêm hai ca render trang liên quan. `mvn -DskipTests package` thành công; chạy lại đúng JAR trên cổng 8081.
- Edge: **46 màn hình** ở 1440/768/390 px, không tràn trang/đè nút; form POST có CSRF, nhãn từ chối đúng màu đỏ. Đã xem ảnh và chỉnh bảng mobile để chữ không bị bó hẹp; bảng cuộn ngang được trong khung, liên kết cuối bảng vẫn truy cập được.
- Màn hình quyết định và hợp đồng dùng HTML do MockMvc render từ database test. Các kiểm tra BIZ04 thật chỉ đăng nhập và GET: dashboard, Yêu cầu của tôi, danh sách nhận/gửi và trang xác nhận trước quyết định.
- Trạng thái dữ liệu thật trước/sau giống nhau: đề xuất mã 1 vẫn DANG_GUI, hợp đồng và giao dịch vẫn 0. Không tự từ chối/chấp thuận/tạo hợp đồng trên BIZ04 thay người dùng.

Phần chưa thực hiện trên dữ liệu thật: POST từ chối/chấp thuận/tạo hợp đồng; các bước ghi này được kiểm chứng trên H2 và SQL Server BIZ04_Test. Không còn quy tắc cần người dùng chốt thêm: giữ một chuyên gia/một hợp đồng trên mỗi yêu cầu và chính sách từ chối còn lại đã có.

Bằng chứng: `.run/proposal-all-tests.log`, `.run/proposal-sql-tests.log`, `.run/proposal-ui-tests.log`, `.run/proposal-package-final.log`, `.run/proposal-production-before.txt`, `.run/proposal-production-after.txt`, `.run/proposal-scope-audit.json`, `.run/proposal-qa/after/report.json`.

Ảnh: [nhận đề xuất mobile, dữ liệu test](../.run/proposal-qa/after/test-received-390.png), [rà soát hợp đồng mobile, dữ liệu test](../.run/proposal-qa/after/test-review-390.png), [dashboard khách hàng thật](../.run/proposal-qa/after/live-customer-bang-dieu-khien-390.png), [bảng đã cuộn tới cuối trên điện thoại](../.run/proposal-qa/after/live-table-scrolled-390.png).
