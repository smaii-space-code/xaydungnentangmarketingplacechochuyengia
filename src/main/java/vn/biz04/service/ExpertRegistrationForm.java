package vn.biz04.service;

/** Only applicant-editable fields. Never contains role or review state. */
public class ExpertRegistrationForm {
    private String hoTen,email,password,passwordAgain,soDienThoai,linhVuc,gioiThieu;
    public String getHoTen(){return hoTen;} public void setHoTen(String v){hoTen=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getPasswordAgain(){return passwordAgain;} public void setPasswordAgain(String v){passwordAgain=v;}
    public String getSoDienThoai(){return soDienThoai;} public void setSoDienThoai(String v){soDienThoai=v;}
    public String getLinhVuc(){return linhVuc;} public void setLinhVuc(String v){linhVuc=v;}
    public String getGioiThieu(){return gioiThieu;} public void setGioiThieu(String v){gioiThieu=v;}
    public void clearSecrets(){password=null;passwordAgain=null;}
    public AccountForm account(){AccountForm f=new AccountForm();f.setHoTen(hoTen);f.setEmail(email);f.setPassword(password);f.setPasswordAgain(passwordAgain);f.setSoDienThoai(soDienThoai);f.setGioiThieu(gioiThieu);return f;}
}
