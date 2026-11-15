-- Reuse HopDong/MocNghiemThu. Existing rows receive nullable extension fields only.
DECLARE @requestUnique SYSNAME;
SELECT @requestUnique=kc.name FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id=kc.parent_object_id AND ic.index_id=kc.unique_index_id
JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
WHERE kc.parent_object_id=OBJECT_ID('dbo.HopDong') AND kc.type='UQ' AND c.name='MaYeuCau'
AND NOT EXISTS(SELECT 1 FROM sys.index_columns other WHERE other.object_id=ic.object_id AND other.index_id=ic.index_id AND other.key_ordinal>1);
IF @requestUnique IS NOT NULL
BEGIN
 DECLARE @dropRequestUnique NVARCHAR(500)=N'ALTER TABLE dbo.HopDong DROP CONSTRAINT '+QUOTENAME(@requestUnique);
 EXEC sp_executesql @dropRequestUnique;
END;
GO
ALTER TABLE HopDong ALTER COLUMN MaYeuCau BIGINT NULL;
ALTER TABLE HopDong ALTER COLUMN MaDeXuat BIGINT NULL;
CREATE UNIQUE INDEX UX_HopDong_YeuCau ON HopDong(MaYeuCau) WHERE MaYeuCau IS NOT NULL;
ALTER TABLE HopDong ADD MaKeHoach BIGINT NULL, PhienBanKeHoach INT NULL,
 MaKhachHang BIGINT NULL, MaChuyenGia BIGINT NULL,
 QuyTacPhi VARCHAR(40) NULL, QuyTacBatDau VARCHAR(40) NULL,
 DauKiemNoiDung VARCHAR(64) NULL, NgayLap DATETIMEOFFSET(6) NULL, ThoiDiemBatDau DATETIMEOFFSET(6) NULL;
GO
ALTER TABLE HopDong ADD CONSTRAINT FK_HopDong_KeHoach FOREIGN KEY(MaKeHoach) REFERENCES KeHoachMoc(MaKeHoach),
 CONSTRAINT FK_HopDong_PhienBanKeHoach FOREIGN KEY(MaKeHoach,PhienBanKeHoach) REFERENCES PhienBanKeHoachMoc(MaKeHoach,SoPhienBan),
 CONSTRAINT FK_HopDong_Khach FOREIGN KEY(MaKhachHang) REFERENCES NguoiDung(MaNguoiDung),
 CONSTRAINT FK_HopDong_ChuyenGia FOREIGN KEY(MaChuyenGia) REFERENCES NguoiDung(MaNguoiDung),
 CONSTRAINT CK_HopDong_Nguon CHECK(
 (MaKeHoach IS NULL AND MaYeuCau IS NOT NULL AND MaDeXuat IS NOT NULL AND PhienBanKeHoach IS NULL AND MaKhachHang IS NULL AND MaChuyenGia IS NULL AND QuyTacPhi IS NULL AND QuyTacBatDau IS NULL AND DauKiemNoiDung IS NULL AND NgayLap IS NULL AND ThoiDiemBatDau IS NULL)
 OR (MaKeHoach IS NOT NULL AND PhienBanKeHoach IS NOT NULL AND PhienBanKeHoach>=1 AND MaKhachHang IS NOT NULL AND MaChuyenGia IS NOT NULL AND MaKhachHang<>MaChuyenGia AND QuyTacPhi='DEDUCT_EXPERT_ALLOCATION_HALF_UP' AND QuyTacPhi IS NOT NULL AND QuyTacBatDau='FIRST_ESCROW_SUCCESS' AND QuyTacBatDau IS NOT NULL AND NgayLap IS NOT NULL AND DauKiemNoiDung IS NOT NULL AND LEN(DauKiemNoiDung)=64 AND ((MaYeuCau IS NULL AND MaDeXuat IS NULL) OR (MaYeuCau IS NOT NULL AND MaDeXuat IS NOT NULL))));
