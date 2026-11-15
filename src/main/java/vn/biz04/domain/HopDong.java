package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="HopDong")
public class HopDong {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="MaHopDong", nullable=false)
    private Long maHopDong;

    @Column(name="MaYeuCau")
    private Long maYeuCau;

    @Column(name="MaDeXuat")
    private Long maDeXuat;

    @Column(name="NoiDungThoaThuan", nullable=false, columnDefinition="nvarchar(max)")
    @Nationalized
    private String noiDungThoaThuan;

    @Column(name="TongGiaTri", nullable=false, precision=18, scale=0)
    private BigDecimal tongGiaTri;

    @Column(name="PhienBan", nullable=false)
    private Integer phienBan;

    @Column(name="PhienBanKhachXacNhan", nullable=true)
    private Integer phienBanKhachXacNhan;

    @Column(name="PhienBanChuyenGiaXacNhan", nullable=true)
    private Integer phienBanChuyenGiaXacNhan;

    @Column(name="TrangThai", nullable=false, length=40)
    @Nationalized
    private String trangThai;

    @Column(name="NgayKetThuc", nullable=true)
    private LocalDateTime ngayKetThuc;

    @Column(name="TyLePhi", nullable=false, precision=18, scale=4)
    private BigDecimal tyLePhi;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaYeuCau", insertable=false, updatable=false)
    private YeuCau maYeuCauLienKet;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="MaDeXuat", insertable=false, updatable=false)
    private DeXuat maDeXuatLienKet;

    @Column(name="MaKeHoach") private Long maKeHoach;
    @Column(name="PhienBanKeHoach") private Integer phienBanKeHoach;
    @Column(name="MaKhachHang") private Long maKhachHang;
    @Column(name="MaChuyenGia") private Long maChuyenGia;
    @Column(name="QuyTacPhi",length=40) private String quyTacPhi;
    @Column(name="QuyTacBatDau",length=40) private String quyTacBatDau;
    @Column(name="DauKiemNoiDung",length=64) private String dauKiemNoiDung;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="NgayLap",precision=6) private OffsetDateTime ngayLap;
    @TimeZoneStorage(TimeZoneStorageType.NATIVE) @Column(name="ThoiDiemBatDau",precision=6) private OffsetDateTime thoiDiemBatDau;
    public Long getMaKeHoach(){return maKeHoach;} public void setMaKeHoach(Long v){maKeHoach=v;}
    public Integer getPhienBanKeHoach(){return phienBanKeHoach;} public void setPhienBanKeHoach(Integer v){phienBanKeHoach=v;}
    public Long getMaKhachHang(){return maKhachHang;} public void setMaKhachHang(Long v){maKhachHang=v;}
    public Long getMaChuyenGia(){return maChuyenGia;} public void setMaChuyenGia(Long v){maChuyenGia=v;}
    public String getQuyTacPhi(){return quyTacPhi;} public void setQuyTacPhi(String v){quyTacPhi=v;}
    public String getQuyTacBatDau(){return quyTacBatDau;} public void setQuyTacBatDau(String v){quyTacBatDau=v;}
    public String getDauKiemNoiDung(){return dauKiemNoiDung;} public void setDauKiemNoiDung(String v){dauKiemNoiDung=v;}
    public OffsetDateTime getNgayLap(){return ngayLap;} public void setNgayLap(OffsetDateTime v){ngayLap=v;}
    public OffsetDateTime getThoiDiemBatDau(){return thoiDiemBatDau;} public void setThoiDiemBatDau(OffsetDateTime v){thoiDiemBatDau=v;}

    public Long getMaHopDong() { return maHopDong; }
    public void setMaHopDong(Long value) { this.maHopDong=value; }
    public Long getMaYeuCau() { return maYeuCau; }
    public void setMaYeuCau(Long value) { this.maYeuCau=value; }
    public Long getMaDeXuat() { return maDeXuat; }
    public void setMaDeXuat(Long value) { this.maDeXuat=value; }
    public String getNoiDungThoaThuan() { return noiDungThoaThuan; }
    public void setNoiDungThoaThuan(String value) { this.noiDungThoaThuan=value; }
    public BigDecimal getTongGiaTri() { return tongGiaTri; }
    public void setTongGiaTri(BigDecimal value) { this.tongGiaTri=value; }
    public Integer getPhienBan() { return phienBan; }
    public void setPhienBan(Integer value) { this.phienBan=value; }
    public Integer getPhienBanKhachXacNhan() { return phienBanKhachXacNhan; }
    public void setPhienBanKhachXacNhan(Integer value) { this.phienBanKhachXacNhan=value; }
    public Integer getPhienBanChuyenGiaXacNhan() { return phienBanChuyenGiaXacNhan; }
    public void setPhienBanChuyenGiaXacNhan(Integer value) { this.phienBanChuyenGiaXacNhan=value; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String value) { this.trangThai=value; }
    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime value) { this.ngayKetThuc=value; }
    public BigDecimal getTyLePhi() { return tyLePhi; }
    public void setTyLePhi(BigDecimal value) { this.tyLePhi=value; }
    public YeuCau getMaYeuCauLienKet() { return maYeuCauLienKet; }
    public DeXuat getMaDeXuatLienKet() { return maDeXuatLienKet; }
}
