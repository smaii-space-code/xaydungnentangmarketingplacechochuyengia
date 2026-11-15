package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@org.hibernate.annotations.Immutable
@Entity @Table(name="QuyetDinhChuyenGia",uniqueConstraints=@UniqueConstraint(columnNames={"MaNguoiDung","PhienBan"}))
public class QuyetDinhChuyenGia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaQuyetDinh") public Long id;
    @Column(name="MaNguoiDung",nullable=false) public Long userId;
    @Column(name="PhienBan",nullable=false) public int version;
    @Column(name="MaAdmin",nullable=false) public Long admin;
    @Column(name="ChapNhan",nullable=false) public boolean accepted;
    @Nationalized @Column(name="LyDo",length=2000) public String reason;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayXuLy",nullable=false,precision=6) public OffsetDateTime decided;
}
