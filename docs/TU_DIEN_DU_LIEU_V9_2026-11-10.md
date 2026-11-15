# Sang Connect — Từ điển dữ liệu triển khai V9

Đối chiếu trực tiếp metadata BIZ04 ngày 10/11/2026 qua Windows Authentication. Chỉ lấy định nghĩa bảng/cột/khóa; không xuất bản ghi tài khoản, OTP, mật khẩu hoặc nội dung riêng.

Có **28 bảng ứng dụng, 295 cột vật lý và 58 ràng buộc khóa ngoại**. Không tính `flyway_schema_history` (10 cột) và `sysdiagrams` (5 cột). Cột tính toán cũng được tính trong 295 cột; FK ghép được tính là một ràng buộc, không phải một FK cho mỗi cặp cột.

ERD cơ sở vẫn là 12 bảng, 106 thuộc tính, 21 FK. Nhãn “bảng gốc” bên dưới chỉ nguồn gốc tên bảng; cấu trúc V9 của bảng đó có thể đã được mở rộng. Tệp này mô tả vật lý đang triển khai, không thay thế ERD logic gốc.

## 1. Danh sách bảng

| Bảng | Số cột | Nguồn gốc |
|---|---:|---|
| `BanChupHopDong` | 7 | Bảng mở rộng |
| `BangChungTranhChap` | 7 | Bảng gốc, cấu trúc hiện tại V9 |
| `BanGiao` | 9 | Bảng gốc, cấu trúc hiện tại V9 |
| `ChiTietKeHoachMoc` | 14 | Bảng mở rộng |
| `DanhGia` | 10 | Bảng gốc, cấu trúc hiện tại V9 |
| `DeNghiThue` | 15 | Bảng mở rộng |
| `DeXuat` | 7 | Bảng gốc, cấu trúc hiện tại V9 |
| `DichVu` | 16 | Bảng gốc, cấu trúc hiện tại V9 |
| `GiaoDich` | 12 | Bảng gốc, cấu trúc hiện tại V9 |
| `HopDong` | 20 | Bảng gốc, cấu trúc hiện tại V9 |
| `HoSoChuyenGia` | 8 | Bảng mở rộng |
| `HoSoKhachHang` | 8 | Bảng mở rộng |
| `KeHoachMoc` | 22 | Bảng mở rộng |
| `MaXacThucOTP` | 11 | Bảng mở rộng |
| `MocNghiemThu` | 17 | Bảng gốc, cấu trúc hiện tại V9 |
| `NguoiDung` | 16 | Bảng gốc, cấu trúc hiện tại V9 |
| `NhatKyXuLy` | 7 | Bảng gốc, cấu trúc hiện tại V9 |
| `OtpDieuPhoi` | 1 | Bảng mở rộng |
| `OtpGioiHan` | 3 | Bảng mở rộng |
| `PhienBanDeNghiThue` | 11 | Bảng mở rộng |
| `PhienBanHoSoChuyenGia` | 10 | Bảng mở rộng |
| `PhienBanKeHoachMoc` | 11 | Bảng mở rộng |
| `QuyetDinhChuyenGia` | 7 | Bảng mở rộng |
| `TiepQuanTaiKhoan` | 7 | Bảng mở rộng |
| `TranhChap` | 13 | Bảng gốc, cấu trúc hiện tại V9 |
| `XacNhanHopDong` | 6 | Bảng mở rộng |
| `YeuCau` | 8 | Bảng gốc, cấu trúc hiện tại V9 |
| `YeuCauChamDut` | 12 | Bảng mở rộng |

## 2. Chi tiết cột và khóa ngoại

### BanChupHopDong

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaBanChup` | bigint | Không | PK, IDENTITY |
| `MaHopDong` | bigint | Không |  |
| `PhienBan` | int | Không |  |
| `MaNguoiLap` | bigint | Không |  |
| `NoiDungJson` | nvarchar(MAX) | Không |  |
| `DauKiem` | varchar(64) | Không |  |
| `ThoiDiem` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__BanChupHo__MaHop__7849DB76`: `BanChupHopDong(MaHopDong)` → `HopDong(MaHopDong)`.
- `FK__BanChupHo__MaNgu__793DFFAF`: `BanChupHopDong(MaNguoiLap)` → `NguoiDung(MaNguoiDung)`.

