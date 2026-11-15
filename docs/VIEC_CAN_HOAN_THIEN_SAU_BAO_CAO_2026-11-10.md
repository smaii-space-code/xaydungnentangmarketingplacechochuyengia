# Sang Connect — Công việc sau phần báo cáo

Ngày rà soát 10/11/2026. Thứ tự người dùng chọn: **tổng hợp → báo cáo theo yêu cầu riêng → hoàn thiện nghiệp vụ**. Chưa triển khai các hạng mục dưới đây trong lượt chuẩn bị báo cáo.

## 1. Phạm vi lõi đã có mã

Tài khoản/OTP/hồ sơ; đăng ký và duyệt chuyên gia; duyệt dịch vụ; khám phá; yêu cầu/đề xuất; đề nghị thuê có phiên bản; kế hoạch mốc; hợp đồng điện tử; ký quỹ mô phỏng; bàn giao/nghiệm thu/sửa; chấm dứt; tranh chấp; phân bổ tiền; đánh giá hai chiều; kiểm tra nghĩa vụ và nhật ký.

Không xây lại bước mốc/hợp đồng từ các ghi chú V4/V6 cũ. Hai luồng nguồn đã nối về cùng hợp đồng. Việc có mã và test đã lưu không thay cho bước kiểm tra bản cuối sau các thay đổi tiếp theo.

## 2. Các việc còn lại và tiêu chí hoàn tất

| Mã | Hạng mục | Hiện trạng đã đối chiếu | Công việc và tiêu chí hoàn tất |
|---|---|---|---|
| HT01 | Báo cáo theo mẫu riêng | Người dùng đã chọn sẽ cung cấp yêu cầu riêng; chưa có bố cục/số chương/thông tin trường và giảng viên. | Dùng bản tổng hợp làm nguồn; nhận mẫu, viết từng chương, bổ sung sơ đồ/ảnh/tài liệu tham khảo; kiểm tra định dạng Word sau khi có yêu cầu. |
| HT02 | Chuyển khách hàng hiện hữu sang chuyên gia | Đăng ký chuyên gia mới đã có; chưa có luồng chuyển tài khoản khách hàng cũ. `ExpertApplications.pending` dành cho đăng ký mới sẽ bỏ cờ khách hàng, nên không áp trực tiếp cho tài khoản đang có nghĩa vụ. | Thiết kế yêu cầu chuyển có sự đồng ý của chủ tài khoản; xác thực email hiện tại khi cần; nộp và duyệt hồ sơ có phiên bản. Khi quyết định chuyển phải khóa và đọc lại tài khoản/nghĩa vụ trong transaction. Giữ ID, lịch sử và lối xem lịch sử khách hàng; không mất nghĩa vụ, không tự cấp admin. Kiểm tra xung đột lúc admin duyệt, gửi lặp, từ chối và gửi lại. |
| HT03 | Gửi/nhận email OTP thật của web | Mã SMTP và kiểm thử giả lập đã có. Tài khoản chuyên gia đã tiếp quản trong lịch sử không chứng minh mọi lượt đăng ký trên phiên web mới đều gửi mail được. | Khởi động web với cấu hình SMTP/khóa hiện có; người dùng tự nhập bí mật trong terminal. Thử có kiểm soát trên email do người dùng kiểm soát, xác nhận nhận thư và hoàn tất đúng luồng. Không đưa OTP/khóa/mật khẩu vào tài liệu hoặc log. |
| HT04 | Dữ liệu mẫu ID 4 còn treo | Kiểm tra BIZ04 ngày 10/11: ID 4 mẫu/chờ tiếp quản; ID 5 không còn; yêu cầu #3 của ID 2 vẫn mở, gắn dịch vụ #2. | Tiếp tục phương án đã được duyệt ở chat trước khi đến bước xử lý dữ liệu: chủ ID 2 đóng yêu cầu qua web; kiểm tra DA_DONG và nhật ký đúng actor, toàn bộ liên kết/nghĩa vụ mới; sao lưu; mới gỡ liên kết và xóa dữ liệu riêng trong transaction. Giữ yêu cầu/lịch sử ID 2 và dữ liệu ID 3. Không tự đóng thay khách hoặc gỡ dịch vụ lúc yêu cầu còn mở. |
| HT05 | Kiểm chứng bản trình diễn cuối | Bằng chứng nhiều đợt H2/BIZ04_Test/Edge đã có; ngày 10/11 web không lắng nghe 8081. | Sau sửa, kiểm thử đúng nhóm bị ảnh hưởng, đóng gói JAR, chạy SQL Server và kiểm tra bản phục vụ. Trình diễn cả nguồn đề xuất và thuê trực tiếp; nhánh thành công, sửa, giao dịch thất bại/thử lại, chấm dứt và tranh chấp. Ghi rõ dữ liệu test; tránh tạo quyết định thay người dùng trong BIZ04. |
| HT06 | Đồng bộ tài liệu và minh chứng | README, trạng thái và tệp tiếp tục còn nhiều đoạn lịch sử; một đường dẫn tổng kiểm chứng hợp đồng bị thiếu. | Dùng mốc tổng hợp mới làm đầu đọc; dẫn bằng chứng có thật, ghi lại phiên bản cuối và giới hạn; chọn ảnh đúng giao diện, che dữ liệu riêng; không cộng số test trùng giữa các đợt. |

