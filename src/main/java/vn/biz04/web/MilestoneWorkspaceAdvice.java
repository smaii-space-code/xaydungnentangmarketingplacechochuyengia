package vn.biz04.web;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.MilestonePlanService;
import java.security.Principal;
@ControllerAdvice(assignableTypes=Web.class)
public class MilestoneWorkspaceAdvice {
    private final Store db;private final TransactionTemplate tx;private final MilestonePlanService plans;
    public MilestoneWorkspaceAdvice(Store db,TransactionTemplate tx,MilestonePlanService plans){this.db=db;this.tx=tx;this.plans=plans;}
    @ModelAttribute("milestoneWorkspace") Object workspace(Principal principal,HttpServletRequest request){if(principal==null||!request.getRequestURI().equals("/bang-dieu-khien"))return null;Long actor=tx.execute(s->db.first("from NguoiDung u where u.email=:e and u.trangThai='HOAT_DONG'",NguoiDung.class,"e",principal.getName()).map(NguoiDung::getMaNguoiDung).orElse(null));return actor==null?null:plans.workspace(actor,1);}
}
