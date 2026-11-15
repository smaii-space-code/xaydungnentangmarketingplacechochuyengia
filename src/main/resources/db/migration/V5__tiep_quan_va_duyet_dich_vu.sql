-- No identity deletion/recreation. Existing business FK values remain untouched.
ALTER TABLE NguoiDung ADD LoaiTaiKhoan VARCHAR(20) NOT NULL CONSTRAINT DF_NguoiDung_Loai DEFAULT 'CHINH_THUC',
    PhienBaoMat INT NOT NULL CONSTRAINT DF_NguoiDung_Phien DEFAULT 0;
GO
ALTER TABLE NguoiDung ADD CONSTRAINT CK_NguoiDung_Loai CHECK(LoaiTaiKhoan IN ('MAU','CHINH_THUC'));
CREATE TABLE TiepQuanTaiKhoan (
    MaNguoiDung BIGINT NOT NULL PRIMARY KEY REFERENCES NguoiDung(MaNguoiDung),
    TenGoc NVARCHAR(100) NOT NULL,
    EmailGoc NVARCHAR(254) NOT NULL,
    NguonGoc NVARCHAR(200) NOT NULL,
    BatDau DATETIMEOFFSET(6) NOT NULL,
    HoanTat DATETIMEOFFSET(6) NULL,
    TenDuKien NVARCHAR(100) NULL
);
-- Identify legacy seed records by exact paired name/email and expert role, never UI position.
INSERT INTO TiepQuanTaiKhoan(MaNguoiDung,TenGoc,EmailGoc,NguonGoc,BatDau)
SELECT MaNguoiDung,HoTen,Email,N'DemoData: tên và email seed khớp',TODATETIMEOFFSET(SYSUTCDATETIME(),'+00:00')
FROM NguoiDung WHERE LaChuyenGia=1 AND LaQuanTriVien=0 AND (
 (Email='chuyengia@biz04.local' AND HoTen=N'Chuyên gia thiết kế') OR
 (Email='noidung@biz04.local' AND HoTen=N'Chuyên gia nội dung') OR
 (Email='ngoai@biz04.local' AND HoTen=N'Tài khoản ngoài hợp đồng'));
UPDATE u SET LoaiTaiKhoan='MAU',TrangThai=N'CHO_TIEP_QUAN',PhienBaoMat=PhienBaoMat+1
FROM NguoiDung u JOIN TiepQuanTaiKhoan t ON t.MaNguoiDung=u.MaNguoiDung;
ALTER TABLE MaXacThucOTP DROP CONSTRAINT CK_OTP_Loai;
ALTER TABLE MaXacThucOTP ADD CONSTRAINT CK_OTP_Loai CHECK(LoaiOTP IN ('DANG_KY','DAT_LAI_MAT_KHAU','CAP_NHAT_THONG_TIN','TIEP_QUAN'));
ALTER TABLE DichVu ADD TrangThaiDuyet VARCHAR(20) NOT NULL CONSTRAINT DF_DichVu_Duyet DEFAULT 'CHUA_GUI',
    PhienBanNoiDung INT NOT NULL CONSTRAINT DF_DichVu_Phien DEFAULT 1,
    PhienBanDuyet INT NULL,
    XacNhanNoiDung BIT NOT NULL CONSTRAINT DF_DichVu_XacNhan DEFAULT 0,
    CanRaSoatLai BIT NOT NULL CONSTRAINT DF_DichVu_RaSoat DEFAULT 0,
    NguonNoiDung VARCHAR(20) NOT NULL CONSTRAINT DF_DichVu_Nguon DEFAULT 'NGUOI_DUNG',
    MaNguoiDuyet BIGINT NULL REFERENCES NguoiDung(MaNguoiDung),
    NgayDuyet DATETIMEOFFSET(6) NULL,
    LyDoDuyet NVARCHAR(2000) NULL;
GO
ALTER TABLE DichVu ADD CONSTRAINT CK_DichVu_Duyet CHECK(TrangThaiDuyet IN ('CHUA_GUI','CHO_DUYET','DA_DUYET','TU_CHOI'));
UPDATE d SET TrangThai=N'NHAP',CanRaSoatLai=1,NguonNoiDung='MAU'
FROM DichVu d JOIN TiepQuanTaiKhoan t ON t.MaNguoiDung=d.MaChuyenGia;
-- Existing articles have no administrator approval evidence: none are grandfathered as approved.
