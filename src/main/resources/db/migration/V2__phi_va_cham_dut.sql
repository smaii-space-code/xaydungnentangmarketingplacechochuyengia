ALTER TABLE [HopDong] ADD [TyLePhi] DECIMAL(18,4) NOT NULL CONSTRAINT [DF_HopDong_Phi] DEFAULT (0.10);
GO
ALTER TABLE [HopDong] ADD CONSTRAINT [CK_HopDong_Phi] CHECK ([TyLePhi] BETWEEN 0 AND 1);
ALTER TABLE [GiaoDich] ADD [PhiNenTang] DECIMAL(18,0) NOT NULL CONSTRAINT [DF_GD_Phi] DEFAULT (0);
ALTER TABLE [GiaoDich] ADD [ThucNhan] DECIMAL(18,0) NOT NULL CONSTRAINT [DF_GD_ThucNhan] DEFAULT (0);
ALTER TABLE [TranhChap] ADD [HanNopBangChung] DATETIME2(6) NULL;
CREATE TABLE [YeuCauChamDut] (
    [MaYeuCauChamDut] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MaHopDong] BIGINT NOT NULL,
    [MaMocNghiemThu] BIGINT NULL,
    [MaNguoiDeNghi] BIGINT NOT NULL,
    [MaNguoiDongY] BIGINT NULL,
    [LyDo] NVARCHAR(MAX) NOT NULL,
    [TrangThai] NVARCHAR(40) NOT NULL,
    [TienChuyenGia] DECIMAL(18,0) NOT NULL,
    [TienHoanKhach] DECIMAL(18,0) NOT NULL,
    [NgayDeNghi] DATETIME2(6) NOT NULL,
    [HanPhanHoi] DATETIME2(6) NOT NULL,
    [NgayPhanHoi] DATETIME2(6) NULL
);
ALTER TABLE [YeuCauChamDut] ADD CONSTRAINT [FK_ChamDut_MaHopDong] FOREIGN KEY ([MaHopDong]) REFERENCES [HopDong] ([MaHopDong]);
ALTER TABLE [YeuCauChamDut] ADD CONSTRAINT [FK_ChamDut_MaMocNghiemThu] FOREIGN KEY ([MaMocNghiemThu]) REFERENCES [MocNghiemThu] ([MaMocNghiemThu]);
ALTER TABLE [YeuCauChamDut] ADD CONSTRAINT [FK_ChamDut_MaNguoiDeNghi] FOREIGN KEY ([MaNguoiDeNghi]) REFERENCES [NguoiDung] ([MaNguoiDung]);
ALTER TABLE [YeuCauChamDut] ADD CONSTRAINT [FK_ChamDut_MaNguoiDongY] FOREIGN KEY ([MaNguoiDongY]) REFERENCES [NguoiDung] ([MaNguoiDung]);
CREATE UNIQUE INDEX [UX_ChamDut_DangCho] ON [YeuCauChamDut] ([MaHopDong]) WHERE [TrangThai] = N'CHO_PHAN_HOI';
