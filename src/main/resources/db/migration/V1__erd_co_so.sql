-- ERD cơ sở: 12 bảng, 106 thuộc tính, 21 FK. Không xóa dữ liệu hiện có.
CREATE TABLE [NguoiDung] (
    [MaNguoiDung] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [HoTen] NVARCHAR(100) NOT NULL,
    [Email] NVARCHAR(254) NOT NULL UNIQUE,
    [MatKhauBam] NVARCHAR(255) NOT NULL,
    [LaKhachHang] BIT NOT NULL,
    [LaChuyenGia] BIT NOT NULL,
    [LaQuanTriVien] BIT NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL
 );

CREATE TABLE [DichVu] (
    [MaDichVu] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaChuyenGia] BIGINT NOT NULL,
    [TenDichVu] NVARCHAR(255) NOT NULL,
    [LinhVuc] NVARCHAR(100) NOT NULL,
    [MoTa] NVARCHAR(MAX) NOT NULL,
    [GiaThamKhao] DECIMAL(18,0) NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL
 );

CREATE TABLE [YeuCau] (
    [MaYeuCau] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaKhachHang] BIGINT NOT NULL,
    [MaDichVu] BIGINT NULL,
    [TieuDe] NVARCHAR(255) NOT NULL,
    [MoTa] NVARCHAR(MAX) NOT NULL,
    [NganSach] DECIMAL(18,0) NOT NULL,
    [HanDuKien] DATETIME2(6) NULL,
    [TrangThai] NVARCHAR(40) NOT NULL
 );

CREATE TABLE [DeXuat] (
    [MaDeXuat] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaYeuCau] BIGINT NOT NULL,
    [MaChuyenGia] BIGINT NOT NULL,
    [PhuongAn] NVARCHAR(MAX) NOT NULL,
    [GiaDeXuat] DECIMAL(18,0) NOT NULL,
    [SoNgayThucHien] INT NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL
 );

CREATE TABLE [HopDong] (
    [MaHopDong] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaYeuCau] BIGINT NOT NULL UNIQUE,
    [MaDeXuat] BIGINT NOT NULL UNIQUE,
    [NoiDungThoaThuan] NVARCHAR(MAX) NOT NULL,
    [TongGiaTri] DECIMAL(18,0) NOT NULL,
    [PhienBan] INT NOT NULL,
    [PhienBanKhachXacNhan] INT NULL,
    [PhienBanChuyenGiaXacNhan] INT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL,
    [NgayKetThuc] DATETIME2(6) NULL
 );

CREATE TABLE [MocNghiemThu] (
    [MaMocNghiemThu] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaHopDong] BIGINT NOT NULL,
    [ThuTu] INT NOT NULL,
    [TenMoc] NVARCHAR(255) NOT NULL,
    [NoiDung] NVARCHAR(MAX) NOT NULL,
    [TieuChiNghiemThu] NVARCHAR(MAX) NOT NULL,
    [SoTien] DECIMAL(18,0) NOT NULL,
    [HanBanGiao] DATETIME2(6) NOT NULL,
    [HanNghiemThu] DATETIME2(6) NULL,
    [NgayNghiemThu] DATETIME2(6) NULL,
    [TrangThai] NVARCHAR(40) NOT NULL
 );

CREATE TABLE [BanGiao] (
    [MaBanGiao] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaMocNghiemThu] BIGINT NOT NULL,
    [LanBanGiao] INT NOT NULL,
    [MoTa] NVARCHAR(MAX) NOT NULL,
    [DuongDanSanPham] NVARCHAR(MAX) NOT NULL,
    [NgayGui] DATETIME2(6) NOT NULL,
    [KetQuaXuLy] NVARCHAR(40) NOT NULL,
    [NoiDungYeuCauSua] NVARCHAR(MAX) NULL,
    [NgayXuLy] DATETIME2(6) NULL
 );

