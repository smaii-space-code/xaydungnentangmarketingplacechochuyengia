-- Profile extension keyed by the expert account, including experts who predate V9.
CREATE TABLE AnhDaiDienChuyenGia (
    MaNguoiDung BIGINT NOT NULL PRIMARY KEY REFERENCES NguoiDung(MaNguoiDung),
    ThamChieuTep VARCHAR(64) NOT NULL
);
