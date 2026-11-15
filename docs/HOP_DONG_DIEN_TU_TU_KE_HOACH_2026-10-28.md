# Sang Connect — kế hoạch mốc đến hợp đồng điện tử

Phạm vi mới thay phần chỉ xác lập mốc trước đây. Dùng tiếp HopDong, MocNghiemThu, PaymentService, DeliveryService, TerminationService, DisputeService, ReviewService; không có hợp đồng hoặc hệ thống tiền song song.

## Quy trình sử dụng

1. Khách hàng và chuyên gia thống nhất đề nghị thuê; hoặc khách hàng chọn đề xuất. Nguồn vẫn là hai luồng riêng, không tạo YeuCau/DeXuat giả cho thuê trực tiếp.
2. Vào Không gian làm việc → Kế hoạch mốc (`/ke-hoach-moc`), hoặc liên kết ở chi tiết nguồn. Route nguồn: `/ke-hoach-moc/thue/{id}` và `/ke-hoach-moc/de-xuat/{id}`.
3. Chuyên gia chọn Thiết lập mốc, chia 1–10 mốc, điền công việc/sản phẩm/tiêu chí/hạn/tiền/số lần sửa. Rà soát → gửi. Khách hàng chấp nhận hoặc chỉnh sửa có lý do để gửi lại. Hai bên luân phiên; giữ mọi phiên bản và định danh mốc. Không lưu nháp.
4. Sau thống nhất, khách hàng chọn **Rà soát và lập hợp đồng** tại `{route kế hoạch}/hop-dong`. Kiểm tra đúng hồ sơ A/B, trung gian, mốc/phí; điền điều khoản cần rà soát → Rà soát bản cuối → Xác nhận lập hợp đồng. GET và bước rà soát chưa ghi hợp đồng.
5. Hai bên mở `/hop-dong/{id}`, chọn **Rà soát và xác nhận hợp đồng điện tử**, đọc và xác nhận cuối cho chính mình. Phải cùng phiên bản và dấu kiểm tra nội dung. Việc lập hợp đồng hoặc thống nhất mốc không thay xác nhận này.
6. Sau đủ hai xác nhận, khách hàng ký quỹ mô phỏng mốc đầu. **Ký quỹ thành công mốc đầu tiên là sự kiện bắt đầu đã được người dùng chọn.** Trước đó không gán ngày bàn giao thực tế. Thất bại không bắt đầu; thử lại không đổi ngày hoặc ghi tiền trùng.
7. Chuyên gia bàn giao; khách hàng nghiệm thu hoặc yêu cầu sửa trong giới hạn đã chốt. Chỉ tất toán sau giao dịch thành công rồi mới ký quỹ mốc tiếp. Chấm dứt/tranh chấp/đánh giá dùng nghiệp vụ hiện có.

Admin xem qua Không gian làm việc/đường dẫn có quyền, không xác nhận thay A/B. Lịch sử bất biến: `/hop-dong/{id}/phien-ban/{version}`. Khách hàng có thể đề xuất sửa điều khoản trước hiệu lực qua `/hop-dong/{id}/dieu-khoan`; chỉ thay trách nhiệm/điều khoản bổ sung/đại diện khai báo, không thay mốc/giá/nguồn/phạm vi. Phiên bản mới vô hiệu xác nhận cũ, giữ bằng chứng lịch sử. Hủy trước hiệu lực có rà soát và lý do tại `/hop-dong/{id}/huy-dien-tu`.

## Thời hạn và tiền

