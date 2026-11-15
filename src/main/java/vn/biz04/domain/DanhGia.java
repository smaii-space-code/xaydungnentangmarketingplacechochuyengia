package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="DanhGia")
public class DanhGia {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaDanhGia", nullable=false)
    private Long maDanhGia;

    @Column(name="MaHopDong", nullable=false)
    private Long maHopDong;

    @Column(name="MaNguoiDanhGia", nullable=false)
    private Long maNguoiDanhGia;

    @Column(name="MaNguoiDuocDanhGia", nullable=false)
    private Long maNguoiDuocDanhGia;

    @Column(name="Diem", nullable=false)
    private Integer diem;

    @Column(name="NoiDung", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDung;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @Column(name="NgayGui", nullable=false)
    private LocalDateTime ngayGui;

    @Column(name="NgayCongKhai", nullable=true)
    private LocalDateTime ngayCongKhai;

    @Column(name="LyDoAn", nullable=true, columnDefinition="nvarchar(max)")
    @Nationalized
    private String lyDoAn;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaHopDong", insertable=false, updatable=false)
    private HopDong maHopDongLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiDanhGia", insertable=false, updatable=false)
    private NguoiDung maNguoiDanhGiaLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiDuocDanhGia", insertable=false, updatable=false)
    private NguoiDung maNguoiDuocDanhGiaLienKet;

    public Long getMaDanhGia() { return maDanhGia; }
    public void setMaDanhGia(Long value) { this.maDanhGia=value; }
    public Long getMaHopDong() { return maHopDong; }
    public void setMaHopDong(Long value) { this.maHopDong=value; }
    public Long getMaNguoiDanhGia() { return maNguoiDanhGia; }
    public void setMaNguoiDanhGia(Long value) { this.maNguoiDanhGia=value; }
    public Long getMaNguoiDuocDanhGia() { return maNguoiDuocDanhGia; }
    public void setMaNguoiDuocDanhGia(Long value) { this.maNguoiDuocDanhGia=value; }
    public Integer getDiem() { return diem; }
    public void setDiem(Integer value) { this.diem=value; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String value) { this.noiDung=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public LocalDateTime getNgayGui() { return ngayGui; }
    public void setNgayGui(LocalDateTime value) { this.ngayGui=value; }
    public LocalDateTime getNgayCongKhai() { return ngayCongKhai; }
    public void setNgayCongKhai(LocalDateTime value) { this.ngayCongKhai=value; }
    public String getLyDoAn() { return lyDoAn; }
    public void setLyDoAn(String value) { this.lyDoAn=value; }
    public HopDong getMaHopDongLienKet() { return maHopDongLienKet; }
    public NguoiDung getMaNguoiDanhGiaLienKet() { return maNguoiDanhGiaLienKet; }
    public NguoiDung getMaNguoiDuocDanhGiaLienKet() { return maNguoiDuocDanhGiaLienKet; }
}
