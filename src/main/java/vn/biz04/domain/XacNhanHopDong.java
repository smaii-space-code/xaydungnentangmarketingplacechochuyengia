package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;
@Entity @Table(name="XacNhanHopDong",uniqueConstraints=@UniqueConstraint(columnNames={"MaHopDong","PhienBan","MaNguoiXacNhan"})) @Immutable
public class XacNhanHopDong {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaXacNhan") public Long id;
    @Column(name="MaHopDong",nullable=false) public Long contract;
    @Column(name="PhienBan",nullable=false) public int version;
    @Column(name="MaNguoiXacNhan",nullable=false) public Long actor;
    @Column(name="DauKiem",nullable=false,length=64) public String digest;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="ThoiDiem",nullable=false,precision=6) public OffsetDateTime at;
}