### BangChungTranhChap

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaBangChung` | bigint | Không | PK, IDENTITY |
| `MaTranhChap` | bigint | Không |  |
| `MaNguoiGui` | bigint | Không |  |
| `LoaiNoiDung` | nvarchar(40) | Không |  |
| `NoiDung` | nvarchar(MAX) | Không |  |
| `DuongDanTep` | nvarchar(MAX) | Có |  |
| `NgayGui` | datetime2(6) | Không |  |

Khóa ngoại:

- `FK_BangChungTranhChap_MaNguoiGui`: `BangChungTranhChap(MaNguoiGui)` → `NguoiDung(MaNguoiDung)`.
- `FK_BangChungTranhChap_MaTranhChap`: `BangChungTranhChap(MaTranhChap)` → `TranhChap(MaTranhChap)`.

### BanGiao

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaBanGiao` | bigint | Không | PK, IDENTITY |
| `MaMocNghiemThu` | bigint | Không |  |
| `LanBanGiao` | int | Không |  |
| `MoTa` | nvarchar(MAX) | Không |  |
| `DuongDanSanPham` | nvarchar(MAX) | Không |  |
| `NgayGui` | datetime2(6) | Không |  |
| `KetQuaXuLy` | nvarchar(40) | Không |  |
| `NoiDungYeuCauSua` | nvarchar(MAX) | Có |  |
| `NgayXuLy` | datetime2(6) | Có |  |

Khóa ngoại:

- `FK_BanGiao_MaMocNghiemThu`: `BanGiao(MaMocNghiemThu)` → `MocNghiemThu(MaMocNghiemThu)`.

### ChiTietKeHoachMoc

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaChiTiet` | bigint | Không | PK, IDENTITY |
| `MaPhienBan` | bigint | Không |  |
| `DinhDanhMoc` | varchar(36) | Không |  |
| `ThuTu` | int | Không |  |
| `TenMoc` | nvarchar(255) | Không |  |
| `CongViec` | nvarchar(MAX) | Không |  |
| `SanPham` | nvarchar(MAX) | Không |  |
| `TieuChi` | nvarchar(MAX) | Không |  |
| `DonViHan` | varchar(10) | Không |  |
| `HanTuongDoi` | int | Không |  |
| `GiaTri` | decimal(18,0) | Không |  |
| `PhiDuKien` | decimal(18,0) | Không |  |
| `ThucNhanDuKien` | decimal(18,0) | Không |  |
| `SoLanChinhSua` | int | Không |  |

Khóa ngoại:

- `FK__ChiTietKe__MaPhi__6166761E`: `ChiTietKeHoachMoc(MaPhienBan)` → `PhienBanKeHoachMoc(MaPhienBan)`.

### DanhGia

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaDanhGia` | bigint | Không | PK, IDENTITY |
| `MaHopDong` | bigint | Không |  |
| `MaNguoiDanhGia` | bigint | Không |  |
| `MaNguoiDuocDanhGia` | bigint | Không |  |
| `Diem` | int | Không |  |
| `NoiDung` | nvarchar(MAX) | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `NgayGui` | datetime2(6) | Không |  |
| `NgayCongKhai` | datetime2(6) | Có |  |
| `LyDoAn` | nvarchar(MAX) | Có |  |

Khóa ngoại:

- `FK_DanhGia_MaHopDong`: `DanhGia(MaHopDong)` → `HopDong(MaHopDong)`.
- `FK_DanhGia_MaNguoiDanhGia`: `DanhGia(MaNguoiDanhGia)` → `NguoiDung(MaNguoiDung)`.
- `FK_DanhGia_MaNguoiDuocDanhGia`: `DanhGia(MaNguoiDuocDanhGia)` → `NguoiDung(MaNguoiDung)`.

