package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="NhatKyXuLy")
public class NhatKyXuLy {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaNhatKy", nullable=false)
    private Long maNhatKy;

    @Column(name="MaNguoiThucHien", nullable=true)
    private Long maNguoiThucHien;

    @Column(name="LoaiDoiTuong", nullable=false, length=80)
    @Nationalized
    private String loaiDoiTuong;

    @Column(name="MaDoiTuong", nullable=false)
    private Long maDoiTuong;

    @Column(name="HanhDong", nullable=false, length=100)
    @Nationalized
    private String hanhDong;

    @Column(name="NoiDungLyDo", nullable=true, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDungLyDo;

    @Column(name="ThoiDiem", nullable=false)
    private LocalDateTime thoiDiem;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaNguoiThucHien", insertable=false, updatable=false)
    private NguoiDung maNguoiThucHienLienKet;

    public Long getMaNhatKy() { return maNhatKy; }
    public void setMaNhatKy(Long value) { this.maNhatKy=value; }
    public Long getMaNguoiThucHien() { return maNguoiThucHien; }
    public void setMaNguoiThucHien(Long value) { this.maNguoiThucHien=value; }
    public String getLoaiDoiTuong() { return loaiDoiTuong; }
    public void setLoaiDoiTuong(String value) { this.loaiDoiTuong=value; }
    public Long getMaDoiTuong() { return maDoiTuong; }
    public void setMaDoiTuong(Long value) { this.maDoiTuong=value; }
    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String value) { this.hanhDong=value; }
    public String getNoiDungLyDo() { return noiDungLyDo; }
    public void setNoiDungLyDo(String value) { this.noiDungLyDo=value; }
    public LocalDateTime getThoiDiem() { return thoiDiem; }
    public void setThoiDiem(LocalDateTime value) { this.thoiDiem=value; }
    public NguoiDung getMaNguoiThucHienLienKet() { return maNguoiThucHienLienKet; }
}
