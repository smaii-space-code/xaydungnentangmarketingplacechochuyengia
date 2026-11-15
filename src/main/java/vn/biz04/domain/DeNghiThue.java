package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@Entity @Table(name="DeNghiThue")
public class DeNghiThue {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaDeNghi") public Long id;
    @Column(name="MaKhachHang",nullable=false) public Long customer;
    @Column(name="MaChuyenGia",nullable=false) public Long expert;
    @Column(name="MaDichVu") public Long source;
    @Column(name="MaGui",nullable=false,length=36) public String key;
    @Nationalized @Column(name="TieuDe",nullable=false,length=255) public String title;
    @Column(name="TrangThai",nullable=false,length=30) public String state;
    @Column(name="PhienBanHienTai",nullable=false) public int current;
    @Column(name="MaBenChoPhanHoi") public Long waiting;
    @Column(name="PhienBanDaThongNhat") public Integer agreed;
    @Column(name="MaNguoiQuyetDinh") public Long decider;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="ThoiDiemQuyetDinh",precision=6) public OffsetDateTime decided;
    @Nationalized @Column(name="LyDoTuChoi",length=2000) public String rejection;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayGui",nullable=false,precision=6) public OffsetDateTime sent;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayCapNhat",nullable=false,precision=6) public OffsetDateTime updated;
}