CREATE TABLE [DanhGia] (
    [MaDanhGia] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaHopDong] BIGINT NOT NULL,
    [MaNguoiDanhGia] BIGINT NOT NULL,
    [MaNguoiDuocDanhGia] BIGINT NOT NULL,
    [Diem] INT NOT NULL,
    [NoiDung] NVARCHAR(MAX) NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL,
    [NgayGui] DATETIME2(6) NOT NULL,
    [NgayCongKhai] DATETIME2(6) NULL,
    [LyDoAn] NVARCHAR(MAX) NULL
 );

CREATE TABLE [TranhChap] (
    [MaTranhChap] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaMocNghiemThu] BIGINT NOT NULL,
    [MaNguoiMo] BIGINT NOT NULL,
    [MaQuanTriVienXuLy] BIGINT NULL,
    [LyDo] NVARCHAR(MAX) NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL,
    [KetLuan] NVARCHAR(MAX) NULL,
    [TienGiaiNgan] DECIMAL(18,0) NULL,
    [TienHoanTra] DECIMAL(18,0) NULL,
    [HuongXuLyHopDong] NVARCHAR(40) NULL,
    [NgayKetLuan] DATETIME2(6) NULL,
    [NgayDong] DATETIME2(6) NULL
 );

CREATE TABLE [GiaoDich] (
    [MaGiaoDich] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaMocNghiemThu] BIGINT NOT NULL,
    [MaTranhChap] BIGINT NULL,
    [MaNguoiYeuCau] BIGINT NULL,
    [LoaiGiaoDich] NVARCHAR(40) NOT NULL,
    [SoTien] DECIMAL(18,0) NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL,
    [KhoaChongLap] NVARCHAR(180) NOT NULL UNIQUE,
    [MaThamChieuCong] NVARCHAR(120) NULL,
    [NgayHoanTat] DATETIME2(6) NULL
 );

CREATE TABLE [BangChungTranhChap] (
    [MaBangChung] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaTranhChap] BIGINT NOT NULL,
    [MaNguoiGui] BIGINT NOT NULL,
    [LoaiNoiDung] NVARCHAR(40) NOT NULL,
    [NoiDung] NVARCHAR(MAX) NOT NULL,
    [DuongDanTep] NVARCHAR(MAX) NULL,
    [NgayGui] DATETIME2(6) NOT NULL
 );

CREATE TABLE [NhatKyXuLy] (
    [MaNhatKy] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaNguoiThucHien] BIGINT NULL,
    [LoaiDoiTuong] NVARCHAR(80) NOT NULL,
    [MaDoiTuong] BIGINT NOT NULL,
    [HanhDong] NVARCHAR(100) NOT NULL,
    [NoiDungLyDo] NVARCHAR(MAX) NULL,
    [ThoiDiem] DATETIME2(6) NOT NULL
 );