### DeNghiThue

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaDeNghi` | bigint | Không | PK, IDENTITY |
| `MaKhachHang` | bigint | Không |  |
| `MaChuyenGia` | bigint | Không |  |
| `MaDichVu` | bigint | Có |  |
| `MaGui` | varchar(36) | Không |  |
| `TieuDe` | nvarchar(255) | Không |  |
| `TrangThai` | varchar(30) | Không |  |
| `PhienBanHienTai` | int | Không |  |
| `MaBenChoPhanHoi` | bigint | Có |  |
| `PhienBanDaThongNhat` | int | Có |  |
| `MaNguoiQuyetDinh` | bigint | Có |  |
| `ThoiDiemQuyetDinh` | datetimeoffset(6) | Có |  |
| `LyDoTuChoi` | nvarchar(2000) | Có |  |
| `NgayGui` | datetimeoffset(6) | Không |  |
| `NgayCapNhat` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__DeNghiThu__MaBen__1CBC4616`: `DeNghiThue(MaBenChoPhanHoi)` → `NguoiDung(MaNguoiDung)`.
- `FK__DeNghiThu__MaChu__1AD3FDA4`: `DeNghiThue(MaChuyenGia)` → `NguoiDung(MaNguoiDung)`.
- `FK__DeNghiThu__MaDic__1BC821DD`: `DeNghiThue(MaDichVu)` → `DichVu(MaDichVu)`.
- `FK__DeNghiThu__MaKha__19DFD96B`: `DeNghiThue(MaKhachHang)` → `NguoiDung(MaNguoiDung)`.
- `FK__DeNghiThu__MaNgu__1DB06A4F`: `DeNghiThue(MaNguoiQuyetDinh)` → `NguoiDung(MaNguoiDung)`.

### DeXuat

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaDeXuat` | bigint | Không | PK, IDENTITY |
| `MaYeuCau` | bigint | Không |  |
| `MaChuyenGia` | bigint | Không |  |
| `PhuongAn` | nvarchar(MAX) | Không |  |
| `GiaDeXuat` | decimal(18,0) | Không |  |
| `SoNgayThucHien` | int | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |

Khóa ngoại:

- `FK_DeXuat_MaChuyenGia`: `DeXuat(MaChuyenGia)` → `NguoiDung(MaNguoiDung)`.
- `FK_DeXuat_MaYeuCau`: `DeXuat(MaYeuCau)` → `YeuCau(MaYeuCau)`.

### DichVu

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaDichVu` | bigint | Không | PK, IDENTITY |
| `MaChuyenGia` | bigint | Không |  |
| `TenDichVu` | nvarchar(255) | Không |  |
| `LinhVuc` | nvarchar(100) | Không |  |
| `MoTa` | nvarchar(MAX) | Không |  |
| `GiaThamKhao` | decimal(18,0) | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `TrangThaiDuyet` | varchar(20) | Không |  |
| `PhienBanNoiDung` | int | Không |  |
| `PhienBanDuyet` | int | Có |  |
| `XacNhanNoiDung` | bit | Không |  |
| `CanRaSoatLai` | bit | Không |  |
| `NguonNoiDung` | varchar(20) | Không |  |
| `MaNguoiDuyet` | bigint | Có |  |
| `NgayDuyet` | datetimeoffset(6) | Có |  |
| `LyDoDuyet` | nvarchar(2000) | Có |  |

Khóa ngoại:

- `FK__DichVu__MaNguoiD__45BE5BA9`: `DichVu(MaNguoiDuyet)` → `NguoiDung(MaNguoiDung)`.
- `FK_DichVu_MaChuyenGia`: `DichVu(MaChuyenGia)` → `NguoiDung(MaNguoiDung)`.

