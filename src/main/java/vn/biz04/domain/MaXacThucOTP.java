package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.time.OffsetDateTime;

@Entity @Table(name="MaXacThucOTP")
public class MaXacThucOTP {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="MaOTP") public Long maOTP;
    @Column(name="MaNguoiDung",nullable=false) public Long maNguoiDung;
    @Nationalized @Column(name="Email",length=254,nullable=false) public String email;
    @Column(name="MaCodeHash",length=255,nullable=false) public String maCodeHash;
    @Column(name="MaYeuCau",length=64,unique=true,nullable=false) public String maYeuCau;
    @Column(name="LoaiOTP",length=30,nullable=false) public String loaiOTP;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="ThoiDiemTao",precision=6,nullable=false) public OffsetDateTime thoiDiemTao;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="ThoiDiemHetHan",precision=6,nullable=false) public OffsetDateTime thoiDiemHetHan;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="ThoiDiemSuDung",precision=6) public OffsetDateTime thoiDiemSuDung;
    @Column(name="SoLanThuSai",nullable=false) public int soLanThuSai;
    @Column(name="TrangThai",length=30,nullable=false) public String trangThai;
}
