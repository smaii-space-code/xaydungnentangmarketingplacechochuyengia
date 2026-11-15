# Mở rộng V7–V8: hợp đồng điện tử từ kế hoạch mốc

Không sửa ERD 12 bảng / 106 thuộc tính / 21 FK cơ sở trong ERD_BIZ04_CoSo.json. Đây là phần mở rộng riêng sau V6, không tuyên bố schema gốc đã có các thuộc tính này.

HopDong bổ sung 9 cột nullable: MaKeHoach, PhienBanKeHoach, MaKhachHang, MaChuyenGia, QuyTacPhi, QuyTacBatDau, DauKiemNoiDung, NgayLap, ThoiDiemBatDau. MaYeuCau/MaDeXuat nullable cho luồng thuê qua kế hoạch; CHECK yêu cầu hợp đồng cũ giữ cả hai nguồn thực và mọi cột mở rộng NULL, hoặc hợp đồng mới có kế hoạch/phiên bản/party/quy tắc đầy đủ. FK nguồn thực giữ nguyên. Filtered unique trên MaYeuCau, MaDeXuat và MaKeHoach; giá trị nguồn khác NULL vẫn duy nhất. V8 riêng thay unique MaDeXuat vì V7 đã được áp dụng trên test, không sửa migration đã chạy.

MocNghiemThu bổ sung 6 cột nullable: DinhDanhMoc, DonViHan, HanTuongDoi, LaMocCuoi, SanPhamThoaThuan, SoLanChinhSuaToiDa. HanBanGiao giữ DATETIME2(6), cho NULL trước sự kiện bắt đầu hợp lệ. CHECK tách metadata legacy/mới; legacy vẫn có hạn cụ thể, metadata mới đầy đủ hoặc không có toàn bộ.

PhienBanKeHoachMoc bổ sung QuyTacHan nullable (DAY_OFFSET hoặc MONTH_CAPPED_PROJECT_END). Phiên bản cũ NULL đọc theo DAY_OFFSET; không cập nhật hàng cũ.

BanChupHopDong (7 thuộc tính): MaBanChup, MaHopDong, PhienBan, MaNguoiLap, NoiDungJson, DauKiem, ThoiDiem. Nội dung JSON UTF-16 NVARCHAR(MAX), dấu SHA-256, unique hợp đồng/phiên bản. Entity bất biến.

XacNhanHopDong (6 thuộc tính): MaXacNhan, MaHopDong, PhienBan, MaNguoiXacNhan, DauKiem, ThoiDiem. Unique hợp đồng/phiên bản/người; FK đến đúng bản chụp hợp đồng/phiên bản. Entity bất biến; chỉ server xác định actor và thời điểm UTC.

9 FK mới: HopDong → KeHoachMoc, phiên bản kế hoạch (composite), khách hàng và chuyên gia; BanChupHopDong → HopDong và người lập; XacNhanHopDong → HopDong, người xác nhận và bản chụp (composite). Không thêm bảng tiền/hợp đồng song song. V7/V8 không UPDATE/DELETE/seed dữ liệu cũ.