### GiaoDich

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaGiaoDich` | bigint | Không | PK, IDENTITY |
| `MaMocNghiemThu` | bigint | Không |  |
| `MaTranhChap` | bigint | Có |  |
| `MaNguoiYeuCau` | bigint | Có |  |
| `LoaiGiaoDich` | nvarchar(40) | Không |  |
| `SoTien` | decimal(18,0) | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `KhoaChongLap` | nvarchar(180) | Không |  |
| `MaThamChieuCong` | nvarchar(120) | Có |  |
| `NgayHoanTat` | datetime2(6) | Có |  |
| `PhiNenTang` | decimal(18,0) | Không |  |
| `ThucNhan` | decimal(18,0) | Không |  |

Khóa ngoại:

- `FK_GiaoDich_MaMocNghiemThu`: `GiaoDich(MaMocNghiemThu)` → `MocNghiemThu(MaMocNghiemThu)`.
- `FK_GiaoDich_MaNguoiYeuCau`: `GiaoDich(MaNguoiYeuCau)` → `NguoiDung(MaNguoiDung)`.
- `FK_GiaoDich_MaTranhChap`: `GiaoDich(MaTranhChap)` → `TranhChap(MaTranhChap)`.

### HopDong

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaHopDong` | bigint | Không | PK, IDENTITY |
| `MaYeuCau` | bigint | Có |  |
| `MaDeXuat` | bigint | Có |  |
| `NoiDungThoaThuan` | nvarchar(MAX) | Không |  |
| `TongGiaTri` | decimal(18,0) | Không |  |
| `PhienBan` | int | Không |  |
| `PhienBanKhachXacNhan` | int | Có |  |
| `PhienBanChuyenGiaXacNhan` | int | Có |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `NgayKetThuc` | datetime2(6) | Có |  |
| `TyLePhi` | decimal(18,4) | Không |  |
| `MaKeHoach` | bigint | Có |  |
| `PhienBanKeHoach` | int | Có |  |
| `MaKhachHang` | bigint | Có |  |
| `MaChuyenGia` | bigint | Có |  |
| `QuyTacPhi` | varchar(40) | Có |  |
| `QuyTacBatDau` | varchar(40) | Có |  |
| `DauKiemNoiDung` | varchar(64) | Có |  |
| `NgayLap` | datetimeoffset(6) | Có |  |
| `ThoiDiemBatDau` | datetimeoffset(6) | Có |  |

Khóa ngoại:

- `FK_HopDong_ChuyenGia`: `HopDong(MaChuyenGia)` → `NguoiDung(MaNguoiDung)`.
- `FK_HopDong_KeHoach`: `HopDong(MaKeHoach)` → `KeHoachMoc(MaKeHoach)`.
- `FK_HopDong_Khach`: `HopDong(MaKhachHang)` → `NguoiDung(MaNguoiDung)`.
- `FK_HopDong_MaDeXuat`: `HopDong(MaDeXuat)` → `DeXuat(MaDeXuat)`.
- `FK_HopDong_MaYeuCau`: `HopDong(MaYeuCau)` → `YeuCau(MaYeuCau)`.
- `FK_HopDong_PhienBanKeHoach`: `HopDong(MaKeHoach, PhienBanKeHoach)` → `PhienBanKeHoachMoc(MaKeHoach, SoPhienBan)`.

### HoSoChuyenGia

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaNguoiDung` | bigint | Không | PK |
| `TrangThai` | varchar(20) | Không |  |
| `PhienBan` | int | Không |  |
| `LinhVuc` | nvarchar(100) | Không |  |
| `GioiThieu` | nvarchar(2000) | Không |  |
| `MaDangKy` | varchar(36) | Không |  |
| `NgayTao` | datetimeoffset(6) | Không |  |
| `NgayGui` | datetimeoffset(6) | Có |  |

Khóa ngoại:

- `FK__HoSoChuye__MaNgu__0A688BB1`: `HoSoChuyenGia(MaNguoiDung)` → `NguoiDung(MaNguoiDung)`.

### HoSoKhachHang

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaNguoiDung` | bigint | Không | PK |
| `SoDienThoai` | varchar(10) | Có |  |
| `LoaiKhachHang` | varchar(20) | Không |  |
| `TenToChuc` | nvarchar(200) | Có |  |
| `DiaChi` | nvarchar(250) | Có |  |
| `GioiThieu` | nvarchar(2000) | Có |  |
| `SoCccdMaHoa` | varchar(255) | Có |  |
| `CccdBonSoCuoi` | varchar(4) | Có |  |

Khóa ngoại:

