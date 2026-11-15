package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name="NguoiDung")
public class NguoiDung {
    @Column(name="NguonDangKy",length=20) private String nguonDangKy;
    public String getNguonDangKy(){return nguonDangKy;} public void setNguonDangKy(String v){nguonDangKy=v;}
    @Column(name="LoaiTaiKhoan",nullable=false,length=20) private String loaiTaiKhoan="CHINH_THUC";
    @Column(name="PhienBaoMat",nullable=false) private int phienBaoMat;
    public String getLoaiTaiKhoan(){return loaiTaiKhoan;} public void setLoaiTaiKhoan(String v){loaiTaiKhoan=v;}
    public int getPhienBaoMat(){return phienBaoMat;} public void setPhienBaoMat(int v){phienBaoMat=v;}
    @Column(name="TrangThaiEmail",nullable=false,length=30)
    private String trangThaiEmail="CHUYEN_TIEP";
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="ThoiDiemXacThucEmail",precision=6)
    private OffsetDateTime thoiDiemXacThucEmail;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="NgayTao",nullable=false,precision=6)
    private OffsetDateTime ngayTao=OffsetDateTime.now(java.time.ZoneOffset.UTC);
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="NgayCapNhat",nullable=false,precision=6)
    private OffsetDateTime ngayCapNhat=ngayTao;
    public String getTrangThaiEmail(){return trangThaiEmail;}
    public void setTrangThaiEmail(String v){trangThaiEmail=v;}
    public OffsetDateTime getThoiDiemXacThucEmail(){return thoiDiemXacThucEmail;}
    public void setThoiDiemXacThucEmail(OffsetDateTime v){thoiDiemXacThucEmail=v;}
    public OffsetDateTime getNgayTao(){return ngayTao;}
    public void setNgayTao(OffsetDateTime v){ngayTao=v;}
    public OffsetDateTime getNgayCapNhat(){return ngayCapNhat;}
    public void setNgayCapNhat(OffsetDateTime v){ngayCapNhat=v;}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaNguoiDung", nullable=false)
    private Long maNguoiDung;

    @Column(name="HoTen", nullable=false, length=100)
    @Nationalized
    private String hoTen;

    @Column(name="Email", nullable=false, length=254)
    @Nationalized
    private String email;

    @Column(name="MatKhauBam", nullable=false, length=255)
    @Nationalized
    private String matKhauBam;

    @Column(name="LaKhachHang", nullable=false)
    private Boolean laKhachHang;

    @Column(name="LaChuyenGia", nullable=false)
    private Boolean laChuyenGia;

    @Column(name="LaQuanTriVien", nullable=false)
    private Boolean laQuanTriVien;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    public Long getMaNguoiDung() { return maNguoiDung; }
    public void setMaNguoiDung(Long value) { this.maNguoiDung=value; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String value) { this.hoTen=value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email=value; }
    public String getMatKhauBam() { return matKhauBam; }
    public void setMatKhauBam(String value) { this.matKhauBam=value; }
    public Boolean getLaKhachHang() { return laKhachHang; }
    public void setLaKhachHang(Boolean value) { this.laKhachHang=value; }
    public Boolean getLaChuyenGia() { return laChuyenGia; }
    public void setLaChuyenGia(Boolean value) { this.laChuyenGia=value; }
    public Boolean getLaQuanTriVien() { return laQuanTriVien; }
    public void setLaQuanTriVien(Boolean value) { this.laQuanTriVien=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
}
