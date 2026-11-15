-- Additive extension. No changes to existing contracts, milestones or financial tables.
CREATE TABLE KeHoachMoc (
 MaKeHoach BIGINT IDENTITY PRIMARY KEY,
 MaDeNghi BIGINT NULL REFERENCES DeNghiThue(MaDeNghi),
 MaDeXuat BIGINT NULL REFERENCES DeXuat(MaDeXuat),
 DauVanNguon VARCHAR(64) NOT NULL,
 PhienBanNguon INT NULL,
 MaKhachHang BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
 MaChuyenGia BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
 TenKhachHang NVARCHAR(255) NOT NULL,
 TenChuyenGia NVARCHAR(255) NOT NULL,
 TieuDeNguon NVARCHAR(255) NOT NULL,
 PhamViNguon NVARCHAR(MAX) NOT NULL,
 SanPhamNguon NVARCHAR(MAX) NOT NULL,
 TongGiaThue DECIMAL(18,0) NOT NULL,
 SoNgayNguon INT NOT NULL,
 TrangThai VARCHAR(30) NOT NULL,
 PhienBanHienTai INT NOT NULL,
 MaBenChoPhanHoi BIGINT NULL REFERENCES NguoiDung(MaNguoiDung),
 PhienBanDaThongNhat INT NULL,
 MaNguoiQuyetDinh BIGINT NULL REFERENCES NguoiDung(MaNguoiDung),
 LyDoHuy NVARCHAR(2000) NULL,
 ThoiDiemQuyetDinh DATETIMEOFFSET(6) NULL,
 NgayTao DATETIMEOFFSET(6) NOT NULL,
 CONSTRAINT CK_KeHoachMoc_Nguon CHECK ((MaDeNghi IS NOT NULL AND MaDeXuat IS NULL AND PhienBanNguon IS NOT NULL AND PhienBanNguon>=1) OR (MaDeNghi IS NULL AND MaDeXuat IS NOT NULL AND PhienBanNguon IS NULL)),
 CONSTRAINT CK_KeHoachMoc_HaiBen CHECK (MaKhachHang<>MaChuyenGia),
 CONSTRAINT CK_KeHoachMoc_Gia CHECK (TongGiaThue>0 AND TongGiaThue<=1000000000000),
 CONSTRAINT CK_KeHoachMoc_Ngay CHECK (SoNgayNguon>0),
 CONSTRAINT CK_KeHoachMoc_TrangThai CHECK (
  (TrangThai='CHO_PHAN_HOI' AND PhienBanHienTai>=1 AND MaBenChoPhanHoi IN(MaKhachHang,MaChuyenGia) AND MaBenChoPhanHoi IS NOT NULL AND PhienBanDaThongNhat IS NULL AND MaNguoiQuyetDinh IS NULL AND ThoiDiemQuyetDinh IS NULL AND LyDoHuy IS NULL)
  OR (TrangThai='DA_THONG_NHAT' AND PhienBanHienTai>=1 AND PhienBanDaThongNhat=PhienBanHienTai AND PhienBanDaThongNhat IS NOT NULL AND MaBenChoPhanHoi IS NULL AND MaNguoiQuyetDinh IN(MaKhachHang,MaChuyenGia) AND MaNguoiQuyetDinh IS NOT NULL AND ThoiDiemQuyetDinh IS NOT NULL AND LyDoHuy IS NULL)
  OR (TrangThai='DA_HUY' AND PhienBanHienTai>=0 AND MaBenChoPhanHoi IS NULL AND PhienBanDaThongNhat IS NULL AND MaNguoiQuyetDinh IN(MaKhachHang,MaChuyenGia) AND MaNguoiQuyetDinh IS NOT NULL AND ThoiDiemQuyetDinh IS NOT NULL AND LyDoHuy IS NOT NULL AND LEN(LTRIM(RTRIM(LyDoHuy)))>=5))
);
CREATE UNIQUE INDEX UX_KeHoachMoc_DeNghi ON KeHoachMoc(MaDeNghi) WHERE MaDeNghi IS NOT NULL;
CREATE UNIQUE INDEX UX_KeHoachMoc_DeXuat ON KeHoachMoc(MaDeXuat) WHERE MaDeXuat IS NOT NULL;
CREATE INDEX IX_KeHoachMoc_Khach ON KeHoachMoc(MaKhachHang,TrangThai);
CREATE INDEX IX_KeHoachMoc_ChuyenGia ON KeHoachMoc(MaChuyenGia,TrangThai);
CREATE TABLE PhienBanKeHoachMoc (
 MaPhienBan BIGINT IDENTITY PRIMARY KEY,
 MaKeHoach BIGINT NOT NULL REFERENCES KeHoachMoc(MaKeHoach),
 SoPhienBan INT NOT NULL,
 MaTacGia BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
 MaGui VARCHAR(36) NOT NULL,
 CachChia VARCHAR(20) NOT NULL,
 LyDoDieuChinh NVARCHAR(2000) NULL,
 TyLePhi DECIMAL(7,4) NOT NULL,
 QuyTacPhi VARCHAR(40) NOT NULL,
 NgayGui DATETIMEOFFSET(6) NOT NULL,
 CONSTRAINT UX_PhienBanKeHoachMoc_So UNIQUE(MaKeHoach,SoPhienBan),
 CONSTRAINT UX_PhienBanKeHoachMoc_Gui UNIQUE(MaKeHoach,MaGui),
 CONSTRAINT CK_PhienBanKeHoachMoc_So CHECK(SoPhienBan>=1),
 CONSTRAINT CK_PhienBanKeHoachMoc_Cach CHECK(CachChia IN('MANUAL','FORTNIGHT','MONTH')),
 CONSTRAINT CK_PhienBanKeHoachMoc_Phi CHECK(TyLePhi=0.1000 AND QuyTacPhi='DEDUCT_EXPERT_ALLOCATION_HALF_UP'),
 CONSTRAINT CK_PhienBanKeHoachMoc_LyDo CHECK((SoPhienBan=1 AND LyDoDieuChinh IS NULL) OR (SoPhienBan>1 AND LyDoDieuChinh IS NOT NULL AND LEN(LTRIM(RTRIM(LyDoDieuChinh)))>=5))
);
CREATE TABLE ChiTietKeHoachMoc (
 MaChiTiet BIGINT IDENTITY PRIMARY KEY,
 MaPhienBan BIGINT NOT NULL REFERENCES PhienBanKeHoachMoc(MaPhienBan),
 DinhDanhMoc VARCHAR(36) NOT NULL,
 ThuTu INT NOT NULL,
 TenMoc NVARCHAR(255) NOT NULL,
 CongViec NVARCHAR(MAX) NOT NULL,
 SanPham NVARCHAR(MAX) NOT NULL,
 TieuChi NVARCHAR(MAX) NOT NULL,
 DonViHan VARCHAR(10) NOT NULL,
 HanTuongDoi INT NOT NULL,
 GiaTri DECIMAL(18,0) NOT NULL,
 PhiDuKien DECIMAL(18,0) NOT NULL,
 ThucNhanDuKien DECIMAL(18,0) NOT NULL,
 SoLanChinhSua INT NOT NULL,
 CONSTRAINT UX_ChiTietKeHoachMoc_ThuTu UNIQUE(MaPhienBan,ThuTu),
 CONSTRAINT UX_ChiTietKeHoachMoc_DinhDanh UNIQUE(MaPhienBan,DinhDanhMoc),
 CONSTRAINT CK_ChiTietKeHoachMoc_So CHECK(ThuTu BETWEEN 1 AND 10 AND HanTuongDoi>0 AND SoLanChinhSua>=0),
 CONSTRAINT CK_ChiTietKeHoachMoc_DonVi CHECK(DonViHan IN('DAY','MONTH')),
 CONSTRAINT CK_ChiTietKeHoachMoc_Gia CHECK(GiaTri>0 AND GiaTri<=1000000000000 AND PhiDuKien>=0 AND PhiDuKien<=GiaTri AND ThucNhanDuKien=GiaTri-PhiDuKien)
);
