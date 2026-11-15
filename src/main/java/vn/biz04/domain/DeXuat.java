package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="DeXuat")
public class DeXuat {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaDeXuat", nullable=false)
    private Long maDeXuat;

    @Column(name="MaYeuCau", nullable=false)
    private Long maYeuCau;

    @Column(name="MaChuyenGia", nullable=false)
    private Long maChuyenGia;

    @Column(name="PhuongAn", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String phuongAn;

    @Column(name="GiaDeXuat", nullable=false, precision=18, scale=0)
    private BigDecimal giaDeXuat;

    @Column(name="SoNgayThucHien", nullable=false)
    private Integer soNgayThucHien;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaYeuCau", insertable=false, updatable=false)
    private YeuCau maYeuCauLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaChuyenGia", insertable=false, updatable=false)
    private NguoiDung maChuyenGiaLienKet;

    public Long getMaDeXuat() { return maDeXuat; }
    public void setMaDeXuat(Long value) { this.maDeXuat=value; }
    public Long getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(Long value) { this.maYeuCau=value; }
    public Long getMaChuyenGia() { return maChuyenGia; }
    public void setMaChuyenGia(Long value) { this.maChuyenGia=value; }
    public String getPhuongAn() { return phuongAn; }
    public void setPhuongAn(String value) { this.phuongAn=value; }
    public BigDecimal getGiaDeXuat() { return giaDeXuat; }
    public void setGiaDeXuat(BigDecimal value) { this.giaDeXuat=value; }
    public Integer getSoNgayThucHien() { return soNgayThucHien; }
    public void setSoNgayThucHien(Integer value) { this.soNgayThucHien=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public YeuCau getMaYeuCauLienKet() { return maYeuCauLienKet; }
    public NguoiDung getMaChuyenGiaLienKet() { return maChuyenGiaLienKet; }
}
