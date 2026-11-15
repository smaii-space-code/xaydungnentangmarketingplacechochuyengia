package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@Entity @Table(name="BanChupHopDong",uniqueConstraints=@UniqueConstraint(columnNames={"MaHopDong","PhienBan"})) @Immutable
public class BanChupHopDong {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaBanChup") public Long id;
    @Column(name="MaHopDong",nullable=false) public Long contract;
    @Column(name="PhienBan",nullable=false) public int version;
    @Column(name="MaNguoiLap",nullable=false) public Long author;
    @Nationalized @Column(name="NoiDungJson",nullable=false,columnDefinition="nvarchar(max)") public String json;
    @Column(name="DauKiem",nullable=false,length=64) public String digest;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="ThoiDiem",nullable=false,precision=6) public OffsetDateTime created;
}
