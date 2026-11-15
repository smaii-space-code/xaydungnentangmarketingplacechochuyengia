package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@Entity @Table(name="HoSoChuyenGia")
public class HoSoChuyenGia {
    @Id @Column(name="MaNguoiDung") public Long userId;
    @Column(name="TrangThai",nullable=false,length=20) public String state="CHO_XAC_THUC";
    @Column(name="PhienBan",nullable=false) public int version;
    @Nationalized @Column(name="LinhVuc",nullable=false,length=100) public String field;
    @Nationalized @Column(name="GioiThieu",nullable=false,length=2000) public String bio;
    @Column(name="MaDangKy",nullable=false,length=36,unique=true) public String operation;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayTao",nullable=false,precision=6) public OffsetDateTime created;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayGui",precision=6) public OffsetDateTime submitted;
}
