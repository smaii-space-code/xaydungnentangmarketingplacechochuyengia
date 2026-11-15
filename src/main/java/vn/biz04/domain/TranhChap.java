package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="TranhChap")
public class TranhChap {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaTranhChap", nullable=false)
    private Long maTranhChap;

    @Column(name="MaMocNghiemThu", nullable=false)
    private Long maMocNghiemThu;

    @Column(name="MaNguoiMo", nullable=false)
    private Long maNguoiMo;

    @Column(name="MaQuanTriVienXuLy", nullable=true)
    private Long maQuanTriVienXuLy;

    @Column(name="LyDo", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String lyDo;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @Column(name="KetLuan", nullable=true, columnDefinition="nvarchar(max)")
    @Nationalized
    private String ketLuan;

    @Column(name="TienGiaiNgan", nullable=true, precision=18, scale=0)
    private BigDecimal tienGiaiNgan;

    @Column(name="TienHoanTra", nullable=true, precision=18, scale=0)
    private BigDecimal tienHoanTra;

    @Column(name="HuongXuLyHopDong", nullable=true, length=40)
    @Nationalized
    private String huongXuLyHopDong;

    @Column(name="NgayKetLuan", nullable=true)
    private LocalDateTime ngayKetLuan;

    @Column(name="NgayDong", nullable=true)
    private LocalDateTime ngayDong;

    @Column(name="HanNopBangChung", nullable=true)
    private LocalDateTime hanNopBangChung;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaMocNghiemThu", insertable=false, updatable=false)
    private MocNghiemThu maMocNghiemThuLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiMo", insertable=false, updatable=false)
    private NguoiDung maNguoiMoLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaQuanTriVienXuLy", insertable=false, updatable=false)
    private NguoiDung maQuanTriVienXuLyLienKet;

    public Long getMaTranhChap() { return maTranhChap; }
    public void setMaTranhChap(Long value) { this.maTranhChap=value; }
    public Long getMaMocNghiemThu() { return maMocNghiemThu; }
    public void setMaMocNghiemThu(Long value) { this.maMocNghiemThu=value; }
    public Long getMaNguoiMo() { return maNguoiMo; }
    public void setMaNguoiMo(Long value) { this.maNguoiMo=value; }
    public Long getMaQuanTriVienXuLy() { return maQuanTriVienXuLy; }
    public void setMaQuanTriVienXuLy(Long value) { this.maQuanTriVienXuLy=value; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String value) { this.lyDo=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public String getKetLuan() { return ketLuan; }
    public void setKetLuan(String value) { this.ketLuan=value; }
    public BigDecimal getTienGiaiNgan() { return tienGiaiNgan; }
    public void setTienGiaiNgan(BigDecimal value) { this.tienGiaiNgan=value; }
    public BigDecimal getTienHoanTra() { return tienHoanTra; }
    public void setTienHoanTra(BigDecimal value) { this.tienHoanTra=value; }
    public String getHuongXuLyHopDong() { return huongXuLyHopDong; }
    public void setHuongXuLyHopDong(String value) { this.huongXuLyHopDong=value; }
    public LocalDateTime getNgayKetLuan() { return ngayKetLuan; }
    public void setNgayKetLuan(LocalDateTime value) { this.ngayKetLuan=value; }
    public LocalDateTime getNgayDong() { return ngayDong; }
    public void setNgayDong(LocalDateTime value) { this.ngayDong=value; }
    public LocalDateTime getHanNopBangChung() { return hanNopBangChung; }
    public void setHanNopBangChung(LocalDateTime value) { this.hanNopBangChung=value; }
    public MocNghiemThu getMaMocNghiemThuLienKet() { return maMocNghiemThuLienKet; }
    public NguoiDung getMaNguoiMoLienKet() { return maNguoiMoLienKet; }
    public NguoiDung getMaQuanTriVienXuLyLienKet() { return maQuanTriVienXuLyLienKet; }
}
