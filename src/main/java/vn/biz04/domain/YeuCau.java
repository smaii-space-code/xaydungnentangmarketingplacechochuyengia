package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="YeuCau")
public class YeuCau {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaYeuCau", nullable=false)
    private Long maYeuCau;

    @Column(name="MaKhachHang", nullable=false)
    private Long maKhachHang;

    @Column(name="MaDichVu", nullable=true)
    private Long maDichVu;

    @Column(name="TieuDe", nullable=false, length=255)
    @Nationalized
    private String tieuDe;

    @Column(name="MoTa", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String moTa;

    @Column(name="NganSach", nullable=false, precision=18, scale=0)
    private BigDecimal nganSach;

    @Column(name="HanDuKien", nullable=true)
    private LocalDateTime hanDuKien;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaDichVu", insertable=false, updatable=false)
    private DichVu maDichVuLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaKhachHang", insertable=false, updatable=false)
    private NguoiDung maKhachHangLienKet;

    public Long getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(Long value) { this.maYeuCau=value; }
    public Long getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(Long value) { this.maKhachHang=value; }
    public Long getMaDichVu() { return maDichVu; }
    public void setMaDichVu(Long value) { this.maDichVu=value; }
    public String getTieuDe() { return tieuDe; }
    public void setTieuDe(String value) { this.tieuDe=value; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String value) { this.moTa=value; }
    public BigDecimal getNganSach() { return nganSach; }
    public void setNganSach(BigDecimal value) { this.nganSach=value; }
    public LocalDateTime getHanDuKien() { return hanDuKien; }
    public void setHanDuKien(LocalDateTime value) { this.hanDuKien=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public DichVu getMaDichVuLienKet() { return maDichVuLienKet; }
    public NguoiDung getMaKhachHangLienKet() { return maKhachHangLienKet; }
}