ALTER TABLE [YeuCau] ADD CONSTRAINT [FK_YeuCau_MaDichVu] FOREIGN KEY ([MaDichVu]) REFERENCES [DichVu] ([MaDichVu]);
ALTER TABLE [DeXuat] ADD CONSTRAINT [FK_DeXuat_MaYeuCau] FOREIGN KEY ([MaYeuCau]) REFERENCES [YeuCau] ([MaYeuCau]);
ALTER TABLE [HopDong] ADD CONSTRAINT [FK_HopDong_MaYeuCau] FOREIGN KEY ([MaYeuCau]) REFERENCES [YeuCau] ([MaYeuCau]);
ALTER TABLE [HopDong] ADD CONSTRAINT [FK_HopDong_MaDeXuat] FOREIGN KEY ([MaDeXuat]) REFERENCES [DeXuat] ([MaDeXuat]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_MaHopDong] FOREIGN KEY ([MaHopDong]) REFERENCES [HopDong] ([MaHopDong]);
ALTER TABLE [MocNghiemThu] ADD CONSTRAINT [FK_MocNghiemThu_MaHopDong] FOREIGN KEY ([MaHopDong]) REFERENCES [HopDong] ([MaHopDong]);
ALTER TABLE [BanGiao] ADD CONSTRAINT [FK_BanGiao_MaMocNghiemThu] FOREIGN KEY ([MaMocNghiemThu]) REFERENCES [MocNghiemThu] ([MaMocNghiemThu]);
ALTER TABLE [GiaoDich] ADD CONSTRAINT [FK_GiaoDich_MaMocNghiemThu] FOREIGN KEY ([MaMocNghiemThu]) REFERENCES [MocNghiemThu] ([MaMocNghiemThu]);
ALTER TABLE [TranhChap] ADD CONSTRAINT [FK_TranhChap_MaMocNghiemThu] FOREIGN KEY ([MaMocNghiemThu]) REFERENCES [MocNghiemThu] ([MaMocNghiemThu]);
ALTER TABLE [GiaoDich] ADD CONSTRAINT [FK_GiaoDich_MaTranhChap] FOREIGN KEY ([MaTranhChap]) REFERENCES [TranhChap] ([MaTranhChap]);
ALTER TABLE [BangChungTranhChap] ADD CONSTRAINT [FK_BangChungTranhChap_MaTranhChap] FOREIGN KEY ([MaTranhChap]) REFERENCES [TranhChap] ([MaTranhChap]);
ALTER TABLE [DichVu] ADD CONSTRAINT [FK_DichVu_MaChuyenGia] FOREIGN KEY ([MaChuyenGia]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [YeuCau] ADD CONSTRAINT [FK_YeuCau_MaKhachHang] FOREIGN KEY ([MaKhachHang]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [DeXuat] ADD CONSTRAINT [FK_DeXuat_MaChuyenGia] FOREIGN KEY ([MaChuyenGia]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_MaNguoiDanhGia] FOREIGN KEY ([MaNguoiDanhGia]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [DanhGia] ADD CONSTRAINT [FK_DanhGia_MaNguoiDuocDanhGia] FOREIGN KEY ([MaNguoiDuocDanhGia]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [GiaoDich] ADD CONSTRAINT [FK_GiaoDich_MaNguoiYeuCau] FOREIGN KEY ([MaNguoiYeuCau]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [TranhChap] ADD CONSTRAINT [FK_TranhChap_MaNguoiMo] FOREIGN KEY ([MaNguoiMo]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [TranhChap] ADD CONSTRAINT [FK_TranhChap_MaQuanTriVienXuLy] FOREIGN KEY ([MaQuanTriVienXuLy]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [BangChungTranhChap] ADD CONSTRAINT [FK_BangChungTranhChap_MaNguoiGui] FOREIGN KEY ([MaNguoiGui]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [NhatKyXuLy] ADD CONSTRAINT [FK_NhatKyXuLy_MaNguoiThucHien] FOREIGN KEY ([MaNguoiThucHien]) REFERENCES [NguoiDung] ([MaNguoiDung]);
CREATE UNIQUE INDEX [UX_DeXuat_Cap] ON [DeXuat] (MaYeuCau,MaChuyenGia);
CREATE UNIQUE INDEX [UX_MocNghiemThu_Cap] ON [MocNghiemThu] (MaHopDong,ThuTu);
CREATE UNIQUE INDEX [UX_BanGiao_Cap] ON [BanGiao] (MaMocNghiemThu,LanBanGiao);
CREATE UNIQUE INDEX [UX_DanhGia_Cap] ON [DanhGia] (MaHopDong,MaNguoiDanhGia);
CREATE UNIQUE INDEX [UX_GiaoDich_ThamChieu] ON [GiaoDich] ([MaThamChieuCong]) WHERE [MaThamChieuCong] IS NOT NULL;
CREATE UNIQUE INDEX [UX_TranhChap_DangMo] ON [TranhChap] ([MaMocNghiemThu]) WHERE [NgayDong] IS NULL;
ALTER TABLE [DanhGia] ADD CONSTRAINT [CK_DanhGia_Diem] CHECK ([Diem] BETWEEN 1 AND 5);
ALTER TABLE [DanhGia] ADD CONSTRAINT [CK_DanhGia_HaiBen] CHECK ([MaNguoiDanhGia] <> [MaNguoiDuocDanhGia]);
ALTER TABLE [MocNghiemThu] ADD CONSTRAINT [CK_Moc_Tien] CHECK ([SoTien] > 0 AND [ThuTu] > 0);
ALTER TABLE [GiaoDich] ADD CONSTRAINT [CK_GiaoDich_Tien] CHECK ([SoTien] > 0);
