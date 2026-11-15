package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="MocNghiemThu")
public class MocNghiemThu {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaMocNghiemThu", nullable=false)
    private Long maMocNghiemThu;

    @Column(name="MaHopDong", nullable=false)
    private Long maHopDong;

    @Column(name="ThuTu", nullable=false)
    private Integer thuTu;

    @Column(name="TenMoc", nullable=false, length=255)
    @Nationalized
    private String tenMoc;

    @Column(name="NoiDung", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDung;

    @Column(name="TieuChiNghiemThu", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String tieuChiNghiemThu;

    @Column(name="SoTien", nullable=false, precision=18, scale=0)
    private BigDecimal soTien;

    @Column(name="HanBanGiao")
    private LocalDateTime hanBanGiao;

    @Column(name="HanNghiemThu", nullable=true)
    private LocalDateTime hanNghiemThu;

    @Column(name="NgayNghiemThu", nullable=true)
    private LocalDateTime ngayNghiemThu;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaHopDong", insertable=false, updatable=false)
    private HopDong maHopDongLienKet;

    @Column(name="DinhDanhMoc",length=36) private String dinhDanhMoc;
    @Column(name="DonViHan",length=10) private String donViHan;
    @Column(name="HanTuongDoi") private Integer hanTuongDoi;
    @Column(name="LaMocCuoi") private Boolean laMocCuoi;
    @Nationalized @Column(name="SanPhamThoaThuan",columnDefinition="nvarchar(max)") private String sanPhamThoaThuan;
    @Column(name="SoLanChinhSuaToiDa") private Integer soLanChinhSuaToiDa;
    public String getDinhDanhMoc(){return dinhDanhMoc;} public void setDinhDanhMoc(String v){dinhDanhMoc=v;}
    public String getDonViHan(){return donViHan;} public void setDonViHan(String v){donViHan=v;}
    public Integer getHanTuongDoi(){return hanTuongDoi;} public void setHanTuongDoi(Integer v){hanTuongDoi=v;}
    public Boolean getLaMocCuoi(){return laMocCuoi;} public void setLaMocCuoi(Boolean v){laMocCuoi=v;}
    public String getSanPhamThoaThuan(){return sanPhamThoaThuan;} public void setSanPhamThoaThuan(String v){sanPhamThoaThuan=v;}
    public Integer getSoLanChinhSuaToiDa(){return soLanChinhSuaToiDa;} public void setSoLanChinhSuaToiDa(Integer v){soLanChinhSuaToiDa=v;}

    public Long getMaMocNghiemThu() { return maMocNghiemThu; }
    public void setMaMocNghiemThu(Long value) { this.maMocNghiemThu=value; }
    public Long getMaHopDong() { return maHopDong; }
    public void setMaHopDong(Long value) { this.maHopDong=value; }
    public Integer getThuTu() { return thuTu; }
    public void setThuTu(Integer value) { this.thuTu=value; }
    public String getTenMoc() { return tenMoc; }
    public void setTenMoc(String value) { this.tenMoc=value; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String value) { this.noiDung=value; }
    public String getTieuChiNghiemThu() { return tieuChiNghiemThu; }
    public void setTieuChiNghiemThu(String value) { this.tieuChiNghiemThu=value; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal value) { this.soTien=value; }
    public LocalDateTime getHanBanGiao() { return hanBanGiao; }
    public void setHanBanGiao(LocalDateTime value) { this.hanBanGiao=value; }
    public LocalDateTime getHanNghiemThu() { return hanNghiemThu; }
    public void setHanNghiemThu(LocalDateTime value) { this.hanNghiemThu=value; }
    public LocalDateTime getNgayNghiemThu() { return ngayNghiemThu; }
    public void setNgayNghiemThu(LocalDateTime value) { this.ngayNghiemThu=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public HopDong getMaHopDongLienKet() { return maHopDongLienKet; }
}