- `FK__HoSoKhach__MaNgu__05D8E0BE`: `HoSoKhachHang(MaNguoiDung)` → `NguoiDung(MaNguoiDung)`.

### KeHoachMoc

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaKeHoach` | bigint | Không | PK, IDENTITY |
| `MaDeNghi` | bigint | Có |  |
| `MaDeXuat` | bigint | Có |  |
| `DauVanNguon` | varchar(64) | Không |  |
| `PhienBanNguon` | int | Có |  |
| `MaKhachHang` | bigint | Không |  |
| `MaChuyenGia` | bigint | Không |  |
| `TenKhachHang` | nvarchar(255) | Không |  |
| `TenChuyenGia` | nvarchar(255) | Không |  |
| `TieuDeNguon` | nvarchar(255) | Không |  |
| `PhamViNguon` | nvarchar(MAX) | Không |  |
| `SanPhamNguon` | nvarchar(MAX) | Không |  |
| `TongGiaThue` | decimal(18,0) | Không |  |
| `SoNgayNguon` | int | Không |  |
| `TrangThai` | varchar(30) | Không |  |
| `PhienBanHienTai` | int | Không |  |
| `MaBenChoPhanHoi` | bigint | Có |  |
| `PhienBanDaThongNhat` | int | Có |  |
| `MaNguoiQuyetDinh` | bigint | Có |  |
| `LyDoHuy` | nvarchar(2000) | Có |  |
| `ThoiDiemQuyetDinh` | datetimeoffset(6) | Có |  |
| `NgayTao` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__KeHoachMo__MaBen__4D5F7D71`: `KeHoachMoc(MaBenChoPhanHoi)` → `NguoiDung(MaNguoiDung)`.
- `FK__KeHoachMo__MaChu__4C6B5938`: `KeHoachMoc(MaChuyenGia)` → `NguoiDung(MaNguoiDung)`.
- `FK__KeHoachMo__MaDeN__498EEC8D`: `KeHoachMoc(MaDeNghi)` → `DeNghiThue(MaDeNghi)`.
- `FK__KeHoachMo__MaDeX__4A8310C6`: `KeHoachMoc(MaDeXuat)` → `DeXuat(MaDeXuat)`.
- `FK__KeHoachMo__MaKha__4B7734FF`: `KeHoachMoc(MaKhachHang)` → `NguoiDung(MaNguoiDung)`.
- `FK__KeHoachMo__MaNgu__4E53A1AA`: `KeHoachMoc(MaNguoiQuyetDinh)` → `NguoiDung(MaNguoiDung)`.

### MaXacThucOTP

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaOTP` | bigint | Không | PK, IDENTITY |
| `MaNguoiDung` | bigint | Không |  |
| `Email` | nvarchar(254) | Không |  |
| `MaCodeHash` | varchar(255) | Không |  |
| `MaYeuCau` | varchar(64) | Không |  |
| `LoaiOTP` | varchar(30) | Không |  |
| `ThoiDiemTao` | datetimeoffset(6) | Không |  |
| `ThoiDiemHetHan` | datetimeoffset(6) | Không |  |
| `ThoiDiemSuDung` | datetimeoffset(6) | Có |  |
| `SoLanThuSai` | int | Không |  |
| `TrangThai` | varchar(30) | Không |  |

Khóa ngoại:

- `FK__MaXacThuc__MaNgu__0E6E26BF`: `MaXacThucOTP(MaNguoiDung)` → `NguoiDung(MaNguoiDung)`.

### MocNghiemThu

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaMocNghiemThu` | bigint | Không | PK, IDENTITY |
| `MaHopDong` | bigint | Không |  |
| `ThuTu` | int | Không |  |
| `TenMoc` | nvarchar(255) | Không |  |
| `NoiDung` | nvarchar(MAX) | Không |  |
| `TieuChiNghiemThu` | nvarchar(MAX) | Không |  |
| `SoTien` | decimal(18,0) | Không |  |
| `HanBanGiao` | datetime2(6) | Có |  |
| `HanNghiemThu` | datetime2(6) | Có |  |
| `NgayNghiemThu` | datetime2(6) | Có |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `DinhDanhMoc` | varchar(36) | Có |  |
| `DonViHan` | varchar(10) | Có |  |
| `HanTuongDoi` | int | Có |  |
| `LaMocCuoi` | bit | Có |  |
| `SanPhamThoaThuan` | nvarchar(MAX) | Có |  |
| `SoLanChinhSuaToiDa` | int | Có |  |

