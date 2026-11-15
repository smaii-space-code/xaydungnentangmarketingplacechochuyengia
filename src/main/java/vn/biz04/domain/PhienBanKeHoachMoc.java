package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity @Table(name="PhienBanKeHoachMoc",uniqueConstraints={@UniqueConstraint(columnNames={"MaKeHoach","SoPhienBan"}),@UniqueConstraint(columnNames={"MaKeHoach","MaGui"})}) @Immutable
public class PhienBanKeHoachMoc {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaPhienBan") public Long id;
    @Column(name="MaKeHoach",nullable=false) public Long plan;
    @Column(name="SoPhienBan",nullable=false) public int number;
    @Column(name="MaTacGia",nullable=false) public Long author;
    @Column(name="MaGui",nullable=false,length=36) public String operation;
    @Column(name="CachChia",nullable=false,length=20) public String mode;
    @Nationalized @Column(name="LyDoDieuChinh",length=2000) public String reason;
    @Column(name="TyLePhi",nullable=false,precision=7,scale=4) public BigDecimal rate;
    @Column(name="QuyTacPhi",nullable=false,length=40) public String feeRule;
    @Column(name="QuyTacHan",length=40) public String deadlineRule;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayGui",nullable=false,precision=6) public OffsetDateTime sent;
}
