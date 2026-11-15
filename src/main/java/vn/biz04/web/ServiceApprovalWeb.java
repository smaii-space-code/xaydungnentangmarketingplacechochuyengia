package vn.biz04.web;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.service.*;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import java.security.Principal;

@Controller
public class ServiceApprovalWeb {
    private final ServiceApproval approvals;private final Store db;private final TransactionTemplate tx;
    public ServiceApprovalWeb(ServiceApproval approvals,Store db,TransactionTemplate tx){this.approvals=approvals;this.db=db;this.tx=tx;}
    private Long actor(Principal p){return tx.execute(s->db.first("from NguoiDung u where u.email=:e and u.trangThai='HOAT_DONG'",NguoiDung.class,"e",p.getName()).orElseThrow(()->new org.springframework.security.access.AccessDeniedException("Cần tài khoản hoạt động.")).getMaNguoiDung());}
    @PostMapping("/dich-vu/{id}/gui-duyet") String submit(Principal p,@PathVariable Long id,@RequestParam(defaultValue="false")boolean xacNhan,RedirectAttributes r){approvals.submit(actor(p),id,xacNhan);r.addFlashAttribute("success","Đã gửi dịch vụ cho admin duyệt. Nội dung chưa xuất hiện công khai.");return "redirect:/bang-dieu-khien";}
    @GetMapping("/quan-tri/dich-vu/{id}/duyet") String review(Principal p,@PathVariable Long id,Model m){m.addAttribute("service",approvals.forReview(actor(p),id));return "service-review";}
    @PostMapping("/quan-tri/dich-vu/{id}/duyet") String decide(Principal p,@PathVariable Long id,@RequestParam int phienBan,@RequestParam boolean chapThuan,@RequestParam(defaultValue="")String lyDo,RedirectAttributes r){approvals.review(actor(p),id,phienBan,chapThuan,lyDo);r.addFlashAttribute("success",chapThuan?"Đã duyệt đúng phiên bản dịch vụ.":"Đã từ chối nội dung dịch vụ.");return "redirect:/quan-tri";}
}
