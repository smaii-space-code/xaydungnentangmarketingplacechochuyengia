package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Entity @Table(name="PhienBanDeNghiThue") @org.hibernate.annotations.Immutable
public class PhienBanDeNghiThue {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaPhienBan") public Long id;
    @Column(name="MaDeNghi",nullable=false) public Long offer;
    @Column(name="SoPhienBan",nullable=false) public int number;
    @Column(name="MaTacGia",nullable=false) public Long author;
    @Column(name="MaGui",nullable=false,length=36) public String key;
    @Nationalized @Column(name="PhamVi",nullable=false,columnDefinition="nvarchar(max)") public String scope;
    @Nationalized @Column(name="SanPham",nullable=false,columnDefinition="nvarchar(max)") public String deliverable;
    @Column(name="Gia",nullable=false,precision=18,scale=0) public BigDecimal price;
    @Column(name="SoNgay",nullable=false) public int days;
    @Nationalized @Column(name="LyDoDieuChinh",length=2000) public String reason;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayGui",nullable=false,precision=6) public OffsetDateTime sent;
}
