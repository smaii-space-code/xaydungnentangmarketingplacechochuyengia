package vn.biz04.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service @Transactional
public class ProposalService {
    private final Store db;private final Access access;
    public ProposalService(Store db,Access access){this.db=db;this.access=access;}
    public record Locked(DeXuat proposal,YeuCau request){}
    public record Decision(Long proposalId,Long contractId,boolean changed){}
    public record Row(Long id,Long requestId,String requestTitle,Long customerId,Long expertId,String expertName,String customerName,String plan,BigDecimal price,Integer days,LocalDateTime submitted,String rawStatus,String label,String tone,Long contractId,String contractStatus,boolean canDecide,boolean canContinue,boolean canWithdraw){}
    private static final String SELECT="select d,y,e.hoTen,c.hoTen,h.maHopDong,h.trangThai,(select max(n.thoiDiem) from NhatKyXuLy n where n.loaiDoiTuong='DeXuat' and n.maDoiTuong=d.maDeXuat and n.hanhDong='GUI_DE_XUAT') from DeXuat d join d.maYeuCauLienKet y join d.maChuyenGiaLienKet e join y.maKhachHangLienKet c left join HopDong h on h.maDeXuat=d.maDeXuat ";
    private List<Row> rows(Long actor,String where,Object... params){NguoiDung viewer=access.user(actor);return db.list(SELECT+where+" order by d.maDeXuat desc",Object[].class,params).stream().map(r->row(viewer,r)).toList();}
    private Row row(NguoiDung viewer,Object[] data){
        DeXuat d=(DeXuat)data[0];YeuCau y=(YeuCau)data[1];Long contract=(Long)data[4];String state=d.getTrangThai(),label,tone="neutral";
        if(contract!=null){label="Đã lập hợp đồng";tone="accepted";}
        else if("DANG_GUI".equals(state)){label="Đã đề xuất";tone="sent";}
        else if("BI_TU_CHOI".equals(state)){label="Đề xuất bị từ chối";tone="rejected";}
        else if("DA_CHON".equals(state)){label="Được chấp thuận — Chờ xác lập mốc";tone="accepted";}
        else label="DA_RUT".equals(state)?"Đã rút đề xuất":state;
        boolean owner=Objects.equals(viewer.getMaNguoiDung(),y.getMaKhachHang())&&viewer.getLaKhachHang();
        boolean decide=owner&&contract==null&&"DANG_MO".equals(y.getTrangThai())&&"DANG_GUI".equals(state);
        boolean resume=owner&&contract==null&&"DA_CHON".equals(state)&&Set.of("DANG_MO","DANG_THUONG_LUONG").contains(y.getTrangThai());
        boolean withdraw=viewer.getLaChuyenGia()&&Objects.equals(viewer.getMaNguoiDung(),d.getMaChuyenGia())&&"DANG_GUI".equals(state)&&contract==null;
        return new Row(d.getMaDeXuat(),y.getMaYeuCau(),y.getTieuDe(),y.getMaKhachHang(),d.getMaChuyenGia(),(String)data[2],(String)data[3],d.getPhuongAn(),d.getGiaDeXuat(),d.getSoNgayThucHien(),(LocalDateTime)data[6],state,label,tone,contract,(String)data[5],decide,resume,withdraw);
    }
    @Transactional(readOnly=true) public List<Row> received(Long actor){return rows(actor,"where y.maKhachHang=:u","u",actor);}
    @Transactional(readOnly=true) public List<Row> sent(Long actor){return rows(actor,"where d.maChuyenGia=:u","u",actor);}
    @Transactional(readOnly=true) public List<Row> forRequest(Long actor,Long request){
        NguoiDung u=access.user(actor);YeuCau y=db.get(YeuCau.class,request);boolean all=u.getLaQuanTriVien()||Objects.equals(y.getMaKhachHang(),actor);
        return all?rows(actor,"where y.maYeuCau=:y","y",request):rows(actor,"where y.maYeuCau=:y and d.maChuyenGia=:u","y",request,"u",actor);
    }
    @Transactional(readOnly=true) public Row details(Long actor,Long id){
        NguoiDung u=access.user(actor);DeXuat d=db.get(DeXuat.class,id);YeuCau y=db.get(YeuCau.class,d.getMaYeuCau());
        if(!u.getLaQuanTriVien()&&!Objects.equals(actor,y.getMaKhachHang())&&!Objects.equals(actor,d.getMaChuyenGia()))throw new AccessDeniedException("Bạn không được xem đề xuất này.");
        return rows(actor,"where d.maDeXuat=:d","d",id).get(0);
    }
    @Transactional(readOnly=true) public Row ownerDetails(Long actor,Long id){Row row=details(actor,id);if(!Objects.equals(actor,row.customerId()))throw new AccessDeniedException("Chỉ chủ yêu cầu được xử lý đề xuất.");return row;}
    @Transactional(readOnly=true) public Map<Long,Long> counts(Long actor,List<YeuCau> requests){
        NguoiDung u=access.user(actor);Map<Long,Long> counts=new HashMap<>();for(YeuCau y:requests){if(!u.getLaQuanTriVien()&&!Objects.equals(actor,y.getMaKhachHang()))throw new AccessDeniedException("Không được xem số đề xuất của người khác.");counts.put(y.getMaYeuCau(),0L);}
        if(!counts.isEmpty())for(Object[] row:db.list("select d.maYeuCau,count(d) from DeXuat d where d.maYeuCau in :ids group by d.maYeuCau",Object[].class,"ids",counts.keySet()))counts.put((Long)row[0],(Long)row[1]);return counts;
    }
    /** Thứ tự khóa dùng chung cho quyết định và tạo hợp đồng: người dùng tăng dần, rồi yêu cầu. */
    public Locked lockOwner(Long actor,Long id){
        DeXuat d=db.get(DeXuat.class,id);for(Long uid:new TreeSet<>(List.of(actor,d.getMaChuyenGia())))db.lockEntity(NguoiDung.class,uid);
        access.user(actor);YeuCau y=db.lockEntity(YeuCau.class,d.getMaYeuCau());db.refresh(d);
        Checks.require(Objects.equals(d.getMaYeuCau(),y.getMaYeuCau()),"Đề xuất đã thay đổi. Hãy tải lại trang.");
        if(!Objects.equals(actor,y.getMaKhachHang()))throw new AccessDeniedException("Chỉ chủ yêu cầu được xử lý đề xuất.");return new Locked(d,y);
    }
    public Decision accept(Long actor,Long id){
        Locked locked=lockOwner(actor,id);DeXuat d=locked.proposal();YeuCau y=locked.request();
        HopDong existing=db.first("from HopDong h where h.maYeuCau=:y",HopDong.class,"y",y.getMaYeuCau()).orElse(null);
        if(existing!=null){Checks.require(Objects.equals(existing.getMaDeXuat(),id),"Yêu cầu đã có hợp đồng với đề xuất khác.");return new Decision(id,existing.getMaHopDong(),false);}
        access.role(actor,true);
        if("DA_CHON".equals(d.getTrangThai()))return new Decision(id,null,false);
        Checks.require("DANG_MO".equals(y.getTrangThai())&&"DANG_GUI".equals(d.getTrangThai()),"Đề xuất không còn chờ xử lý. Hãy tải lại để xem trạng thái mới.");
        Checks.require(!Objects.equals(actor,d.getMaChuyenGia()),"Không chấp thuận đề xuất của chính mình.");access.role(d.getMaChuyenGia(),false);
        Checks.require(db.count("select count(d) from DeXuat d where d.maYeuCau=:y and d.trangThai='DA_CHON'","y",y.getMaYeuCau())==0,"Yêu cầu đã chấp thuận một chuyên gia khác.");
        d.setTrangThai("DA_CHON");y.setTrangThai("DANG_THUONG_LUONG");access.audit(actor,"DeXuat",id,"CHAP_THUAN_DE_XUAT",null);return new Decision(id,null,true);
    }
    public boolean reject(Long actor,Long id){
        Locked locked=lockOwner(actor,id);DeXuat d=locked.proposal();YeuCau y=locked.request();access.role(actor,true);
        Checks.require(db.count("select count(h) from HopDong h where h.maDeXuat=:d","d",id)==0,"Đề xuất đã có hợp đồng, không thể từ chối tại đây.");
        if("BI_TU_CHOI".equals(d.getTrangThai()))return false;
        Checks.require("DANG_MO".equals(y.getTrangThai())&&"DANG_GUI".equals(d.getTrangThai()),"Đề xuất không còn chờ xử lý. Hãy tải lại để xem trạng thái mới.");
        d.setTrangThai("BI_TU_CHOI");access.audit(actor,"DeXuat",id,"TU_CHOI_DE_XUAT",null);return true;
    }
}
