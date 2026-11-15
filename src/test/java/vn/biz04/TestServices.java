package vn.biz04;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.math.BigDecimal;

/** Publication fixtures exercise submission and a separate administrator's approval, on test DBs only. */
@Component @Transactional
class TestServices {
    private final Store db;private final CatalogService catalog;private final ServiceApproval approvals;
    TestServices(Store db,CatalogService catalog,ServiceApproval approvals){this.db=db;this.catalog=catalog;this.approvals=approvals;}
    DichVu create(Long actor,Long id,String name,String field,String description,BigDecimal price){
        NguoiDung admin=db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e","publication-fixture@test.local").orElseGet(()->{var a=TestAccounts.create(db,"Quản trị kiểm thử duyệt bài","publication-fixture@test.local","TestFixtureOnly!",false,false);a.setLaQuanTriVien(true);return a;});
        DichVu d=catalog.service(actor,id,name,field,description,price);approvals.submit(actor,d.getMaDichVu(),true);approvals.review(admin.getMaNguoiDung(),d.getMaDichVu(),d.getPhienBanNoiDung(),true,"");return d;
    }
}
