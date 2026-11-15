package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@org.hibernate.annotations.Immutable
@Entity @Table(name="PhienBanHoSoChuyenGia",uniqueConstraints={@UniqueConstraint(columnNames={"MaNguoiDung","PhienBan"}),@UniqueConstraint(columnNames={"MaNguoiDung","MaThaoTac"})})
public class PhienBanHoSoChuyenGia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaBanChup") public Long id;
    @Column(name="MaNguoiDung",nullable=false) public Long userId;
    @Column(name="PhienBan",nullable=false) public int version;
    @Nationalized @Column(name="HoTen",nullable=false,length=100) public String name;
    @Nationalized @Column(name="Email",nullable=false,length=254) public String email;
    @Column(name="SoDienThoai",nullable=false,length=10) public String phone;
    @Nationalized @Column(name="LinhVuc",nullable=false,length=100) public String field;
    @Nationalized @Column(name="GioiThieu",nullable=false,length=2000) public String bio;
    @Column(name="MaThaoTac",nullable=false,length=36) public String operation;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayGui",nullable=false,precision=6) public OffsetDateTime submitted;
}
