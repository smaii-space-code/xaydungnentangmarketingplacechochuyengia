package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="BangChungTranhChap")
public class BangChungTranhChap {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaBangChung", nullable=false)
    private Long maBangChung;

    @Column(name="MaTranhChap", nullable=false)
    private Long maTranhChap;

    @Column(name="MaNguoiGui", nullable=false)
    private Long maNguoiGui;

    @Column(name="LoaiNoiDung", nullable=false, length=40)
    @Nationalized
    private String loaiNoiDung;

    @Column(name="NoiDung", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDung;

    @Column(name="DuongDanTep", nullable=true, columnDefinition="nvarchar(max)")
    @Nationalized
    private String duongDanTep;

    @Column(name="NgayGui", nullable=false)
    private LocalDateTime ngayGui;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaTranhChap", insertable=false, updatable=false)
    private TranhChap maTranhChapLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiGui", insertable=false, updatable=false)
    private NguoiDung maNguoiGuiLienKet;

    public Long getMaBangChung() { return maBangChung; }
    public void setMaBangChung(Long value) { this.maBangChung=value; }
    public Long getMaTranhChap() { return maTranhChap; }
    public void setMaTranhChap(Long value) { this.maTranhChap=value; }
    public Long getMaNguoiGui() { return maNguoiGui; }
    public void setMaNguoiGui(Long value) { this.maNguoiGui=value; }
    public String getLoaiNoiDung() { return loaiNoiDung; }
    public void setLoaiNoiDung(String value) { this.loaiNoiDung=value; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String value) { this.noiDung=value; }
    public String getDuongDanTep() { return duongDanTep; }
    public void setDuongDanTep(String value) { this.duongDanTep=value; }
    public LocalDateTime getNgayGui() { return ngayGui; }
    public void setNgayGui(LocalDateTime value) { this.ngayGui=value; }
    public TranhChap getMaTranhChapLienKet() { return maTranhChapLienKet; }
    public NguoiDung getMaNguoiGuiLienKet() { return maNguoiGuiLienKet; }
}
