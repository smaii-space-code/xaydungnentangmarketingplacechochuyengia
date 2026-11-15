package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="GiaoDich")
public class GiaoDich {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaGiaoDich", nullable=false)
    private Long maGiaoDich;

    @Column(name="MaMocNghiemThu", nullable=false)
    private Long maMocNghiemThu;

    @Column(name="MaTranhChap", nullable=true)
    private Long maTranhChap;

    @Column(name="MaNguoiYeuCau", nullable=true)
    private Long maNguoiYeuCau;

    @Column(name="LoaiGiaoDich", nullable=false, length=40)
    @Nationalized
    private String loaiGiaoDich;

    @Column(name="SoTien", nullable=false, precision=18, scale=0)
    private BigDecimal soTien;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @Column(name="KhoaChongLap", nullable=false, length=180)
    @Nationalized
    private String khoaChongLap;

    @Column(name="MaThamChieuCong", nullable=true, length=120)
    @Nationalized
    private String maThamChieuCong;

    @Column(name="NgayHoanTat", nullable=true)
    private LocalDateTime ngayHoanTat;

    @Column(name="PhiNenTang", nullable=false, precision=18, scale=0)
    private BigDecimal phiNenTang;

    @Column(name="ThucNhan", nullable=false, precision=18, scale=0)
    private BigDecimal thucNhan;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaMocNghiemThu", insertable=false, updatable=false)
    private MocNghiemThu maMocNghiemThuLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaTranhChap", insertable=false, updatable=false)
    private TranhChap maTranhChapLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiYeuCau", insertable=false, updatable=false)
    private NguoiDung maNguoiYeuCauLienKet;

    public Long getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(Long value) { this.maGiaoDich=value; }
    public Long getMaMocNghiemThu() { return maMocNghiemThu; }
    public void setMaMocNghiemThu(Long value) { this.maMocNghiemThu=value; }
    public Long getMaTranhChap() { return maTranhChap; }
    public void setMaTranhChap(Long value) { this.maTranhChap=value; }
    public Long getMaNguoiYeuCau() { return maNguoiYeuCau; }
    public void setMaNguoiYeuCau(Long value) { this.maNguoiYeuCau=value; }
    public String getLoaiGiaoDich() { return loaiGiaoDich; }
    public void setLoaiGiaoDich(String value) { this.loaiGiaoDich=value; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal value) { this.soTien=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public String getKhoaChongLap() { return khoaChongLap; }
    public void setKhoaChongLap(String value) { this.khoaChongLap=value; }
    public String getMaThamChieuCong() { return maThamChieuCong; }
    public void setMaThamChieuCong(String value) { this.maThamChieuCong=value; }
    public LocalDateTime getNgayHoanTat() { return ngayHoanTat; }
    public void setNgayHoanTat(LocalDateTime value) { this.ngayHoanTat=value; }
    public BigDecimal getPhiNenTang() { return phiNenTang; }
    public void setPhiNenTang(BigDecimal value) { this.phiNenTang=value; }
    public BigDecimal getThucNhan() { return thucNhan; }
    public void setThucNhan(BigDecimal value) { this.thucNhan=value; }
    public MocNghiemThu getMaMocNghiemThuLienKet() { return maMocNghiemThuLienKet; }
    public TranhChap getMaTranhChapLienKet() { return maTranhChapLienKet; }
    public NguoiDung getMaNguoiYeuCauLienKet() { return maNguoiYeuCauLienKet; }
}
