package vn.biz04.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import java.time.*;
import java.util.*;

@Service @Transactional
public class ServiceApproval {
    private final Store db;private final Access access;private final Clock clock;
    public ServiceApproval(Store db,Access access,Clock clock){this.db=db;this.access=access;this.clock=clock;}
    public void submit(Long actor,Long id,boolean confirmed){
        NguoiDung u=access.role(actor,false);DichVu d=db.lockEntity(DichVu.class,id);
        if(!actor.equals(d.getMaChuyenGia()))throw new AccessDeniedException("Không sở hữu dịch vụ.");
        Checks.require(PublicCatalogPolicy.official(u),"Cần hoàn tất tiếp quản và xác thực email trước khi gửi duyệt.");
        Checks.require(!d.isCanRaSoatLai(),"Dịch vụ cũ cần được chủ mới chỉnh sửa và lưu lại trước khi gửi duyệt.");Checks.require(confirmed,"Cần xác nhận đã rà soát và chịu trách nhiệm nội dung.");
        Checks.require(!"CHO_DUYET".equals(d.getTrangThaiDuyet())&&!PublicCatalogPolicy.approved(d),"Dịch vụ đã gửi hoặc đã được duyệt ở phiên bản này.");
        d.setXacNhanNoiDung(true);d.setTrangThaiDuyet("CHO_DUYET");d.setTrangThai("NHAP");access.audit(actor,"DichVu",id,"GUI_DUYET_DICH_VU","Phiên bản "+d.getPhienBanNoiDung());
    }
    public void review(Long actor,Long id,int version,boolean accepted,String reason){
        DichVu initial=db.get(DichVu.class,id);for(Long user:new TreeSet<>(List.of(actor,initial.getMaChuyenGia())))db.lockEntity(NguoiDung.class,user);access.admin(actor);DichVu d=db.lockEntity(DichVu.class,id);
        Checks.require(!actor.equals(d.getMaChuyenGia()),"Không tự duyệt dịch vụ của chính mình.");
        Checks.require(d.getPhienBanNoiDung()==version&&"CHO_DUYET".equals(d.getTrangThaiDuyet())&&d.isXacNhanNoiDung()&&!d.isCanRaSoatLai(),"Phiên bản hoặc trạng thái đã thay đổi. Hãy tải lại trước khi duyệt.");
        if(accepted){NguoiDung owner=access.user(d.getMaChuyenGia());Checks.require(owner.getLaChuyenGia()&&PublicCatalogPolicy.official(owner),"Người cung cấp chưa đủ điều kiện tài khoản chính thức/xác thực email.");}
        String explanation=accepted?"Đã duyệt nội dung phiên bản "+version:Checks.text(reason,5,2000,"Lý do từ chối");
        d.setTrangThaiDuyet(accepted?"DA_DUYET":"TU_CHOI");d.setPhienBanDuyet(accepted?version:null);d.setMaNguoiDuyet(actor);d.setNgayDuyet(OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));d.setLyDoDuyet(explanation);d.setTrangThai(accepted?"DANG_HIEN":"NHAP");access.audit(actor,"DichVu",id,accepted?"DUYET_DICH_VU":"TU_CHOI_DICH_VU","Phiên bản "+version+": "+explanation);
    }
    @Transactional(readOnly=true) public DichVu forReview(Long actor,Long id){access.admin(actor);return db.get(DichVu.class,id);}
}