- 40 ngày chia 15 ngày → 15, 30, 40. Tự chia dùng hạn cộng dồn theo ngày, không giảm; mốc cuối đúng thời lượng thuê.
- Tháng lịch lưu quy tắc tương đối MONTH_CAPPED_PROJECT_END; lấy ngày bắt đầu cộng N tháng, thiếu ngày thì lấy cuối tháng. Không đổi một tháng thành 30 ngày. Các mốc trước không vượt ngày kết thúc; mốc cuối luôn ngày bắt đầu + đúng số ngày đã thống nhất. Khung gợi ý dùng các ranh giới tháng chắc chắn trong thời lượng thuê rồi thêm mốc cuối, tối đa 10 mốc; người dùng phải rà soát. Chưa ký quỹ đầu thì chưa có lịch thực tế.
- Ngày bắt đầu lưu UTC; hạn bàn giao là 23:59 ngày tương ứng ở Việt Nam, lưu UTC theo cơ chế hiện có. Thử lại/restart không tính lại sự kiện đã ghi.
- Phí snapshot 10%, HALF_UP trên từng phần phân bổ cho chuyên gia, không tính lại trên tổng, không thu phần hoàn khách, không cộng vào tiền ký quỹ. Ví dụ ký quỹ 2 triệu, phân bổ chuyên gia 40% → hoàn 1,2 triệu, phí 80.000, chuyên gia nhận 720.000.
- Không giải ngân theo ngày/tháng. **Giữ chính sách sẵn có:** tự nghiệm thu sau 14 ngày kể từ bàn giao khi còn chờ nghiệm thu, hợp đồng đang thực hiện và không có tranh chấp mở. Không thêm chính sách im lặng mới.

## Thông tin và dữ liệu

Người vận hành/đại diện được cấu hình qua `BIZ04_CONTRACT_OPERATOR_NAME`, `BIZ04_CONTRACT_OPERATOR_PHONE` và `BIZ04_CONTRACT_OPERATOR_REPRESENTATIVE`; bản hợp đồng đã lập giữ snapshot. Không tự khai pháp nhân/MST/chức danh hoặc lấy admin hiện tại làm trung gian.

Snapshot lấy đúng hồ sơ A/B, chỉ đọc bốn số CCCD cuối đã lưu để che; không giải mã CCCD. Không đòi thêm giấy tờ/định danh không cần thiết. Tổ chức có thể khai tên/chức vụ đại diện, không gắn nhãn xác minh pháp lý. Hồ sơ thay đổi sau khi lập không làm thay snapshot. Nếu hồ sơ/cấu hình đổi giữa rà soát và tạo thì yêu cầu rà soát lại.

V7 mở rộng chính HopDong/MocNghiemThu, bổ sung quy tắc hạn kế hoạch, hai bảng BanChupHopDong/XacNhanHopDong. Nguồn cũ giữ FK; nguồn thuê đi qua MaKeHoach có kiểm tra nguồn/phiên bản/party ở server. V8 đổi UNIQUE mã đề xuất sang filtered unique cho giá trị khác NULL, cho phép nhiều hợp đồng thuê trực tiếp nhưng vẫn cấm hai hợp đồng cùng đề xuất thật. V7 đã chạy trên BIZ04_Test nên giữ nguyên, bổ sung V8 riêng. Không chuyển đổi hợp đồng cũ hoặc tính lại phí. V1–V6 giữ nguyên. Backup `.work-backup/econtract-20261028/` và `.run/econtract/source-changes.json` ghi các tệp thay đổi.

CSRF/DTO/allowlist được giữ; header kỹ thuật không bind vào DTO. Ticket gắn actor, nguồn, phiên bản, nội dung ở server, hết hạn sau 30 phút. Khóa user theo thứ tự ID rồi nguồn/kế hoạch/hợp đồng; tạo lại mở cùng hợp đồng. Nghĩa vụ mở chuyển sang hợp đồng, không bỏ vai trò khi còn nghĩa vụ hoặc tiền giữ.

## Kiểm chứng

Kết quả cuối, JAR/tiến trình/cổng và đối chiếu dữ liệu: `.run/econtract/verification.json`. Báo cáo chi tiết H2/SQL/Edge nằm trong `.run/econtract/`. Không tạo/chấp thuận/hủy giao dịch kiểm thử trong database BIZ04 thật. Database kiểm thử riêng có SafeTestDatabase.