HT02 là phần phát triển còn thiếu đã được nhắc trong bàn giao. Trước khi code, cần ghi rõ hành vi chờ xét duyệt của khách hàng cũ và cách xử lý khi có nghĩa vụ mới phát sinh; tuyệt đối không lấy việc nộp hồ sơ làm lý do tự tước quyền đang cần để hoàn thành hợp đồng.

## 3. Chức năng mở rộng chưa thuộc phạm vi lõi đã chốt

Các mục này có thể đưa vào hướng phát triển hoặc bổ sung nếu mẫu/yêu cầu báo cáo xác định cần có. Không xem sự vắng mặt của chúng là bằng chứng luồng hiện tại chưa hoàn thành:

- **Quên/đặt lại mật khẩu:** schema có giá trị mục đích OTP `DAT_LAI_MAT_KHAU`, nhưng chưa có route/service công khai hoàn chỉnh cho tính năng này. Một giá trị enum không đồng nghĩa đã có chức năng.
- **Thông báo kết quả duyệt và tiến độ qua email:** hiện xem kết quả bằng trang trạng thái/đăng nhập và tải lại workspace; chưa có hệ thống email thông báo ngoài OTP.
- **Xuất PDF hợp đồng, chữ ký số bên ngoài:** bản chụp và xác nhận điện tử đã có; không tự đưa hai tích hợp này vào chương kết quả triển khai.
- **Ảnh đại diện chuyên gia, kỹ năng/chức danh riêng, portfolio, chat thời gian thực, tìm kiếm nâng cao:** không thuộc schema/giao diện đã chốt hiện tại.
- **Triển khai Internet, thanh toán tiền thật, ví/rút ngân hàng:** ngoài phạm vi mô phỏng cục bộ hiện hành. Không phát sinh tích hợp này chỉ vì yêu cầu “coi như hoàn thành”.

## 4. Các bất biến phải giữ khi hoàn thiện

1. Bảo toàn dữ liệu thật, các migration đã chạy và ERD cơ sở. Schema mới dùng migration kế tiếp sau khi kiểm tra lịch sử thực tế.
2. Khóa và đọc lại đúng dữ liệu khi xử lý đồng thời; phiên bản/ticket cũ không quyết định thay nội dung mới.
3. Hai bên xác nhận cùng bản hợp đồng; chỉ ký quỹ thành công mốc đầu ghi thời điểm bắt đầu.
4. Tiền mô phỏng VND nguyên; phí trên phần chuyên gia; tất toán/đóng tranh chấp sau đủ giao dịch thành công.
5. Không rò nội dung/điểm đánh giá còn ẩn; không tạo đánh giá mặc định.
6. Vai trò đang có nghĩa vụ không bị bỏ; lịch sử vẫn xem được theo quan hệ thật.
7. Giữ cấu hình khóa CCCD/OTP; không in hoặc thay bí mật tùy tiện.

## 5. Bộ kịch bản nghiệm thu bản cuối

| Kịch bản | Kết quả cần chứng minh |
|---|---|
| Khách đăng ký, xác thực, đăng nhập, sửa hồ sơ | Email duy nhất; OTP có hạn, không dùng lại; CCCD trống giữ bản mã; không tự cấp quyền. |
| Chuyên gia đăng ký, từ chối, gửi lại, được duyệt | Phiên bản và lý do cũ còn nguyên; chỉ sau duyệt mới có quyền; không tự công khai dịch vụ. |
| Dịch vụ nháp → duyệt → sửa | Sửa làm mất hiệu lực duyệt cũ, dữ liệu chưa duyệt không lọt vào khám phá. |
| Yêu cầu → đề xuất và thuê trực tiếp | Đúng chủ, đúng bên có lượt, rà soát trước lưu, chống bấm lặp, lịch sử riêng từng nguồn. |
| Kế hoạch → hợp đồng | Tổng tiền đúng, bản đầu do chuyên gia lập; bản chốt và bản hợp đồng khớp; không tạo nguồn giả. |
| Xác nhận và ký quỹ đầu | Thiếu một bên bị chặn; thay phiên bản phải xác nhận lại; giao dịch thất bại chưa bắt đầu; thử lại không đổi mốc bắt đầu đã ghi. |
| Bàn giao/sửa/nghiệm thu | Lưu từng lần, giới hạn sửa, thời hạn 14 ngày đúng sự kiện; giải ngân thất bại chưa tất toán. |
| Tranh chấp/chấm dứt | Giữ tiền khi chưa thống nhất; không chuyển tiền lặp khi retry từng phần; chỉ đóng khi xử lý hết tiền. |
| Đánh giá | Không lộ nhận xét đầu cho đối tác; đủ hai hoặc hết hạn mới công khai; đánh giá vi phạm tiếp tục ẩn. |
| Chuyển khách hàng cũ sang chuyên gia | Giữ ID và lịch sử; nghĩa vụ mới phát sinh trước duyệt được phát hiện; phiên/quyền sau chuyển cập nhật đúng. |

Các kịch bản trên là danh sách nghiệm thu, không phải bảng tuyên bố vừa kiểm thử đạt. Bằng chứng ngày 10/11 chỉ gồm đọc nguồn/tài liệu, kiểm tra SQL metadata/trạng thái tối thiểu và kiểm tra tính nhất quán của các tài liệu mới.