Khóa ngoại:

- `FK_MocNghiemThu_MaHopDong`: `MocNghiemThu(MaHopDong)` → `HopDong(MaHopDong)`.

### NguoiDung

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaNguoiDung` | bigint | Không | PK, IDENTITY |
| `HoTen` | nvarchar(100) | Không |  |
| `Email` | nvarchar(254) | Không |  |
| `MatKhauBam` | nvarchar(255) | Không |  |
| `LaKhachHang` | bit | Không |  |
| `LaChuyenGia` | bit | Không |  |
| `LaQuanTriVien` | bit | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `TrangThaiEmail` | varchar(30) | Không |  |
| `ThoiDiemXacThucEmail` | datetimeoffset(6) | Có |  |
| `NgayTao` | datetimeoffset(6) | Không |  |
| `NgayCapNhat` | datetimeoffset(6) | Không |  |
| `EmailChuan` | nvarchar(254) | Có | Cột tính toán |
| `LoaiTaiKhoan` | varchar(20) | Không |  |
| `PhienBaoMat` | int | Không |  |
| `NguonDangKy` | varchar(20) | Có |  |

### NhatKyXuLy

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaNhatKy` | bigint | Không | PK, IDENTITY |
| `MaNguoiThucHien` | bigint | Có |  |
| `LoaiDoiTuong` | nvarchar(80) | Không |  |
| `MaDoiTuong` | bigint | Không |  |
| `HanhDong` | nvarchar(100) | Không |  |
| `NoiDungLyDo` | nvarchar(MAX) | Có |  |
| `ThoiDiem` | datetime2(6) | Không |  |

Khóa ngoại:

- `FK_NhatKyXuLy_MaNguoiThucHien`: `NhatKyXuLy(MaNguoiThucHien)` → `NguoiDung(MaNguoiDung)`.

### OtpDieuPhoi

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `Id` | bigint | Không | PK |

### OtpGioiHan

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `Khoa` | varchar(64) | Không | PK |
| `BatDau` | datetimeoffset(6) | Không |  |
| `SoLuong` | int | Không |  |

### PhienBanDeNghiThue

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaPhienBan` | bigint | Không | PK, IDENTITY |
| `MaDeNghi` | bigint | Không |  |
| `SoPhienBan` | int | Không |  |
| `MaTacGia` | bigint | Không |  |
| `MaGui` | varchar(36) | Không |  |
| `PhamVi` | nvarchar(MAX) | Không |  |
| `SanPham` | nvarchar(MAX) | Không |  |
| `Gia` | decimal(18,0) | Không |  |
| `SoNgay` | int | Không |  |
| `LyDoDieuChinh` | nvarchar(2000) | Có |  |
| `NgayGui` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__PhienBanD__MaDeN__25518C17`: `PhienBanDeNghiThue(MaDeNghi)` → `DeNghiThue(MaDeNghi)`.
- `FK__PhienBanD__MaTac__2645B050`: `PhienBanDeNghiThue(MaTacGia)` → `NguoiDung(MaNguoiDung)`.

### PhienBanHoSoChuyenGia

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaBanChup` | bigint | Không | PK, IDENTITY |
| `MaNguoiDung` | bigint | Không |  |
| `PhienBan` | int | Không |  |
| `HoTen` | nvarchar(100) | Không |  |
| `Email` | nvarchar(254) | Không |  |
| `SoDienThoai` | varchar(10) | Không |  |
| `LinhVuc` | nvarchar(100) | Không |  |
| `GioiThieu` | nvarchar(2000) | Không |  |
| `MaThaoTac` | varchar(36) | Không |  |
| `NgayGui` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__PhienBanH__MaNgu__11158940`: `PhienBanHoSoChuyenGia(MaNguoiDung)` → `HoSoChuyenGia(MaNguoiDung)`.

