package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="BanGiao")
public class BanGiao {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaBanGiao", nullable=false)
    private Long maBanGiao;

    @Column(name="MaMocNghiemThu", nullable=false)
    private Long maMocNghiemThu;

    @Column(name="LanBanGiao", nullable=false)
    private Integer lanBanGiao;

    @Column(name="MoTa", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String moTa;

    @Column(name="DuongDanSanPham", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String duongDanSanPham;

    @Column(name="NgayGui", nullable=false)
    private LocalDateTime ngayGui;

    @Column(name="KetQuaXuLy", nullable=false, length=40)
    @Nationalized
    private String ketQuaXuLy;

    @Column(name="NoiDungYeuCauSua", nullable=true, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDungYeuCauSua;

    @Column(name="NgayXuLy", nullable=true)
    private LocalDateTime ngayXuLy;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaMocNghiemThu", insertable=false, updatable=false)
    private MocNghiemThu maMocNghiemThuLienKet;

    public Long getMaBanGiao() { return maBanGiao; }
    public void setMaBanGiao(Long value) { this.maBanGiao=value; }
    public Long getMaMocNghiemThu() { return maMocNghiemThu; }
    public void setMaMocNghiemThu(Long value) { this.maMocNghiemThu=value; }
    public Integer getLanBanGiao() { return lanBanGiao; }
    public void setLanBanGiao(Integer value) { this.lanBanGiao=value; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String value) { this.moTa=value; }
    public String getDuongDanSanPham() { return duongDanSanPham; }
    public void setDuongDanSanPham(String value) { this.duongDanSanPham=value; }
    public LocalDateTime getNgayGui() { return ngayGui; }
    public void setNgayGui(LocalDateTime value) { this.ngayGui=value; }
    public String getKetQuaXuLy() { return ketQuaXuLy; }
    public void setKetQuaXuLy(String value) { this.ketQuaXuLy=value; }
    public String getNoiDungYeuCauSua() { return noiDungYeuCauSua; }
    public void setNoiDungYeuCauSua(String value) { this.noiDungYeuCauSua=value; }
    public LocalDateTime getNgayXuLy() { return ngayXuLy; }
    public void setNgayXuLy(LocalDateTime value) { this.ngayXuLy=value; }
    public MocNghiemThu getMaMocNghiemThuLienKet() { return maMocNghiemThuLienKet; }
}
