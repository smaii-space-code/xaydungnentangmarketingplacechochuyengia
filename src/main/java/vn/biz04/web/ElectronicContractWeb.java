package vn.biz04.web;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class ElectronicContractWeb {
    private final ContractService contracts;private final ContractSnapshots snapshots;private final Store db;private final TransactionTemplate tx;private final Clock clock;
    public ElectronicContractWeb(ContractService contracts,ContractSnapshots snapshots,Store db,TransactionTemplate tx,Clock clock){this.contracts=contracts;this.snapshots=snapshots;this.db=db;this.tx=tx;this.clock=clock;}
    private static final Set<String> FIELDS=Set.of("customerDuties","expertDuties","additional","representativeName","representativeTitle","reviewed",
            "usageRights","confidentiality","liability");
    private record Intent(Long actor,String kind,Long source,Long contract,int version,String digest,Instant expires){}
    private record Ticket(Intent intent,String action,ContractSnapshots.Body body,String reason){}
    public static class CancelForm {private String reason="";public String getReason(){return reason;}public void setReason(String v){reason=v;}}
    @InitBinder("contractForm") void fields(WebDataBinder binder){binder.setAllowedFields(FIELDS.toArray(String[]::new));if(binder instanceof ExtendedServletRequestDataBinder b)b.addHeaderPredicate(header->false);}
    @InitBinder("cancelForm") void cancelFields(WebDataBinder binder){binder.setAllowedFields("reason");if(binder instanceof ExtendedServletRequestDataBinder b)b.addHeaderPredicate(header->false);}
    private Long actor(Principal p){if(p==null)throw new AccessDeniedException("Cần đăng nhập.");return tx.execute(s->db.first("from NguoiDung u where u.email=:e and u.trangThai='HOAT_DONG'",NguoiDung.class,"e",p.getName()).orElseThrow(()->new AccessDeniedException("Tài khoản không hoạt động.")).getMaNguoiDung());}
    private Intent intent(Long actor,String kind,Long source,Long contract,int version,String digest){return new Intent(actor,kind,source,contract,version,digest,clock.instant().plus(Duration.ofMinutes(30)));}
    @SuppressWarnings("unchecked") private String remember(HttpSession session,String group,Object value){synchronized(session){var map=(Map<String,Object>)session.getAttribute(group);if(map==null){map=new LinkedHashMap<>();session.setAttribute(group,map);}String token=UUID.randomUUID().toString();map.put(token,value);while(map.size()>32)map.remove(map.keySet().iterator().next());return token;}}
    private <T> T stored(HttpSession session,String group,String token,Class<T> type){synchronized(session){var raw=session.getAttribute(group);var value=raw instanceof Map<?,?> map?map.get(token):null;Checks.require(type.isInstance(value),"Phiên rà soát đã hết hạn. Hãy mở lại kế hoạch hoặc hợp đồng để rà soát; bản đã lưu không bị mất.");return type.cast(value);}}
    private void authority(Intent intent,Long actor,String kind,Long source,Long contract){if(!intent.actor().equals(actor)||!Objects.equals(kind,intent.kind())||!Objects.equals(source,intent.source())||!Objects.equals(contract,intent.contract()))throw new AccessDeniedException("Ticket không thuộc tài khoản hoặc giao dịch này.");Checks.require(clock.instant().isBefore(intent.expires()),"Phiên rà soát đã hết hạn. Hãy mở lại bản mới để rà soát.");}
    private void bound(BindingResult binding,HttpServletRequest request,Set<String> fields){Checks.require(!binding.hasErrors()&&Arrays.stream(binding.getSuppressedFields()).allMatch(Set.of("token","_csrf","kind","sourceId","id")::contains)&&request.getParameterMap().keySet().stream().allMatch(k->fields.contains(k)||Set.of("token","_csrf","_reviewed").contains(k)),"Biểu mẫu chứa trường không được phép cập nhật.");}
    private String sourceUrl(String kind,Long source){return "/ke-hoach-moc/"+kind+"/"+source;}
    @ExceptionHandler(BusinessException.class) String error(BusinessException error,Principal p,HttpServletRequest request,Model model){model.addAttribute("error",error.getMessage());model.addAttribute("currentPath",request.getRequestURI());model.addAttribute("me",p==null?null:tx.execute(s->db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",p.getName()).map(Global::view).orElse(null)));model.addAttribute("back","/bang-dieu-khien");return "electronic-error";}
    private String edit(Model model,ContractSnapshots.Body body,ElectronicContractForm form,String action,String token,String back){model.addAttribute("body",body);model.addAttribute("contractForm",form);model.addAttribute("action",action);model.addAttribute("token",token);model.addAttribute("back",back);return "electronic-form";}
    private String review(Model model,Ticket ticket,String token){model.addAttribute("body",ticket.body());model.addAttribute("action",ticket.action());model.addAttribute("reason",ticket.reason());model.addAttribute("token",token);model.addAttribute("back",ticket.intent().contract()==null?sourceUrl(ticket.intent().kind(),ticket.intent().source()):"/hop-dong/"+ticket.intent().contract());return "electronic-review";}
    @GetMapping("/ke-hoach-moc/{kind}/{sourceId}/hop-dong") String createForm(Principal p,@PathVariable String kind,@PathVariable Long sourceId,HttpSession session,Model model){Long actor=actor(p);var form=new ElectronicContractForm();var preview=contracts.previewElectronic(actor,kind,sourceId,form);if(preview.plan().contractId()!=null)return "redirect:/hop-dong/"+preview.plan().contractId();var intent=intent(actor,kind,sourceId,null,preview.plan().current(),preview.plan().source().fingerprint());model.addAttribute("post",sourceUrl(kind,sourceId)+"/hop-dong/ra-soat");return edit(model,preview.body(),form,"CREATE",remember(session,"SANG_EC_INTENTS",intent),sourceUrl(kind,sourceId));}
    @PostMapping("/ke-hoach-moc/{kind}/{sourceId}/hop-dong/ra-soat") String createReview(Principal p,@PathVariable String kind,@PathVariable Long sourceId,@RequestParam String token,@ModelAttribute("contractForm")ElectronicContractForm form,BindingResult binding,HttpServletRequest request,HttpSession session,Model model){Long actor=actor(p);var intent=stored(session,"SANG_EC_INTENTS",token,Intent.class);authority(intent,actor,kind,sourceId,null);try{bound(binding,request,FIELDS);Checks.require(form.isReviewed(),"Cần xác nhận đã rà soát thông tin, mốc và điều khoản.");var preview=contracts.previewElectronic(actor,kind,sourceId,form);if(preview.plan().contractId()!=null)return "redirect:/hop-dong/"+preview.plan().contractId();Checks.require(preview.plan().current()==intent.version()&&preview.plan().source().fingerprint().equals(intent.digest()),"Nguồn hoặc kế hoạch đã thay đổi. Hãy mở lại để rà soát.");var ticket=new Ticket(intent,"CREATE",preview.body(),null);return review(model,ticket,remember(session,"SANG_EC_TICKETS",ticket));}catch(BusinessException error){model.addAttribute("formError",error.getMessage());var preview=contracts.previewElectronic(actor,kind,sourceId,new ElectronicContractForm());model.addAttribute("post",sourceUrl(kind,sourceId)+"/hop-dong/ra-soat");return edit(model,preview.body(),form,"CREATE",token,sourceUrl(kind,sourceId));}}
    @GetMapping("/hop-dong/{id}/xac-nhan-dien-tu")
    String confirmReview(Principal p,@PathVariable Long id,@RequestParam int phienBan,HttpSession session,Model model) {
        Long actor = actor(p);
        var read = contracts.reviewElectronicAction(actor,id,phienBan,"CONFIRM");
        var ticket = new Ticket(intent(actor,null,null,id,phienBan,read.digest()),"CONFIRM",read.document().body(),null);
        model.addAttribute("read",read);
        model.addAttribute("signer",read.signedBy(actor) == null ? actor : null);
        return review(model,ticket,remember(session,"SANG_EC_TICKETS",ticket));
    }
    @GetMapping("/hop-dong/{id}/dieu-khoan") String amendForm(Principal p,@PathVariable Long id,HttpSession session,Model model){Long actor=actor(p);var read=contracts.electronic(actor,id,0);contracts.reviewElectronicAction(actor,id,read.document().version(),"AMEND");model.addAttribute("post","/hop-dong/"+id+"/dieu-khoan/ra-soat");return edit(model,read.document().body(),snapshots.form(read.document().body()),"AMEND",remember(session,"SANG_EC_INTENTS",intent(actor,null,null,id,read.document().version(),read.digest())),"/hop-dong/"+id);}
    @PostMapping("/hop-dong/{id}/dieu-khoan/ra-soat") String amendReview(Principal p,@PathVariable Long id,@RequestParam String token,@ModelAttribute("contractForm")ElectronicContractForm form,BindingResult binding,HttpServletRequest request,HttpSession session,Model model){Long actor=actor(p);var intent=stored(session,"SANG_EC_INTENTS",token,Intent.class);authority(intent,actor,null,null,id);var read=contracts.reviewElectronicAction(actor,id,intent.version(),"AMEND");Checks.require(read.digest().equals(intent.digest()),"Hợp đồng đã thay đổi. Hãy mở lại để rà soát.");try{bound(binding,request,FIELDS);Checks.require(form.isReviewed(),"Cần xác nhận đã rà soát điều khoản.");var ticket=new Ticket(intent,"AMEND",snapshots.amended(read.document().body(),form),null);return review(model,ticket,remember(session,"SANG_EC_TICKETS",ticket));}catch(BusinessException error){model.addAttribute("formError",error.getMessage());model.addAttribute("post","/hop-dong/"+id+"/dieu-khoan/ra-soat");return edit(model,read.document().body(),form,"AMEND",token,"/hop-dong/"+id);}}
    @GetMapping("/hop-dong/{id}/huy-dien-tu") String cancelForm(Principal p,@PathVariable Long id,HttpSession session,Model model){Long actor=actor(p);var read=contracts.electronic(actor,id,0);contracts.reviewElectronicAction(actor,id,read.document().version(),"CANCEL");model.addAttribute("contractId",id);model.addAttribute("cancelForm",new CancelForm());model.addAttribute("token",remember(session,"SANG_EC_INTENTS",intent(actor,null,null,id,read.document().version(),read.digest())));return "electronic-cancel";}
    @PostMapping("/hop-dong/{id}/huy-dien-tu/ra-soat") String cancelReview(Principal p,@PathVariable Long id,@RequestParam String token,@ModelAttribute("cancelForm")CancelForm form,BindingResult binding,HttpServletRequest request,HttpSession session,Model model){Long actor=actor(p);var intent=stored(session,"SANG_EC_INTENTS",token,Intent.class);authority(intent,actor,null,null,id);var read=contracts.reviewElectronicAction(actor,id,intent.version(),"CANCEL");Checks.require(read.digest().equals(intent.digest()),"Hợp đồng đã thay đổi.");try{bound(binding,request,Set.of("reason"));var ticket=new Ticket(intent,"CANCEL",read.document().body(),Checks.text(form.getReason(),5,2000,"Lý do hủy"));return review(model,ticket,remember(session,"SANG_EC_TICKETS",ticket));}catch(BusinessException error){model.addAttribute("formError",error.getMessage());model.addAttribute("contractId",id);model.addAttribute("token",token);return "electronic-cancel";}}
    @GetMapping("/hop-dong-dien-tu/sua") String back(Principal p,@RequestParam String token,HttpSession session,Model model){var ticket=stored(session,"SANG_EC_TICKETS",token,Ticket.class);var intent=ticket.intent();authority(intent,actor(p),intent.kind(),intent.source(),intent.contract());Checks.require(Set.of("CREATE","AMEND").contains(ticket.action()),"Thao tác không có điều khoản cần sửa.");if(ticket.action().equals("CREATE")){var preview=contracts.previewElectronic(intent.actor(),intent.kind(),intent.source(),snapshots.form(ticket.body()));if(preview.plan().contractId()!=null)return "redirect:/hop-dong/"+preview.plan().contractId();model.addAttribute("post",sourceUrl(intent.kind(),intent.source())+"/hop-dong/ra-soat");return edit(model,preview.body(),snapshots.form(ticket.body()),"CREATE",remember(session,"SANG_EC_INTENTS",intent),sourceUrl(intent.kind(),intent.source()));}contracts.reviewElectronicAction(intent.actor(),intent.contract(),intent.version(),"AMEND");model.addAttribute("post","/hop-dong/"+intent.contract()+"/dieu-khoan/ra-soat");return edit(model,ticket.body(),snapshots.form(ticket.body()),"AMEND",remember(session,"SANG_EC_INTENTS",intent),"/hop-dong/"+intent.contract());}
    @PostMapping("/hop-dong-dien-tu/xac-nhan")
    String finish(Principal p,@RequestParam String token,HttpSession session,HttpServletRequest request,RedirectAttributes redirect) {
        Long actor = actor(p);
        var ticket = stored(session,"SANG_EC_TICKETS",token,Ticket.class);
        var intent = ticket.intent();
        authority(intent,actor,intent.kind(),intent.source(),intent.contract());
        var fields = "CONFIRM".equals(ticket.action()) ? Set.of("token","_csrf","signature") : Set.of("token","_csrf");
        Checks.require(fields.containsAll(request.getParameterMap().keySet()),"Biểu mẫu chứa trường không được phép cập nhật.");
        Long id = intent.contract();
        String message = switch (ticket.action()) {
            case "CREATE" -> {
                id = contracts.createElectronic(actor,intent.kind(),intent.source(),intent.version(),ticket.body()).getMaHopDong();
                yield "Đã lập hợp đồng điện tử. Hai bên cần rà soát và ký cùng phiên bản.";
            }
            case "CONFIRM" -> {
                contracts.confirmElectronic(actor,id,intent.version(),intent.digest(),request.getParameter("signature"));
                yield "Đã ghi nhận chữ ký của bạn trên phiên bản hợp đồng này.";
            }
            case "AMEND" -> {
                contracts.amendElectronic(actor,id,intent.version(),intent.digest(),ticket.body());
                yield "Đã lưu phiên bản điều khoản mới. Hai bên cần ký lại trên phiên bản mới.";
            }
            case "CANCEL" -> {
                contracts.cancelElectronic(actor,id,intent.version(),intent.digest(),ticket.reason());
                yield "Đã hủy hợp đồng trước hiệu lực.";
            }
            default -> throw new BusinessException("Thao tác không hợp lệ.");
        };
        redirect.addFlashAttribute("success",message);
        return "redirect:/hop-dong/"+id;
    }
    @GetMapping("/hop-dong/{id}/phien-ban/{version}") String history(Principal p,@PathVariable Long id,@PathVariable int version,Model model){model.addAttribute("read",contracts.electronic(actor(p),id,version));model.addAttribute("contractId",id);return "electronic-history";}
}
