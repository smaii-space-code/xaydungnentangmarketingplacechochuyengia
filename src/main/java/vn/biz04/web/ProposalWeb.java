package vn.biz04.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.*;
import java.security.Principal;
import java.util.*;

@Controller
public class ProposalWeb {
    private final Store db;private final Access access;private final TransactionTemplate tx;private final ProposalService proposals;private final ContractService contracts;
    public ProposalWeb(Store db,Access access,TransactionTemplate tx,ProposalService proposals,ContractService contracts){this.db=db;this.access=access;this.tx=tx;this.proposals=proposals;this.contracts=contracts;}
    private Long actor(Principal p){return tx.execute(s->access.user(db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",p.getName()).orElseThrow(()->new BusinessException("Tài khoản không tồn tại.")).getMaNguoiDung()).getMaNguoiDung());}
    @GetMapping("/de-xuat/nhan") String received(Principal p,Model m){m.addAttribute("rows",proposals.received(actor(p)));m.addAttribute("received",true);return "proposal-list";}
    @GetMapping("/de-xuat/da-gui") String sent(Principal p,Model m){m.addAttribute("rows",proposals.sent(actor(p)));m.addAttribute("received",false);return "proposal-list";}
    @GetMapping("/de-xuat/{id}") String details(Principal p,@PathVariable Long id,Model m){var row=proposals.details(actor(p),id);m.addAttribute("row",row);m.addAttribute("rows",List.of(row));return "proposal-detail";}
    @GetMapping("/de-xuat/{id}/xac-nhan") String confirm(Principal p,@PathVariable Long id,@RequestParam String hanhDong,Model m){
        Checks.require(Set.of("chap-thuan","tu-choi").contains(hanhDong),"Thao tác xác nhận không hợp lệ.");m.addAttribute("row",proposals.ownerDetails(actor(p),id));m.addAttribute("accept",hanhDong.equals("chap-thuan"));return "proposal-confirm";
    }
    @PostMapping({"/de-xuat/{id}/chap-thuan","/de-xuat/{id}/chon"}) String accept(Principal p,@PathVariable Long id,RedirectAttributes r){
        try{var result=proposals.accept(actor(p),id);r.addFlashAttribute("success",result.contractId()!=null?"Yêu cầu đã có hợp đồng. Đã mở hợp đồng hiện có.":"Đã chấp thuận chuyên gia. Chờ chuyên gia thiết lập mốc.");return result.contractId()!=null?"redirect:/hop-dong/"+result.contractId():"redirect:/ke-hoach-moc/de-xuat/"+id;}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return "redirect:/de-xuat/"+id;}
    }
    @PostMapping("/de-xuat/{id}/tu-choi") String reject(Principal p,@PathVariable Long id,RedirectAttributes r){
        try{proposals.reject(actor(p),id);r.addFlashAttribute("success","Đã từ chối đề xuất.");}catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}return "redirect:/de-xuat/"+id;
    }
    @GetMapping("/de-xuat/{id}/lap-hop-dong") String review(Principal p,@PathVariable Long id,Model m){
        var row=proposals.ownerDetails(actor(p),id);if(row.contractId()!=null)return "redirect:/hop-dong/"+row.contractId();return "redirect:/ke-hoach-moc/de-xuat/"+id;
    }
    @PostMapping("/de-xuat/{id}/lap-hop-dong") String create(Principal p,@PathVariable Long id,RedirectAttributes r){
        try{var h=contracts.select(actor(p),id);r.addFlashAttribute("success","Đã mở hợp đồng liên kết với đề xuất. Kiểm tra trạng thái xác nhận của hai bên trong hợp đồng.");return "redirect:/hop-dong/"+h.getMaHopDong();}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return "redirect:/de-xuat/"+id+"/lap-hop-dong";}
    }
}