### PhienBanKeHoachMoc

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaPhienBan` | bigint | Không | PK, IDENTITY |
| `MaKeHoach` | bigint | Không |  |
| `SoPhienBan` | int | Không |  |
| `MaTacGia` | bigint | Không |  |
| `MaGui` | varchar(36) | Không |  |
| `CachChia` | varchar(20) | Không |  |
| `LyDoDieuChinh` | nvarchar(2000) | Có |  |
| `TyLePhi` | decimal(7,4) | Không |  |
| `QuyTacPhi` | varchar(40) | Không |  |
| `NgayGui` | datetimeoffset(6) | Không |  |
| `QuyTacHan` | varchar(40) | Có |  |

Khóa ngoại:

- `FK__PhienBanK__MaKeH__57DD0BE4`: `PhienBanKeHoachMoc(MaKeHoach)` → `KeHoachMoc(MaKeHoach)`.
- `FK__PhienBanK__MaTac__58D1301D`: `PhienBanKeHoachMoc(MaTacGia)` → `NguoiDung(MaNguoiDung)`.

### QuyetDinhChuyenGia

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaQuyetDinh` | bigint | Không | PK, IDENTITY |
| `MaNguoiDung` | bigint | Không |  |
| `PhienBan` | int | Không |  |
| `MaAdmin` | bigint | Không |  |
| `ChapNhan` | bit | Không |  |
| `LyDo` | nvarchar(2000) | Có |  |
| `NgayXuLy` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__QuyetDinh__MaAdm__15DA3E5D`: `QuyetDinhChuyenGia(MaAdmin)` → `NguoiDung(MaNguoiDung)`.
- `FK_QuyetDinhCG_BanChup`: `QuyetDinhChuyenGia(MaNguoiDung, PhienBan)` → `PhienBanHoSoChuyenGia(MaNguoiDung, PhienBan)`.

### TiepQuanTaiKhoan

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaNguoiDung` | bigint | Không | PK |
| `TenGoc` | nvarchar(100) | Không |  |
| `EmailGoc` | nvarchar(254) | Không |  |
| `NguonGoc` | nvarchar(200) | Không |  |
| `BatDau` | datetimeoffset(6) | Không |  |
| `HoanTat` | datetimeoffset(6) | Có |  |
| `TenDuKien` | nvarchar(100) | Có |  |

Khóa ngoại:

- `FK__TiepQuanT__MaNgu__3F115E1A`: `TiepQuanTaiKhoan(MaNguoiDung)` → `NguoiDung(MaNguoiDung)`.

### TranhChap

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaTranhChap` | bigint | Không | PK, IDENTITY |
| `MaMocNghiemThu` | bigint | Không |  |
| `MaNguoiMo` | bigint | Không |  |
| `MaQuanTriVienXuLy` | bigint | Có |  |
| `LyDo` | nvarchar(MAX) | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `KetLuan` | nvarchar(MAX) | Có |  |
| `TienGiaiNgan` | decimal(18,0) | Có |  |
| `TienHoanTra` | decimal(18,0) | Có |  |
| `HuongXuLyHopDong` | nvarchar(40) | Có |  |
| `NgayKetLuan` | datetime2(6) | Có |  |
| `NgayDong` | datetime2(6) | Có |  |
| `HanNopBangChung` | datetime2(6) | Có |  |

Khóa ngoại:

- `FK_TranhChap_MaMocNghiemThu`: `TranhChap(MaMocNghiemThu)` → `MocNghiemThu(MaMocNghiemThu)`.
- `FK_TranhChap_MaNguoiMo`: `TranhChap(MaNguoiMo)` → `NguoiDung(MaNguoiDung)`.
- `FK_TranhChap_MaQuanTriVienXuLy`: `TranhChap(MaQuanTriVienXuLy)` → `NguoiDung(MaNguoiDung)`.

### XacNhanHopDong

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaXacNhan` | bigint | Không | PK, IDENTITY |
| `MaHopDong` | bigint | Không |  |
| `PhienBan` | int | Không |  |
| `MaNguoiXacNhan` | bigint | Không |  |
| `DauKiem` | varchar(64) | Không |  |
| `ThoiDiem` | datetimeoffset(6) | Không |  |

