package vn.biz04.web;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.*;
import java.security.Principal;
import java.util.*;

@Controller
public class HireOfferWeb {
    private final HireOfferService offers;private final Store db;private final TransactionTemplate tx;
    public HireOfferWeb(HireOfferService offers,Store db,TransactionTemplate tx){this.offers=offers;this.db=db;this.tx=tx;}
    private record Intent(Long actor,HireOfferService.Origin origin){}
    private record Ticket(Long actor,Long id,int version,String kind,HireOfferService.Terms terms,String reason,String operation){}
    private record CreateTicket(Long actor,HireOfferService.Origin origin,String title,HireOfferService.Terms terms,String operation){}
    @InitBinder("form") void fields(WebDataBinder binder){
        binder.setAllowedFields("title","scope","deliverable","price","days","reason");
        if(binder instanceof ExtendedServletRequestDataBinder servletBinder)servletBinder.addHeaderPredicate(header->false);
    }
    private void bound(BindingResult result,Set<String> technicalFields){Checks.require(Arrays.stream(result.getSuppressedFields()).allMatch(technicalFields::contains),"Biểu mẫu chứa trường không được phép cập nhật.");}
    private Long actor(Principal p){if(p==null)throw new AccessDeniedException("Cần đăng nhập.");return tx.execute(s->db.first("from NguoiDung u where u.email=:email and u.trangThai='HOAT_DONG'",NguoiDung.class,"email",p.getName()).orElseThrow(()->new AccessDeniedException("Tài khoản không hoạt động.")).getMaNguoiDung());}
    @SuppressWarnings("unchecked") private <T> T stored(HttpSession session,String group,String token,Class<T> type){synchronized(session){Object object=session.getAttribute(group);Object value=object instanceof Map<?,?> map?map.get(token):null;Checks.require(type.isInstance(value),"Phiên gửi đã hết hạn. Hãy mở lại biểu mẫu từ trang chi tiết.");return type.cast(value);}}
    @SuppressWarnings("unchecked") private <T> String remember(HttpSession session,String group,T value){synchronized(session){Map<String,T> map=(Map<String,T>)session.getAttribute(group);if(map==null){map=new LinkedHashMap<>();session.setAttribute(group,map);}String token=UUID.randomUUID().toString();map.put(token,value);while(map.size()>32)map.remove(map.keySet().iterator().next());return token;}}
    private Intent intent(HttpSession session,String token,Long actor){Intent intent=stored(session,"SANG_HIRE_INTENTS",token,Intent.class);if(!intent.actor().equals(actor))throw new AccessDeniedException("Phiên gửi không thuộc tài khoản này.");return intent;}
    private String detail(Long id){return "redirect:/de-nghi-thue/"+id;}
    @ExceptionHandler(BusinessException.class) String failed(BusinessException error,HttpServletRequest request,RedirectAttributes redirect){redirect.addFlashAttribute("error",error.getMessage());var matcher=java.util.regex.Pattern.compile("^/de-nghi-thue/([1-9][0-9]*)(?:/.*)?$").matcher(request.getRequestURI());return matcher.matches()?"redirect:/de-nghi-thue/"+matcher.group(1):"redirect:/bang-dieu-khien";}
    @GetMapping("/de-nghi-thue/gui/chuyen-gia/{expert}") String form(Principal p,@PathVariable Long expert,@RequestParam(required=false)Long dichVu,HttpSession session,Model model){
        Long actor=actor(p);var origin=offers.origin(actor,expert,dichVu);model.addAttribute("origin",origin);model.addAttribute("form",origin.form());model.addAttribute("token",remember(session,"SANG_HIRE_INTENTS",new Intent(actor,origin)));return "hire-form";
    }
    @PostMapping("/de-nghi-thue/gui") String send(Principal p,@RequestParam String token,@ModelAttribute("form")HireOfferForm form,BindingResult binding,HttpSession session,Model model,RedirectAttributes redirect){
        Long actor=actor(p);Intent intent=intent(session,token,actor);try{
            bound(binding,Set.of("token","_csrf"));
            var origin=offers.origin(actor,intent.origin().expert(),intent.origin().source());
            var ticket=new CreateTicket(actor,origin,Checks.text(form.getTitle(),5,255,"Tiêu đề công việc"),offers.validate(form,false),token);
            return createReview(model,ticket,remember(session,"SANG_HIRE_CREATE_CONFIRM",ticket));
        }
        catch(BusinessException error){model.addAttribute("formError",error.getMessage());model.addAttribute("origin",intent.origin());model.addAttribute("token",token);return "hire-form";}
    }
    private CreateTicket createTicket(Principal p,HttpSession session,String token){var ticket=stored(session,"SANG_HIRE_CREATE_CONFIRM",token,CreateTicket.class);if(!ticket.actor().equals(actor(p)))throw new AccessDeniedException("Phiên xác nhận không thuộc tài khoản này.");return ticket;}
    private String createReview(Model model,CreateTicket ticket,String token){model.addAttribute("origin",ticket.origin());model.addAttribute("title",ticket.title());model.addAttribute("terms",ticket.terms());model.addAttribute("token",token);return "hire-create-review";}
    private HireOfferForm createForm(CreateTicket ticket){var form=HireOfferService.form(ticket.terms());form.setTitle(ticket.title());return form;}
    @GetMapping("/de-nghi-thue/gui/sua") String editBeforeSend(Principal p,@RequestParam String token,HttpSession session,Model model){var ticket=createTicket(p,session,token);intent(session,ticket.operation(),ticket.actor());model.addAttribute("origin",ticket.origin());model.addAttribute("form",createForm(ticket));model.addAttribute("token",ticket.operation());return "hire-form";}
    @PostMapping("/de-nghi-thue/gui/xac-nhan") String confirmCreate(Principal p,@RequestParam String token,HttpSession session,HttpServletRequest request,Model model,RedirectAttributes redirect){
        var ticket=createTicket(p,session,token);
        try{
            Checks.require(request.getParameterMap().keySet().stream().allMatch(Set.of("token","_csrf")::contains),"Biểu mẫu chứa trường không được phép cập nhật.");
            intent(session,ticket.operation(),ticket.actor());
            Long id=offers.create(ticket.actor(),ticket.origin().expert(),ticket.origin().source(),ticket.operation(),createForm(ticket));
            redirect.addFlashAttribute("success","Đã gửi đề nghị thuê đến chuyên gia.");return detail(id);
        }catch(BusinessException error){model.addAttribute("formError",error.getMessage());return createReview(model,ticket,token);}
    }
    @GetMapping({"/de-nghi-thue/da-gui","/de-nghi-thue/nhan","/de-nghi-thue/tat-ca"}) String list(Principal p,@RequestParam(defaultValue="1")String trang,HttpServletRequest request,Model model){String side=request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/')+1);model.addAttribute("side",side);model.addAttribute("page",offers.list(actor(p),side,CatalogDiscovery.Filter.page(trang)));return "hire-list";}
    @GetMapping("/de-nghi-thue/{id}") String details(Principal p,@PathVariable Long id,@RequestParam(defaultValue="1")String trangLichSu,Model model){model.addAttribute("offer",offers.details(actor(p),id,CatalogDiscovery.Filter.page(trangLichSu)));return "hire-detail";}
    @GetMapping("/de-nghi-thue/{id}/chap-thuan") String accept(Principal p,@PathVariable Long id,@RequestParam int phienBan,HttpSession session,Model model){
        Long actor=actor(p);var view=offers.responseView(actor,id,phienBan);return confirmation(session,model,view,new Ticket(actor,id,phienBan,"ACCEPT",null,null,UUID.randomUUID().toString()));
    }
    @GetMapping("/de-nghi-thue/{id}/dieu-chinh") String edit(Principal p,@PathVariable Long id,@RequestParam int phienBan,Model model){var view=offers.responseView(actor(p),id,phienBan);model.addAttribute("offer",view);model.addAttribute("form",HireOfferService.form(view.current()));return "hire-edit";}
    @PostMapping("/de-nghi-thue/{id}/dieu-chinh/ra-soat") String reviewRevision(Principal p,@PathVariable Long id,@RequestParam int phienBan,@ModelAttribute("form")HireOfferForm form,BindingResult binding,HttpSession session,Model model,RedirectAttributes redirect){
        Long actor=actor(p);try{bound(binding,Set.of("phienBan","_csrf","id"));var terms=offers.previewRevision(actor,id,phienBan,form);return confirmation(session,model,offers.responseView(actor,id,phienBan),new Ticket(actor,id,phienBan,"REVISE",terms,null,UUID.randomUUID().toString()));}
        catch(BusinessException error){var view=offers.details(actor,id,1);if(!view.canRespond()||view.row().current()!=phienBan){redirect.addFlashAttribute("error",error.getMessage());return detail(id);}model.addAttribute("offer",view);model.addAttribute("formError",error.getMessage());return "hire-edit";}
    }
    @GetMapping("/de-nghi-thue/{id}/tu-choi") String reject(Principal p,@PathVariable Long id,@RequestParam int phienBan,Model model){model.addAttribute("offer",offers.responseView(actor(p),id,phienBan));return "hire-reject";}
    @PostMapping("/de-nghi-thue/{id}/tu-choi/ra-soat") String reviewRejection(Principal p,@PathVariable Long id,@RequestParam int phienBan,@RequestParam String reason,HttpSession session,Model model,RedirectAttributes redirect){
        Long actor=actor(p);try{var view=offers.responseView(actor,id,phienBan);String explanation=Checks.text(reason,5,2000,"Lý do từ chối");return confirmation(session,model,view,new Ticket(actor,id,phienBan,"REJECT",null,explanation,UUID.randomUUID().toString()));}
        catch(BusinessException error){var view=offers.details(actor,id,1);if(!view.canRespond()||view.row().current()!=phienBan){redirect.addFlashAttribute("error",error.getMessage());return detail(id);}model.addAttribute("offer",view);model.addAttribute("reason",reason);model.addAttribute("formError",error.getMessage());return "hire-reject";}
    }
    private String confirmation(HttpSession session,Model model,HireOfferService.Details view,Ticket ticket){model.addAttribute("offer",view);model.addAttribute("kind",ticket.kind());model.addAttribute("terms",ticket.terms());model.addAttribute("reason",ticket.reason());model.addAttribute("token",remember(session,"SANG_HIRE_CONFIRM",ticket));return "hire-confirm";}
    @PostMapping("/de-nghi-thue/{id}/xac-nhan") String confirm(Principal p,@PathVariable Long id,@RequestParam String token,HttpSession session,RedirectAttributes redirect){
        Long actor=actor(p);Ticket ticket=stored(session,"SANG_HIRE_CONFIRM",token,Ticket.class);if(!ticket.actor().equals(actor)||!ticket.id().equals(id))throw new AccessDeniedException("Phiên xác nhận không thuộc đề nghị hoặc tài khoản này.");
        try{switch(ticket.kind()){case "ACCEPT"->{offers.accept(actor,id,ticket.version());redirect.addFlashAttribute("success","Đã thống nhất nội dung thuê. Chờ chuyên gia thiết lập mốc.");return "redirect:/ke-hoach-moc/thue/"+id;}case "REJECT"->offers.reject(actor,id,ticket.version(),ticket.reason());case "REVISE"->offers.revise(actor,id,ticket.version(),ticket.operation(),ticket.terms());default->throw new BusinessException("Thao tác không hợp lệ.");}redirect.addFlashAttribute("success",ticket.kind().equals("ACCEPT")?"Đã thống nhất nội dung thuê. Chờ chuyên gia thiết lập mốc.":ticket.kind().equals("REJECT")?"Đã từ chối đề nghị thuê.":"Đã gửi phiên bản điều chỉnh đến bên còn lại.");}
        catch(BusinessException error){redirect.addFlashAttribute("error",error.getMessage());}return detail(id);
    }
}
