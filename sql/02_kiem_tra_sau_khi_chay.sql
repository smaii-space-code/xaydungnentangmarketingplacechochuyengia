USE [BIZ04];
SELECT name, filter_definition FROM sys.indexes
WHERE name IN ('UX_GiaoDich_ThamChieu','UX_TranhChap_DangMo','UX_ChamDut_DangCho');
SELECT COUNT(*) AS SoNguoiDung FROM [NguoiDung];
SELECT COUNT(*) AS SoHopDong FROM [HopDong];
SELECT [LoaiGiaoDich], [TrangThai], COUNT(*) AS SoGiaoDich, SUM([SoTien]) AS TongTien
FROM [GiaoDich] GROUP BY [LoaiGiaoDich], [TrangThai];
SELECT [MaMocNghiemThu], COUNT(*) AS SoTranhChapChuaDong
FROM [TranhChap] WHERE [NgayDong] IS NULL
GROUP BY [MaMocNghiemThu] HAVING COUNT(*) > 1;
-- Truy vấn cuối phải không có dòng.
