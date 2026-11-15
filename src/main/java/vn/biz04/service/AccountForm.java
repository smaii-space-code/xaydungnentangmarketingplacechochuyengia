package vn.biz04.service;

/** Chỉ chứa trường được phép nhập. Không bind entity, trạng thái hay quyền ADMIN. */
public class AccountForm {
    private String hoTen="",email="",password="",passwordAgain="",soDienThoai="",loaiKhachHang="CA_NHAN",tenToChuc="",diaChi="",gioiThieu="",soCccdMoi="";
    private boolean khach,chuyenGia;
    public String getHoTen(){return hoTen;} public void setHoTen(String v){hoTen=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getPasswordAgain(){return passwordAgain;} public void setPasswordAgain(String v){passwordAgain=v;}
    public String getSoDienThoai(){return soDienThoai;} public void setSoDienThoai(String v){soDienThoai=v;}
    public String getLoaiKhachHang(){return loaiKhachHang;} public void setLoaiKhachHang(String v){loaiKhachHang=v;}
    public String getTenToChuc(){return tenToChuc;} public void setTenToChuc(String v){tenToChuc=v;}
    public String getDiaChi(){return diaChi;} public void setDiaChi(String v){diaChi=v;}
    public String getGioiThieu(){return gioiThieu;} public void setGioiThieu(String v){gioiThieu=v;}
    public String getSoCccdMoi(){return soCccdMoi;} public void setSoCccdMoi(String v){soCccdMoi=v;}
    public boolean isKhach(){return khach;} public void setKhach(boolean v){khach=v;}
    public boolean isChuyenGia(){return chuyenGia;} public void setChuyenGia(boolean v){chuyenGia=v;}
    public void clearSecrets(){password="";passwordAgain="";soCccdMoi="";}
}
