-- Extension only: customer-to-expert offers are separate from YeuCau/DeXuat.
CREATE TABLE DeNghiThue (
    MaDeNghi BIGINT IDENTITY PRIMARY KEY,
    MaKhachHang BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
    MaChuyenGia BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
    MaDichVu BIGINT NULL REFERENCES DichVu(MaDichVu),
    MaGui VARCHAR(36) NOT NULL,
    TieuDe NVARCHAR(255) NOT NULL,
    TrangThai VARCHAR(30) NOT NULL,
    PhienBanHienTai INT NOT NULL,
    MaBenChoPhanHoi BIGINT NULL REFERENCES NguoiDung(MaNguoiDung),
    PhienBanDaThongNhat INT NULL,
    MaNguoiQuyetDinh BIGINT NULL REFERENCES NguoiDung(MaNguoiDung),
    ThoiDiemQuyetDinh DATETIMEOFFSET(6) NULL,
    LyDoTuChoi NVARCHAR(2000) NULL,
    NgayGui DATETIMEOFFSET(6) NOT NULL,
    NgayCapNhat DATETIMEOFFSET(6) NOT NULL,
    CONSTRAINT UX_DeNghiThue_MaGui UNIQUE(MaKhachHang,MaGui),
    CONSTRAINT CK_DeNghiThue_HaiBen CHECK(MaKhachHang<>MaChuyenGia),
    CONSTRAINT CK_DeNghiThue_PhienBan CHECK(PhienBanHienTai>=1),
    CONSTRAINT CK_DeNghiThue_TrangThai CHECK(
      (TrangThai='CHO_PHAN_HOI' AND MaBenChoPhanHoi IS NOT NULL AND MaBenChoPhanHoi IN(MaKhachHang,MaChuyenGia) AND PhienBanDaThongNhat IS NULL AND MaNguoiQuyetDinh IS NULL AND ThoiDiemQuyetDinh IS NULL AND LyDoTuChoi IS NULL)
      OR (TrangThai='DA_THONG_NHAT' AND MaBenChoPhanHoi IS NULL AND PhienBanDaThongNhat IS NOT NULL AND PhienBanDaThongNhat=PhienBanHienTai AND MaNguoiQuyetDinh IS NOT NULL AND MaNguoiQuyetDinh IN(MaKhachHang,MaChuyenGia) AND ThoiDiemQuyetDinh IS NOT NULL AND LyDoTuChoi IS NULL)
      OR (TrangThai='BI_TU_CHOI' AND MaBenChoPhanHoi IS NULL AND PhienBanDaThongNhat IS NULL AND MaNguoiQuyetDinh IS NOT NULL AND MaNguoiQuyetDinh IN(MaKhachHang,MaChuyenGia) AND ThoiDiemQuyetDinh IS NOT NULL AND LyDoTuChoi IS NOT NULL AND LEN(LTRIM(RTRIM(LyDoTuChoi)))>0))
);
CREATE INDEX IX_DeNghiThue_KhachHang ON DeNghiThue(MaKhachHang,NgayCapNhat DESC,MaDeNghi DESC);
CREATE INDEX IX_DeNghiThue_ChuyenGia ON DeNghiThue(MaChuyenGia,NgayCapNhat DESC,MaDeNghi DESC);
CREATE TABLE PhienBanDeNghiThue (
    MaPhienBan BIGINT IDENTITY PRIMARY KEY,
    MaDeNghi BIGINT NOT NULL REFERENCES DeNghiThue(MaDeNghi),
    SoPhienBan INT NOT NULL,
    MaTacGia BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
    MaGui VARCHAR(36) NOT NULL,
    PhamVi NVARCHAR(MAX) NOT NULL,
    SanPham NVARCHAR(MAX) NOT NULL,
    Gia DECIMAL(18,0) NOT NULL,
    SoNgay INT NOT NULL,
    LyDoDieuChinh NVARCHAR(2000) NULL,
    NgayGui DATETIMEOFFSET(6) NOT NULL,
    CONSTRAINT UX_PhienBanDeNghiThue_So UNIQUE(MaDeNghi,SoPhienBan),
    CONSTRAINT UX_PhienBanDeNghiThue_Gui UNIQUE(MaDeNghi,MaGui),
    CONSTRAINT CK_PhienBanDeNghiThue_So CHECK(SoPhienBan>=1),
    CONSTRAINT CK_PhienBanDeNghiThue_Gia CHECK(Gia>0 AND Gia<=1000000000000),
    CONSTRAINT CK_PhienBanDeNghiThue_Ngay CHECK(SoNgay BETWEEN 1 AND 730),
    CONSTRAINT CK_PhienBanDeNghiThue_LyDo CHECK((SoPhienBan=1 AND LyDoDieuChinh IS NULL) OR (SoPhienBan>1 AND LyDoDieuChinh IS NOT NULL AND LEN(LTRIM(RTRIM(LyDoDieuChinh)))>0))
);
