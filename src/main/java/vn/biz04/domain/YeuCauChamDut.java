package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="YeuCauChamDut")
public class YeuCauChamDut {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaYeuCauChamDut", nullable=false)
    private Long maYeuCauChamDut;

    @Column(name="MaHopDong", nullable=false)
    private Long maHopDong;

    @Column(name="MaMocNghiemThu", nullable=true)
    private Long maMocNghiemThu;

    @Column(name="MaNguoiDeNghi", nullable=false)
    private Long maNguoiDeNghi;

    @Column(name="MaNguoiDongY", nullable=true)
    private Long maNguoiDongY;

    @Column(name="LyDo", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String lyDo;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @Column(name="TienChuyenGia", nullable=false, precision=18, scale=0)
    private BigDecimal tienChuyenGia;

    @Column(name="TienHoanKhach", nullable=false, precision=18, scale=0)
    private BigDecimal tienHoanKhach;

    @Column(name="NgayDeNghi", nullable=false)
    private LocalDateTime ngayDeNghi;

    @Column(name="HanPhanHoi", nullable=false)
    private LocalDateTime hanPhanHoi;

    @Column(name="NgayPhanHoi", nullable=true)
    private LocalDateTime ngayPhanHoi;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaHopDong", insertable=false, updatable=false)
    private HopDong maHopDongLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaMocNghiemThu", insertable=false, updatable=false)
    private MocNghiemThu maMocNghiemThuLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiDeNghi", insertable=false, updatable=false)
    private NguoiDung maNguoiDeNghiLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiDongY", insertable=false, updatable=false)
    private NguoiDung maNguoiDongYLienKet;

    public Long getMaYeuCauChamDut() { return maYeuCauChamDut; }
    public void setMaYeuCauChamDut(Long value) { this.maYeuCauChamDut=value; }
    public Long getMaHopDong() { return maHopDong; }
    public void setMaHopDong(Long value) { this.maHopDong=value; }
    public Long getMaMocNghiemThu() { return maMocNghiemThu; }
    public void setMaMocNghiemThu(Long value) { this.maMocNghiemThu=value; }
    public Long getMaNguoiDeNghi() { return maNguoiDeNghi; }
    public void setMaNguoiDeNghi(Long value) { this.maNguoiDeNghi=value; }
    public Long getMaNguoiDongY() { return maNguoiDongY; }
    public void setMaNguoiDongY(Long value) { this.maNguoiDongY=value; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String value) { this.lyDo=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public BigDecimal getTienChuyenGia() { return tienChuyenGia; }
    public void setTienChuyenGia(BigDecimal value) { this.tienChuyenGia=value; }
    public BigDecimal getTienHoanKhach() { return tienHoanKhach; }
    public void setTienHoanKhach(BigDecimal value) { this.tienHoanKhach=value; }
    public LocalDateTime getNgayDeNghi() { return ngayDeNghi; }
    public void setNgayDeNghi(LocalDateTime value) { this.ngayDeNghi=value; }
    public LocalDateTime getHanPhanHoi() { return hanPhanHoi; }
    public void setHanPhanHoi(LocalDateTime value) { this.hanPhanHoi=value; }
    public LocalDateTime getNgayPhanHoi() { return ngayPhanHoi; }
    public void setNgayPhanHoi(LocalDateTime value) { this.ngayPhanHoi=value; }
    public HopDong getMaHopDongLienKet() { return maHopDongLienKet; }
    public MocNghiemThu getMaMocNghiemThuLienKet() { return maMocNghiemThuLienKet; }
    public NguoiDung getMaNguoiDeNghiLienKet() { return maNguoiDeNghiLienKet; }
    public NguoiDung getMaNguoiDongYLienKet() { return maNguoiDongYLienKet; }
}
