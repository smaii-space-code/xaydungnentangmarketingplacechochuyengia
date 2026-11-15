-- UC01: mở rộng có kiểm soát; giữ nguyên V1/V2 và dữ liệu nghiệp vụ.
IF EXISTS (SELECT LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS FROM NguoiDung GROUP BY LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS HAVING COUNT(*) > 1)
    THROW 51000, N'Email trùng sau chuẩn hóa. Cần đối chiếu thủ công; migration không xóa/ghép tài khoản.', 1;
ALTER TABLE NguoiDung ADD
    TrangThaiEmail VARCHAR(30) NOT NULL CONSTRAINT DF_NguoiDung_EmailState DEFAULT 'CHUYEN_TIEP',
    ThoiDiemXacThucEmail DATETIMEOFFSET(6) NULL,
    NgayTao DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_NguoiDung_NgayTao DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00'),
    NgayCapNhat DATETIMEOFFSET(6) NOT NULL CONSTRAINT DF_NguoiDung_NgayCapNhat DEFAULT TODATETIMEOFFSET(SYSUTCDATETIME(), '+00:00');
GO
-- Ngày tài khoản cũ là thời điểm chuyển tiếp, không suy đoán ngày đăng ký gốc.
UPDATE NguoiDung SET Email = LOWER(LTRIM(RTRIM(Email)));
ALTER TABLE NguoiDung ADD EmailChuan AS (LOWER(LTRIM(RTRIM(Email))) COLLATE Latin1_General_100_CI_AS) PERSISTED;
GO
CREATE UNIQUE INDEX UX_NguoiDung_EmailChuan ON NguoiDung(EmailChuan);
CREATE TABLE HoSoKhachHang (
    MaNguoiDung BIGINT NOT NULL PRIMARY KEY REFERENCES NguoiDung(MaNguoiDung),
    SoDienThoai VARCHAR(10) NULL,
    LoaiKhachHang VARCHAR(20) NOT NULL DEFAULT 'CA_NHAN',
    TenToChuc NVARCHAR(200) NULL,
    DiaChi NVARCHAR(250) NULL,
    GioiThieu NVARCHAR(2000) NULL,
    SoCccdMaHoa VARCHAR(255) NULL,
    CccdBonSoCuoi VARCHAR(4) NULL,
    CONSTRAINT CK_HoSo_Loai CHECK (LoaiKhachHang IN ('CA_NHAN','TO_CHUC')),
    CONSTRAINT CK_HoSo_ToChuc CHECK (LoaiKhachHang='CA_NHAN' OR LEN(LTRIM(RTRIM(TenToChuc)))>0 AND TenToChuc IS NOT NULL),
    CONSTRAINT CK_HoSo_Phone CHECK (SoDienThoai IS NULL OR (LEN(SoDienThoai)=10 AND SoDienThoai COLLATE Latin1_General_100_BIN2 LIKE '0[35789]%' AND SoDienThoai COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^0-9]%')),
    CONSTRAINT CK_HoSo_Cccd CHECK ((SoCccdMaHoa IS NULL AND CccdBonSoCuoi IS NULL) OR (SoCccdMaHoa IS NOT NULL AND LEN(CccdBonSoCuoi)=4 AND CccdBonSoCuoi COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^0-9]%'))
);
INSERT INTO HoSoKhachHang(MaNguoiDung) SELECT MaNguoiDung FROM NguoiDung WHERE LaKhachHang=1;
CREATE TABLE MaXacThucOTP (
    MaOTP BIGINT IDENTITY PRIMARY KEY,
    MaNguoiDung BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
    Email NVARCHAR(254) NOT NULL,
    MaCodeHash VARCHAR(255) NOT NULL,
    MaYeuCau VARCHAR(64) NOT NULL UNIQUE,
    LoaiOTP VARCHAR(30) NOT NULL,
    ThoiDiemTao DATETIMEOFFSET(6) NOT NULL,
    ThoiDiemHetHan DATETIMEOFFSET(6) NOT NULL,
    ThoiDiemSuDung DATETIMEOFFSET(6) NULL,
    SoLanThuSai INT NOT NULL DEFAULT 0 CHECK (SoLanThuSai BETWEEN 0 AND 5),
    TrangThai VARCHAR(30) NOT NULL,
    CONSTRAINT CK_OTP_Loai CHECK (LoaiOTP IN ('DANG_KY','DAT_LAI_MAT_KHAU','CAP_NHAT_THONG_TIN')),
    CONSTRAINT CK_OTP_State CHECK (TrangThai IN ('CHO_XAC_THUC','DA_SU_DUNG','HET_HAN','BI_HUY'))
);
CREATE UNIQUE INDEX UX_OTP_Cho ON MaXacThucOTP(MaNguoiDung,LoaiOTP) WHERE TrangThai='CHO_XAC_THUC';
CREATE INDEX IX_OTP_Account ON MaXacThucOTP(MaNguoiDung,LoaiOTP,ThoiDiemTao);
-- Khóa kỹ thuật chung để tuần tự hóa tạo challenge và cập nhật hạn mức đa chiều.
CREATE TABLE OtpDieuPhoi (Id BIGINT PRIMARY KEY);
INSERT INTO OtpDieuPhoi(Id) VALUES(1);
CREATE TABLE OtpGioiHan (
    Khoa VARCHAR(64) PRIMARY KEY,
    BatDau DATETIMEOFFSET(6) NOT NULL,
    SoLuong INT NOT NULL
);
