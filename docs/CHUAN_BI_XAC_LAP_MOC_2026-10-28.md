# Chuẩn bị bước xác lập mốc trước hợp đồng

Ngày 28/10/2026, người dùng giao hoàn tất lỗi gửi đề nghị thuê đang sửa và **chuẩn bị** bước sau khi hai bên thỏa thuận, trước khi xác lập hợp đồng. Tài liệu này ghi nhận đối chiếu mã và phương án nối tiếp; chưa bổ sung schema, màn hình mốc hoặc tạo hợp đồng trong lần sửa form.

## Điểm xuất phát đã có

- `HireOfferService.accept` khóa tài khoản và đề nghị, kiểm tra đúng bên đang có lượt/phiên bản hiện tại, rồi ghi `DA_THONG_NHAT`, `agreed`, người và thời điểm quyết định. Snapshot `PhienBanDeNghiThue` giữ nguyên. Nhãn hiện tại: “Đã thống nhất — Chờ chốt mốc nghiệm thu”.
- `RoleObligations` coi đề nghị đã thống nhất là nghĩa vụ mở. Giữ quy tắc này trong giai đoạn chờ mốc.
- `MocNghiemThu.maHopDong` và FK SQL Server hiện bắt buộc có hợp đồng. `ContractService.milestone` chỉ cho khách hàng sửa hợp đồng `CHO_XAC_NHAN`; mỗi thay đổi tăng phiên bản và xóa xác nhận hai bên.
- `HopDong.maYeuCau` và `maDeXuat` đều NOT NULL. `ContractService.select` chỉ nhận đề xuất `DA_CHON`. `Access.customer/expert` tìm các bên qua yêu cầu/đề xuất. Chưa có đường tạo hợp đồng trực tiếp từ `DeNghiThue`.
- Các query dashboard, nghĩa vụ vai trò và thống kê hợp đồng hoàn thành cũng đang nối qua yêu cầu/đề xuất. Cần rà soát chúng khi làm bước nối hợp đồng; chỉ thêm nút tạo là chưa đủ.

## Luồng dự kiến cho bước kế tiếp

`Đề nghị đã thống nhất` → `Lập/rà soát bản mốc` → `Hai bên xác nhận cùng phiên bản mốc` → `Đủ điều kiện xác lập hợp đồng`.

Phương án khởi đầu: khách hàng lập bản mốc, chuyên gia rà soát/xác nhận hoặc yêu cầu sửa. Mỗi bản gắn đúng ID đề nghị và phiên bản thuê đã thống nhất; lấy người thao tác từ Principal. Mỗi mốc gồm thứ tự, tên, nội dung/sản phẩm, tiêu chí nghiệm thu, số tiền VND nguyên và hạn bàn giao. Ít nhất một mốc; thứ tự không trùng; tiền từng mốc dương, tổng bằng giá đã thống nhất. Phương án hạn bàn giao cụ thể cần thống nhất với thời gian thực hiện của đề nghị trước khi triển khai.

Sửa mốc tạo phiên bản mới, giữ lịch sử và hủy xác nhận cũ. Hai bên phải xem và xác nhận cùng bản mới nhất. POST dùng ticket phía server, kiểm tra quyền/phiên bản và khóa lại bản hiện tại; retry không tạo trùng. Người ngoài và admin không được xác nhận thay các bên. Dữ liệu client không quyết định actor, trạng thái hoặc bên đối tác.

Trong giai đoạn này, lưu **bản kế hoạch mốc trước hợp đồng** gắn với đề nghị, không tạo sớm `MocNghiemThu` dưới một hợp đồng giả. Không sinh `YeuCau`/`DeXuat` giả để đáp ứng FK; không ký quỹ, tạo giao dịch hoặc kích hoạt hợp đồng khi chốt mốc.

## Ranh giới dữ liệu cần thiết kế trước khi viết chức năng

1. Bổ sung nơi lưu kế hoạch mốc và các snapshot/xác nhận trước hợp đồng, độc lập với bảng mốc vận hành hiện tại. Đây là đề xuất mở rộng, không phải schema đã tồn tại.
2. Chỉ khi làm bước hợp đồng: thiết kế nguồn hợp đồng rõ ràng cho hai chiều nghiệp vụ, liên kết duy nhất đề nghị/phiên bản kế hoạch đã chốt, cập nhật cách xác định các bên và mọi query liên quan. Chuyển bản mốc đã chốt sang mốc hợp đồng trong cùng transaction có khóa và chống tạo trùng.
3. Giữ 12 bảng/106 thuộc tính/21 FK cơ sở trong tài liệu ERD gốc; ghi phần mở rộng riêng. Kiểm tra lịch sử Flyway thực tế trước khi chọn migration mới (hiện nguồn có V1–V5); không sửa migration đã chạy.
4. Giữ cơ chế hợp đồng, xác nhận cùng phiên bản, ký quỹ từng mốc, thanh toán mô phỏng, tranh chấp và đánh giá hiện có. Không dựng hệ thống hợp đồng thứ hai.

## Bộ kiểm thử cần có khi triển khai

- Chỉ đề nghị đã thống nhất mở được bước mốc; đúng hai bên, đúng snapshot thuê; GET không ghi quyết định.
- Lập/sửa/rà soát/xác nhận bằng HTML form có CSRF; thử tiền lẻ/âm, tổng lệch, thứ tự trùng, hạn thiếu và trường quản trị giả mạo.
- Một bên xác nhận rồi sửa lại phải mất xác nhận cũ; phiên bản cũ hoặc ticket khác người không được chốt.
- Hai bên xác nhận đồng thời, bấm lại và lưu đồng thời chỉ chốt một bản hợp lệ; không có hợp đồng/giao dịch tiền ở bước này.
- Mở lại trang vẫn thấy đúng bản/lịch sử; hai bên nhìn cùng nội dung; nghĩa vụ vai trò không bị bỏ qua.
- Kiểm thử trên H2 và BIZ04_Test; đối chiếu dữ liệu BIZ04 chỉ đọc; không dùng đề nghị thật để thử.

Điểm tiếp tục: triển khai bước kế hoạch mốc theo luồng trên sau bản sửa form đã kiểm chứng. Việc nối hợp đồng là bước sau, cần xử lý đầy đủ các phụ thuộc đã liệt kê.
