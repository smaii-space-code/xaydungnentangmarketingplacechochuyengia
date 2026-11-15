package vn.biz04.domain;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="OtpGioiHan")
public class OtpGioiHan {
    @Id @Column(name="Khoa",length=64) public String khoa;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="BatDau",precision=6,nullable=false) public OffsetDateTime batDau;
    @Column(name="SoLuong",nullable=false) public int soLuong;
}