Chưa có xuất PDF trong ứng dụng nên không thêm. Đây là xác nhận hợp đồng điện tử trên nền tảng, chưa tích hợp chữ ký số/chứng thư bên ngoài. Thanh toán chỉ mô phỏng. Không gửi OTP hoặc email trong kiểm thử thật.

## Chạy lại

Từ thư mục Maven `C:\Users\user\Downloads\BIZ04_Marketplace_v1\BIZ04_Marketplace`:

```powershell
.\chay.ps1 -WindowsAuth -SkipBuild -AppPort 8081
```

Không chạy thêm khi cổng đang có đúng ứng dụng; kiểm tra tiến trình trước khi dừng. Thư mục upload hiện có: `du-lieu/tep` (hoặc BIZ04_UPLOAD_DIR nếu cấu hình); không thay đổi thư mục hoặc khóa CCCD/OTP.

## Các tệp triển khai

- `src/main/java/vn/biz04/service/PaymentService.java` (changed)
- `src/main/java/vn/biz04/service/DeliveryService.java` (changed)
- `src/main/resources/application.properties` (changed)
- `src/main/resources/templates/milestone-detail.html` (changed)
- `src/main/java/vn/biz04/domain/PhienBanKeHoachMoc.java` (changed)
- `src/main/resources/templates/milestone-parts.html` (changed)
- `src/main/java/vn/biz04/service/MilestonePlanService.java` (changed)
- `src/main/resources/static/js/milestone-plan.js` (changed)
- `src/main/java/vn/biz04/service/ContractService.java` (changed)
- `src/main/resources/templates/hire-detail.html` (changed)
- `src/main/java/vn/biz04/domain/HopDong.java` (changed)
- `src/main/java/vn/biz04/service/RoleObligations.java` (changed)
- `src/main/java/vn/biz04/service/MilestonePlanRules.java` (changed)
- `src/main/java/vn/biz04/service/DisputeService.java` (changed)
- `src/main/java/vn/biz04/service/Access.java` (changed)
- `src/main/resources/templates/contract.html` (changed)
- `src/main/resources/templates/milestone-editor.html` (changed)
- `src/main/java/vn/biz04/web/Web.java` (changed)
- `src/main/java/vn/biz04/service/CatalogDiscovery.java` (changed)
- `src/main/java/vn/biz04/domain/MocNghiemThu.java` (changed)
- `src/main/java/vn/biz04/web/MilestoneLinks.java` (changed)
- `src/main/resources/templates/milestone-review.html` (changed)
- `src/main/java/vn/biz04/data/Store.java` (changed)
- `src/main/java/vn/biz04/web/MilestonePlanWeb.java` (changed)
- `src/main/java/vn/biz04/domain/BanChupHopDong.java` (added)
- `src/main/java/vn/biz04/domain/XacNhanHopDong.java` (added)
- `src/main/java/vn/biz04/service/ContractOperator.java` (added)
- `src/main/java/vn/biz04/service/ContractSnapshots.java` (added)
- `src/main/java/vn/biz04/service/ElectronicContractForm.java` (added)
- `src/main/java/vn/biz04/web/ElectronicContractWeb.java` (added)
- `src/main/resources/templates/electronic-cancel.html` (added)
- `src/main/resources/templates/electronic-error.html` (added)
- `src/main/resources/templates/electronic-form.html` (added)
- `src/main/resources/templates/electronic-history.html` (added)
- `src/main/resources/templates/electronic-parts.html` (added)
- `src/main/resources/templates/electronic-review.html` (added)
- `src/main/resources/db/migration/V7__hop_dong_dien_tu_tu_ke_hoach.sql` (added)
- `src/main/resources/db/migration/V8__hop_dong_nguon_de_xuat_nullable_unique.sql` (added)
- `src/main/resources/static/css/electronic-contract.css` (added)

Kiểm thử bổ sung trong ElectronicContractTest/ElectronicContractSqlServerTest; các fixture hợp đồng cũ ở TestContracts chỉ thuộc test scope, không có ngoại lệ cho dữ liệu thật.
