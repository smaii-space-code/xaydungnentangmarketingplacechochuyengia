package vn.biz04.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;

@Entity @Table(name="HoSoKhachHang")
public class HoSoKhachHang {
    @Id @Column(name="MaNguoiDung") public Long maNguoiDung;
    @Column(name="SoDienThoai",length=10) public String soDienThoai;
    @Column(name="LoaiKhachHang",length=20,nullable=false) public String loaiKhachHang="CA_NHAN";
    @Nationalized @Column(name="TenToChuc",length=200) public String tenToChuc;
    @Nationalized @Column(name="DiaChi",length=250) public String diaChi;
    @Nationalized @Column(name="GioiThieu",length=2000) public String gioiThieu;
    @Column(name="SoCccdMaHoa",length=255) public String soCccdMaHoa;
    @Column(name="CccdBonSoCuoi",length=4) public String cccdBonSoCuoi;
}
