package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Pre-contract negotiation; deliberately independent from HopDong/MocNghiemThu. */
@Entity @Table(name="KeHoachMoc")
public class KeHoachMoc {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaKeHoach") public Long id;
    @Column(name="MaDeNghi") public Long hire;
    @Column(name="MaDeXuat") public Long proposal;
    @Column(name="DauVanNguon",nullable=false,length=64) public String fingerprint;
    @Column(name="PhienBanNguon") public Integer sourceVersion;
    @Column(name="MaKhachHang",nullable=false) public Long customer;
    @Column(name="MaChuyenGia",nullable=false) public Long expert;
    @Nationalized @Column(name="TenKhachHang",nullable=false,length=255) public String customerName;
    @Nationalized @Column(name="TenChuyenGia",nullable=false,length=255) public String expertName;
    @Nationalized @Column(name="TieuDeNguon",nullable=false,length=255) public String title;
    @Nationalized @Column(name="PhamViNguon",nullable=false,columnDefinition="nvarchar(max)") public String scope;
    @Nationalized @Column(name="SanPhamNguon",nullable=false,columnDefinition="nvarchar(max)") public String deliverable;
    @Column(name="TongGiaThue",nullable=false,precision=18,scale=0) public BigDecimal total;
    @Column(name="SoNgayNguon",nullable=false) public int days;
    @Column(name="TrangThai",nullable=false,length=30) public String state;
    @Column(name="PhienBanHienTai",nullable=false) public int current;
    @Column(name="MaBenChoPhanHoi") public Long waiting;
    @Column(name="PhienBanDaThongNhat") public Integer agreed;
    @Column(name="MaNguoiQuyetDinh") public Long decider;
    @Nationalized @Column(name="LyDoHuy",length=2000) public String cancellation;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="ThoiDiemQuyetDinh",precision=6) public OffsetDateTime decided;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayTao",nullable=false,precision=6) public OffsetDateTime created;
}
