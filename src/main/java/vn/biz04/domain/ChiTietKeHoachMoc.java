package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;

@Entity @Table(name="ChiTietKeHoachMoc",uniqueConstraints={@UniqueConstraint(columnNames={"MaPhienBan","DinhDanhMoc"}),@UniqueConstraint(columnNames={"MaPhienBan","ThuTu"})}) @Immutable
public class ChiTietKeHoachMoc {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaChiTiet") public Long id;
    @Column(name="MaPhienBan",nullable=false) public Long version;
    @Column(name="DinhDanhMoc",nullable=false,length=36) public String stableKey;
    @Column(name="ThuTu",nullable=false) public int position;
    @Nationalized @Column(name="TenMoc",nullable=false,length=255) public String title;
    @Nationalized @Column(name="CongViec",nullable=false,columnDefinition="nvarchar(max)") public String work;
    @Nationalized @Column(name="SanPham",nullable=false,columnDefinition="nvarchar(max)") public String deliverable;
    @Nationalized @Column(name="TieuChi",nullable=false,columnDefinition="nvarchar(max)") public String criteria;
    @Column(name="DonViHan",nullable=false,length=10) public String unit;
    @Column(name="HanTuongDoi",nullable=false) public int due;
    @Column(name="GiaTri",nullable=false,precision=18,scale=0) public BigDecimal amount;
    @Column(name="PhiDuKien",nullable=false,precision=18,scale=0) public BigDecimal fee;
    @Column(name="ThucNhanDuKien",nullable=false,precision=18,scale=0) public BigDecimal net;
    @Column(name="SoLanChinhSua",nullable=false) public int revisions;
}