Khóa ngoại:

- `FK__XacNhanHo__MaHop__7E02B4CC`: `XacNhanHopDong(MaHopDong)` → `HopDong(MaHopDong)`.
- `FK__XacNhanHo__MaNgu__7EF6D905`: `XacNhanHopDong(MaNguoiXacNhan)` → `NguoiDung(MaNguoiDung)`.
- `FK_XacNhanHopDong_PhienBan`: `XacNhanHopDong(MaHopDong, PhienBan)` → `BanChupHopDong(MaHopDong, PhienBan)`.

### YeuCau

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaYeuCau` | bigint | Không | PK, IDENTITY |
| `MaKhachHang` | bigint | Không |  |
| `MaDichVu` | bigint | Có |  |
| `TieuDe` | nvarchar(255) | Không |  |
| `MoTa` | nvarchar(MAX) | Không |  |
| `NganSach` | decimal(18,0) | Không |  |
| `HanDuKien` | datetime2(6) | Có |  |
| `TrangThai` | nvarchar(40) | Không |  |

Khóa ngoại:

- `FK_YeuCau_MaDichVu`: `YeuCau(MaDichVu)` → `DichVu(MaDichVu)`.
- `FK_YeuCau_MaKhachHang`: `YeuCau(MaKhachHang)` → `NguoiDung(MaNguoiDung)`.

### YeuCauChamDut

| Cột | Kiểu SQL Server | Cho NULL | Đặc điểm |
|---|---|---|---|
| `MaYeuCauChamDut` | bigint | Không | PK, IDENTITY |
| `MaHopDong` | bigint | Không |  |
| `MaMocNghiemThu` | bigint | Có |  |
| `MaNguoiDeNghi` | bigint | Không |  |
| `MaNguoiDongY` | bigint | Có |  |
| `LyDo` | nvarchar(MAX) | Không |  |
| `TrangThai` | nvarchar(40) | Không |  |
| `TienChuyenGia` | decimal(18,0) | Không |  |
| `TienHoanKhach` | decimal(18,0) | Không |  |
| `NgayDeNghi` | datetime2(6) | Không |  |
| `HanPhanHoi` | datetime2(6) | Không |  |
| `NgayPhanHoi` | datetime2(6) | Có |  |

Khóa ngoại:

- `FK_ChamDut_MaHopDong`: `YeuCauChamDut(MaHopDong)` → `HopDong(MaHopDong)`.
- `FK_ChamDut_MaMocNghiemThu`: `YeuCauChamDut(MaMocNghiemThu)` → `MocNghiemThu(MaMocNghiemThu)`.
- `FK_ChamDut_MaNguoiDeNghi`: `YeuCauChamDut(MaNguoiDeNghi)` → `NguoiDung(MaNguoiDung)`.
- `FK_ChamDut_MaNguoiDongY`: `YeuCauChamDut(MaNguoiDongY)` → `NguoiDung(MaNguoiDung)`.

## 3. Ràng buộc nghiệp vụ bổ sung

Bảng cột/khóa ngoại chưa liệt kê toàn bộ CHECK, UNIQUE, chỉ mục có điều kiện, default hoặc quy tắc service. Khi thiết kế/triển khai cần đọc V1–V9 cùng mã service. Một số điểm quan trọng: email chuẩn hóa duy nhất; nguồn kế hoạch chọn đúng một trong hai loại; phiên bản/mã thao tác duy nhất; nguồn hợp đồng nullable dùng filtered unique; quyết định chuyên gia duy nhất theo phiên bản; tiền và trạng thái được kiểm tra cả tại DB và máy chủ.

Nguồn metadata: `.run/report-review-20261110/schema.json`; truy vấn chỉ đọc: `schema-readonly.sql` cùng thư mục. Dữ liệu được chụp tại thời điểm nêu trên, cần đối chiếu lại sau migration mới.
