package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class CatalogService {
    private final Store db;private final Access access;
    public CatalogService(Store db,Access access){this.db=db;this.access=access;}
    public DichVu service(Long actor,Long id,String name,String field,String description,BigDecimal price) {
        NguoiDung u=access.role(actor,false);
        DichVu d=id==null?new DichVu():db.lockEntity(DichVu.class,id);Checks.require(id==null||Objects.equals(d.getMaChuyenGia(),actor),"Bạn không sở hữu dịch vụ này.");
        name=Checks.text(name,3,255,"Tên dịch vụ");field=Checks.text(field,2,100,"Lĩnh vực");description=Checks.text(description,10,10000,"Mô tả");price=Checks.money(price,false);
        if(d.isCanRaSoatLai()){Checks.require(PublicCatalogPolicy.official(u),"Cần hoàn tất tiếp quản trước khi sửa bài mẫu.");Checks.require(!Objects.equals(name,d.getTenDichVu())||!Objects.equals(field,d.getLinhVuc())||!Objects.equals(description,d.getMoTa())||price.compareTo(d.getGiaThamKhao())!=0,"Hãy chỉnh sửa nội dung mẫu thành dịch vụ bạn chịu trách nhiệm trước khi gửi duyệt.");}
        if(id!=null)d.setPhienBanNoiDung(d.getPhienBanNoiDung()+1);d.setTrangThai("NHAP");d.setTrangThaiDuyet("CHUA_GUI");d.setPhienBanDuyet(null);d.setXacNhanNoiDung(false);d.setCanRaSoatLai(false);d.setMaNguoiDuyet(null);d.setNgayDuyet(null);d.setLyDoDuyet(null);
        d.setMaChuyenGia(actor);d.setTenDichVu(Checks.text(name,3,255,"Tên dịch vụ"));d.setLinhVuc(Checks.text(field,2,100,"Lĩnh vực"));d.setMoTa(Checks.text(description,10,10000,"Mô tả"));d.setGiaThamKhao(Checks.money(price,false));if(id==null){db.save(d);}access.audit(actor,"DichVu",d.getMaDichVu(),"LUU_DICH_VU",null);return d;
    }
    public void visibility(Long actor,Long id,boolean visible){NguoiDung u=access.user(actor);if(!u.getLaQuanTriVien())u=access.role(actor,false);DichVu d=db.get(DichVu.class,id);Checks.require(u.getLaQuanTriVien()||Objects.equals(d.getMaChuyenGia(),actor),"Bạn không có quyền sửa dịch vụ.");if(visible)Checks.require(PublicCatalogPolicy.approved(d)&&PublicCatalogPolicy.official(db.get(NguoiDung.class,d.getMaChuyenGia())),"Dịch vụ cần được admin duyệt và chủ sở hữu đủ điều kiện trước khi công khai.");d.setTrangThai(visible?"DANG_HIEN":"DA_AN");access.audit(actor,"DichVu",id,"DOI_HIEN_THI",d.getTrangThai());}
    public YeuCau request(Long actor,Long serviceId,String title,String description,BigDecimal budget,LocalDate due) {
        access.role(actor,true);
        if(serviceId!=null){DichVu d=db.get(DichVu.class,serviceId);Checks.require("DANG_HIEN".equals(d.getTrangThai())&&!Objects.equals(d.getMaChuyenGia(),actor),"Dịch vụ chưa khả dụng hoặc là dịch vụ của bạn.");}
        YeuCau y=new YeuCau();y.setMaKhachHang(actor);y.setMaDichVu(serviceId);y.setTieuDe(Checks.text(title,5,255,"Tiêu đề"));y.setMoTa(Checks.text(description,10,10000,"Nội dung"));y.setNganSach(Checks.money(budget,false));y.setHanDuKien(due==null?null:due.atTime(23,59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());y.setTrangThai("DANG_MO");db.save(y);access.audit(actor,"YeuCau",y.getMaYeuCau(),"DANG_YEU_CAU",null);return y;
    }
    public void closeRequest(Long actor,Long id){access.user(actor);YeuCau y=db.lockEntity(YeuCau.class,id);Checks.require(Objects.equals(y.getMaKhachHang(),actor),"Bạn không sở hữu yêu cầu.");Checks.require("DANG_MO".equals(y.getTrangThai()),"Chỉ đóng yêu cầu còn mở và chưa chấp thuận chuyên gia.");Checks.require(db.count("select count(h) from HopDong h where h.maYeuCau=:y","y",id)==0,"Yêu cầu đã có hợp đồng.");y.setTrangThai("DA_DONG");access.audit(actor,"YeuCau",id,"DONG_YEU_CAU",null);}
    public void editRequest(Long actor,Long id,String title,String description,BigDecimal budget,LocalDate due){access.user(actor);YeuCau y=db.lockEntity(YeuCau.class,id);Checks.require(Objects.equals(y.getMaKhachHang(),actor)&&"DANG_MO".equals(y.getTrangThai()),"Chỉ sửa yêu cầu đang mở của bạn.");Checks.require(db.count("select count(h) from HopDong h where h.maYeuCau=:y","y",id)==0,"Yêu cầu đã có hợp đồng.");y.setTieuDe(Checks.text(title,5,255,"Tiêu đề"));y.setMoTa(Checks.text(description,10,10000,"Nội dung"));y.setNganSach(Checks.money(budget,false));y.setHanDuKien(due==null?null:due.atTime(23,59).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());access.audit(actor,"YeuCau",id,"SUA_YEU_CAU",null);}
    public DeXuat proposal(Long actor,Long requestId,String plan,BigDecimal price,int days){
        access.role(actor,false);YeuCau y=db.lockEntity(YeuCau.class,requestId);
        Checks.require("DANG_MO".equals(y.getTrangThai())&&!Objects.equals(y.getMaKhachHang(),actor),"Yêu cầu không nhận đề xuất hoặc là yêu cầu của bạn.");
        NguoiDung customer=access.user(y.getMaKhachHang());Checks.require(customer.getLaKhachHang(),"Khách hàng không còn đủ điều kiện nhận đề xuất.");
        if(y.getMaDichVu()!=null)Checks.require(Objects.equals(db.get(DichVu.class,y.getMaDichVu()).getMaChuyenGia(),actor),"Yêu cầu từ dịch vụ có sẵn chỉ gửi đến chuyên gia đăng dịch vụ đó.");
        Checks.require(days>0&&days<=730,"Thời gian dự kiến từ 1 đến 730 ngày.");
        DeXuat d=db.first("from DeXuat d where d.maYeuCau=:y and d.maChuyenGia=:u",DeXuat.class,"y",requestId,"u",actor).orElse(null);
        boolean fresh=d==null;if(fresh){d=new DeXuat();d.setMaYeuCau(requestId);d.setMaChuyenGia(actor);}else Checks.require(!"DA_CHON".equals(d.getTrangThai()),"Đề xuất đã được chọn.");
        d.setPhuongAn(Checks.text(plan,10,10000,"Phương án"));d.setGiaDeXuat(Checks.money(price,false));d.setSoNgayThucHien(days);d.setTrangThai("DANG_GUI");if(fresh)db.save(d);access.audit(actor,"DeXuat",d.getMaDeXuat(),"GUI_DE_XUAT",null);return d;
    }
    public void withdraw(Long actor,Long id){DeXuat d=db.get(DeXuat.class,id);db.lockEntity(YeuCau.class,d.getMaYeuCau());db.refresh(d);Checks.require(Objects.equals(d.getMaChuyenGia(),actor)&&"DANG_GUI".equals(d.getTrangThai()),"Không thể rút đề xuất này.");d.setTrangThai("DA_RUT");access.audit(actor,"DeXuat",id,"RUT_DE_XUAT",null);}
}
