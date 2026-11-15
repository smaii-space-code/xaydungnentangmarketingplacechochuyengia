package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="DichVu")
public class DichVu {
    @Column(name="TrangThaiDuyet",nullable=false,length=20) private String trangThaiDuyet="CHUA_GUI";
    @Column(name="PhienBanNoiDung",nullable=false) private int phienBanNoiDung=1;
    @Column(name="PhienBanDuyet") private Integer phienBanDuyet;
    @Column(name="XacNhanNoiDung",nullable=false) private boolean xacNhanNoiDung;
    @Column(name="CanRaSoatLai",nullable=false) private boolean canRaSoatLai;
    @Column(name="NguonNoiDung",nullable=false,length=20) private String nguonNoiDung="NGUOI_DUNG";
    @Column(name="MaNguoiDuyet") private Long maNguoiDuyet;
    @org.hibernate.annotations.TimeZoneStorage(org.hibernate.annotations.TimeZoneStorageType.NATIVE)
    @Column(name="NgayDuyet",precision=6) private java.time.OffsetDateTime ngayDuyet;
    @Nationalized @Column(name="LyDoDuyet",length=2000) private String lyDoDuyet;
    public String getTrangThaiDuyet(){return trangThaiDuyet;} public void setTrangThaiDuyet(String v){trangThaiDuyet=v;}
    public int getPhienBanNoiDung(){return phienBanNoiDung;} public void setPhienBanNoiDung(int v){phienBanNoiDung=v;}
    public Integer getPhienBanDuyet(){return phienBanDuyet;} public void setPhienBanDuyet(Integer v){phienBanDuyet=v;}
    public boolean isXacNhanNoiDung(){return xacNhanNoiDung;} public void setXacNhanNoiDung(boolean v){xacNhanNoiDung=v;}
    public boolean isCanRaSoatLai(){return canRaSoatLai;} public void setCanRaSoatLai(boolean v){canRaSoatLai=v;}
    public String getNguonNoiDung(){return nguonNoiDung;} public void setNguonNoiDung(String v){nguonNoiDung=v;}
    public Long getMaNguoiDuyet(){return maNguoiDuyet;} public void setMaNguoiDuyet(Long v){maNguoiDuyet=v;}
    public java.time.OffsetDateTime getNgayDuyet(){return ngayDuyet;} public void setNgayDuyet(java.time.OffsetDateTime v){ngayDuyet=v;}
    public String getLyDoDuyet(){return lyDoDuyet;} public void setLyDoDuyet(String v){lyDoDuyet=v;}
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaDichVu", nullable=false)
    private Long maDichVu;

    @Column(name="MaChuyenGia", nullable=false)
    private Long maChuyenGia;

    @Column(name="TenDichVu", nullable=false, length=255)
    @Nationalized
    private String tenDichVu;

    @Column(name="LinhVuc", nullable=false, length=100)
    @Nationalized
    private String linhVuc;

    @Column(name="MoTa", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String moTa;

    @Column(name="GiaThamKhao", nullable=false, precision=18, scale=0)
    private BigDecimal giaThamKhao;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaChuyenGia", insertable=false, updatable=false)
    private NguoiDung maChuyenGiaLienKet;

    public Long getMaDichVu() { return maDichVu; }
    public void setMaDichVu(Long value) { this.maDichVu=value; }
    public Long getMaChuyenGia() { return maChuyenGia; }
    public void setMaChuyenGia(Long value) { this.maChuyenGia=value; }
    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String value) { this.tenDichVu=value; }
    public String getLinhVuc() { return linhVuc; }
    public void setLinhVuc(String value) { this.linhVuc=value; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String value) { this.moTa=value; }
    public BigDecimal getGiaThamKhao() { return giaThamKhao; }
    public void setGiaThamKhao(BigDecimal value) { this.giaThamKhao=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public NguoiDung getMaChuyenGiaLienKet() { return maChuyenGiaLienKet; }
}