CREATE UNIQUE INDEX UX_HopDong_KeHoach ON HopDong(MaKeHoach) WHERE MaKeHoach IS NOT NULL;
CREATE INDEX IX_HopDong_CacBen ON HopDong(MaKhachHang,MaChuyenGia,TrangThai);
ALTER TABLE MocNghiemThu ALTER COLUMN HanBanGiao DATETIME2(6) NULL;
ALTER TABLE MocNghiemThu ADD DinhDanhMoc VARCHAR(36) NULL, DonViHan VARCHAR(10) NULL,
 HanTuongDoi INT NULL, LaMocCuoi BIT NULL, SanPhamThoaThuan NVARCHAR(MAX) NULL, SoLanChinhSuaToiDa INT NULL;
GO
ALTER TABLE MocNghiemThu ADD CONSTRAINT CK_MocNghiemThu_HanMoi CHECK(
 (DinhDanhMoc IS NULL AND DonViHan IS NULL AND HanTuongDoi IS NULL AND LaMocCuoi IS NULL AND SanPhamThoaThuan IS NULL AND SoLanChinhSuaToiDa IS NULL AND HanBanGiao IS NOT NULL)
 OR (DinhDanhMoc IS NOT NULL AND LEN(DinhDanhMoc)=36 AND DonViHan IS NOT NULL AND DonViHan IN('DAY','MONTH') AND HanTuongDoi IS NOT NULL AND HanTuongDoi>0 AND LaMocCuoi IS NOT NULL AND SanPhamThoaThuan IS NOT NULL AND SoLanChinhSuaToiDa IS NOT NULL AND SoLanChinhSuaToiDa>=0));
CREATE TABLE BanChupHopDong (
 MaBanChup BIGINT IDENTITY PRIMARY KEY,
 MaHopDong BIGINT NOT NULL REFERENCES HopDong(MaHopDong),
 PhienBan INT NOT NULL,
 MaNguoiLap BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
 NoiDungJson NVARCHAR(MAX) NOT NULL,
 DauKiem VARCHAR(64) NOT NULL,
 ThoiDiem DATETIMEOFFSET(6) NOT NULL,
 CONSTRAINT UX_BanChupHopDong_PhienBan UNIQUE(MaHopDong,PhienBan),
 CONSTRAINT CK_BanChupHopDong_NoiDung CHECK(PhienBan>=1 AND LEN(DauKiem)=64 AND ISJSON(NoiDungJson)=1)
);
CREATE TABLE XacNhanHopDong (
 MaXacNhan BIGINT IDENTITY PRIMARY KEY,
 MaHopDong BIGINT NOT NULL REFERENCES HopDong(MaHopDong),
 PhienBan INT NOT NULL,
 MaNguoiXacNhan BIGINT NOT NULL REFERENCES NguoiDung(MaNguoiDung),
 DauKiem VARCHAR(64) NOT NULL,
 ThoiDiem DATETIMEOFFSET(6) NOT NULL,
 CONSTRAINT UX_XacNhanHopDong_Nguoi UNIQUE(MaHopDong,PhienBan,MaNguoiXacNhan),
 CONSTRAINT FK_XacNhanHopDong_PhienBan FOREIGN KEY(MaHopDong,PhienBan) REFERENCES BanChupHopDong(MaHopDong,PhienBan),
 CONSTRAINT CK_XacNhanHopDong_Dau CHECK(PhienBan>=1 AND LEN(DauKiem)=64)
);
ALTER TABLE PhienBanKeHoachMoc ADD QuyTacHan VARCHAR(40) NULL;
GO
ALTER TABLE PhienBanKeHoachMoc ADD CONSTRAINT CK_PhienBanKeHoachMoc_Han CHECK(QuyTacHan IS NULL OR QuyTacHan IN('DAY_OFFSET','MONTH_CAPPED_PROJECT_END'));
