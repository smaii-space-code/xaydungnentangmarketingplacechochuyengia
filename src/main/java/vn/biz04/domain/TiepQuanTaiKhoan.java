package vn.biz04.domain;
import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;

@Entity @Table(name="TiepQuanTaiKhoan")
public class TiepQuanTaiKhoan {
    @Id @Column(name="MaNguoiDung") public Long user;
    @Nationalized @Column(name="TenGoc",nullable=false,length=100) public String originalName;
    @Nationalized @Column(name="EmailGoc",nullable=false,length=254) public String originalEmail;
    @Nationalized @Column(name="NguonGoc",nullable=false,length=200) public String origin;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="BatDau",nullable=false,precision=6) public OffsetDateTime started;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="HoanTat",precision=6) public OffsetDateTime completed;
    @Nationalized @Column(name="TenDuKien",length=100) public String pendingName;
}
