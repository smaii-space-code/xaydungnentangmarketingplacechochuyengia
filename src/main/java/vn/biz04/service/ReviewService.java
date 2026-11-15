package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.util.*;
@Service
@Transactional
public class ReviewService {
    private final Store db;private final Access access;
    public ReviewService(Store db,Access access){this.db=db;this.access=access;}
    public boolean eligible(HopDong h){return Set.of("HOAN_THANH","DA_CHAM_DUT").contains(h.getTrangThai())&&h.getNgayKetThuc()!=null&&db.count("select count(b) from BanGiao b, MocNghiemThu m where b.maMocNghiemThu=m.maMocNghiemThu and m.maHopDong=:h","h",h.getMaHopDong())>0;}
    public void submit(Long actor,Long id,int score,String content){HopDong h=db.lock(id);access.member(h,actor);Checks.require(eligible(h),"Hợp đồng chưa đủ điều kiện đánh giá.");Checks.require(access.now().isBefore(h.getNgayKetThuc().plusDays(14)),"Đã hết 14 ngày gửi đánh giá.");Checks.require(score>=1&&score<=5,"Điểm phải từ 1 đến 5.");Checks.require(db.count("select count(d) from DanhGia d where d.maHopDong=:h and d.maNguoiDanhGia=:u","h",id,"u",actor)==0,"Bạn đã gửi đánh giá cho hợp đồng này.");
        DanhGia d=new DanhGia();d.setMaHopDong(id);d.setMaNguoiDanhGia(actor);d.setMaNguoiDuocDanhGia(Objects.equals(actor,access.customer(h))?access.expert(h):access.customer(h));d.setDiem(score);d.setNoiDung(Checks.text(content,3,5000,"Nhận xét"));d.setTrangThai("DANG_AN");d.setNgayGui(access.now());db.save(d);db.flush();publishLocked(h);access.audit(actor,"DanhGia",d.getMaDanhGia(),"GUI_DANH_GIA",null);
    }
    private void publishLocked(HopDong h){List<DanhGia> ds=db.list("from DanhGia d where d.maHopDong=:h",DanhGia.class,"h",h.getMaHopDong());if(ds.size()>=2||(h.getNgayKetThuc()!=null&&!access.now().isBefore(h.getNgayKetThuc().plusDays(14))))for(DanhGia d:ds)if("DANG_AN".equals(d.getTrangThai())){d.setTrangThai("CONG_KHAI");d.setNgayCongKhai(access.now());access.audit(null,"DanhGia",d.getMaDanhGia(),"CONG_KHAI",null);}}
    public void publish(Long id){publishLocked(db.lock(id));}
    public List<DanhGia> publicReviews(Long contract){return db.list("from DanhGia d where d.maHopDong=:h and d.trangThai='CONG_KHAI' order by d.ngayGui",DanhGia.class,"h",contract);}
    public void hide(Long admin,Long id,String reason){access.admin(admin);DanhGia d=db.get(DanhGia.class,id);db.lock(d.getMaHopDong());d.setTrangThai("BI_AN_VI_PHAM");d.setLyDoAn(Checks.text(reason,5,2000,"Lý do ẩn"));access.audit(admin,"DanhGia",id,"AN_VI_PHAM",reason);}
}
