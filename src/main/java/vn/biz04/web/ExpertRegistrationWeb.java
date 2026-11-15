package vn.biz04.web;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.security.ExpertLoginFailure;
import vn.biz04.service.*;
import java.security.Principal;
import java.util.*;

@Controller
public class ExpertRegistrationWeb {
    public static final String INTENT="SANG_EXPERT_REGISTER_INTENT",RESULT="SANG_EXPERT_REGISTER_RESULT";
    private static final String BASE="/dang-ky/chuyen-gia";
    public record RegistrationIntent(String operation,java.time.Instant expires){}
    public record EditTicket(ExpertApplications.Proof proof,String operation,boolean complete){}
    private final ExpertApplications experts;private final OtpService otp;private final Store db;private final TransactionTemplate tx;private final java.time.Clock clock;
    public ExpertRegistrationWeb(ExpertApplications experts,OtpService otp,Store db,TransactionTemplate tx,java.time.Clock clock){this.experts=experts;this.otp=otp;this.db=db;this.tx=tx;this.clock=clock;}
    @ModelAttribute("accountPage") boolean accountPage(){return true;}
    @InitBinder("expertForm") void bind(WebDataBinder b,HttpServletRequest req){
        b.setAllowedFields(req.getRequestURI().endsWith("/ho-so")?new String[]{"hoTen","soDienThoai","linhVuc","gioiThieu"}:new String[]{"hoTen","email","password","passwordAgain","soDienThoai","linhVuc","gioiThieu"});
        if(b instanceof ExtendedServletRequestDataBinder servletBinder)servletBinder.addHeaderPredicate(header->false);
    }
    private void fields(HttpServletRequest req,String... allowed){Set<String> names=new HashSet<>(List.of(allowed));names.add("_csrf");Checks.require(req.getParameterMap().entrySet().stream().allMatch(e->names.contains(e.getKey())&&e.getValue().length==1),"Biểu mẫu chứa trường không được phép cập nhật.");}
    private void bound(BindingResult b){Checks.require(!b.hasErrors()&&Arrays.stream(b.getSuppressedFields()).allMatch(n->Set.of("_csrf","intent","ticket").contains(n)),"Biểu mẫu chứa trường không được phép cập nhật.");}
    private ExpertApplications.Proof proof(HttpSession s,boolean rejected){ExpertApplications.Proof p=(ExpertApplications.Proof)s.getAttribute(ExpertLoginFailure.PROOF);experts.validateProof(p,rejected);return p;}
    @GetMapping(BASE) String registration(HttpSession session,Model m,Principal p){
        if(p!=null)return "redirect:/bang-dieu-khien";
        synchronized(session){RegistrationIntent intent=(RegistrationIntent)session.getAttribute(INTENT);if(intent==null||!clock.instant().isBefore(intent.expires())){intent=new RegistrationIntent(UUID.randomUUID().toString(),clock.instant().plusSeconds(1800));session.setAttribute(INTENT,intent);session.removeAttribute(RESULT);}m.addAttribute("intent",intent.operation());}
        m.addAttribute("expertForm",new ExpertRegistrationForm());return "expert-register";
    }
    @PostMapping(BASE) String register(@ModelAttribute("expertForm")ExpertRegistrationForm f,BindingResult b,@RequestParam String intent,HttpServletRequest req,Model m,RedirectAttributes r,Principal p){
        if(p!=null)return "redirect:/bang-dieu-khien";
        try{synchronized(req.getSession()){
            fields(req,"hoTen","email","password","passwordAgain","soDienThoai","linhVuc","gioiThieu","intent");bound(b);
            RegistrationIntent expected=(RegistrationIntent)req.getSession().getAttribute(INTENT);Checks.require(expected!=null&&clock.instant().isBefore(expected.expires())&&expected.operation().equals(intent),"Phiên đăng ký hết hạn. Vui lòng tải lại trang.");
            OtpService.Result result=(OtpService.Result)req.getSession().getAttribute(RESULT);if(result==null){result=otp.registerExpert(f,intent,req.getRemoteAddr());req.getSession().setAttribute(RESULT,result);}
            req.getSession().setAttribute("SANG_REGISTER_CHALLENGE",result.token());r.addFlashAttribute(result.success()?"success":"error",result.message());return "redirect:/xac-thuc-email";
        }}catch(BusinessException ex){m.addAttribute("formError",ex.getMessage());}catch(DataIntegrityViolationException ex){m.addAttribute("formError","Email đã được sử dụng hoặc đăng ký đã được xử lý.");}
        f.clearSecrets();m.addAttribute("intent",intent);return "expert-register";
    }
    @GetMapping(BASE+"/trang-thai") String status(HttpSession s,Model m){try{var p=proof(s,false);m.addAttribute("detail",experts.privateDetail(p));m.addAttribute("pendingMessage",ExpertApplications.PENDING);return "expert-status";}catch(BusinessException ex){ExpertLoginFailure.clear(s);return "redirect:/dang-nhap";}}
    @PostMapping(BASE+"/dong") String close(HttpServletRequest req){fields(req);ExpertLoginFailure.clear(req.getSession());return "redirect:/dang-nhap";}
    @PostMapping(BASE+"/dang-ky-lai") String retry(HttpServletRequest req,RedirectAttributes r){
        try{fields(req);synchronized(req.getSession()){var p=proof(req.getSession(),true);var result=otp.startExpertResubmission(p,req.getRemoteAddr());if(result.token()!=null)req.getSession().setAttribute(ExpertLoginFailure.OTP,result.token());r.addFlashAttribute(result.success()?"success":"error",result.message());return "redirect:"+BASE+(result.token()!=null?"/xac-thuc-lai":"/trang-thai");}}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return "redirect:"+BASE+"/trang-thai";}
    }
    @GetMapping(BASE+"/xac-thuc-lai") String retryOtp(HttpSession s,Model m,RedirectAttributes r){try{var p=proof(s,true);m.addAttribute("challenge",otp.expertResubmissionView((String)s.getAttribute(ExpertLoginFailure.OTP),p));return "expert-retry-otp";}catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return "redirect:"+BASE+"/trang-thai";}}
    @PostMapping(BASE+"/xac-thuc-lai") String verify(@RequestParam(defaultValue="")String code,HttpServletRequest req,RedirectAttributes r){
        try{fields(req,"code");synchronized(req.getSession()){var p=proof(req.getSession(),true);var result=otp.verifyExpertResubmission((String)req.getSession().getAttribute(ExpertLoginFailure.OTP),p,code,req.getRemoteAddr());r.addFlashAttribute(result.success()?"success":"error",result.message());if(result.success()){req.getSession().removeAttribute(ExpertLoginFailure.OTP);req.getSession().setAttribute(ExpertLoginFailure.EDIT,new EditTicket(experts.proof(p.userId()),UUID.randomUUID().toString(),false));return "redirect:"+BASE+"/ho-so";}}}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}return "redirect:"+BASE+"/xac-thuc-lai";
    }
    private EditTicket ticket(HttpSession s){EditTicket t=(EditTicket)s.getAttribute(ExpertLoginFailure.EDIT);Checks.require(t!=null&&clock.instant().isBefore(t.proof().expires()),"Phiên chỉnh sửa hết hạn. Vui lòng đăng nhập và xác thực lại.");return t;}
    @GetMapping(BASE+"/ho-so") String edit(HttpSession s,Model m,RedirectAttributes r){try{var t=ticket(s);if(t.complete())return "redirect:/dang-nhap";m.addAttribute("expertForm",experts.edit(t.proof()));m.addAttribute("ticket",t.operation());return "expert-edit";}catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return "redirect:/dang-nhap";}}
    @PostMapping(BASE+"/ho-so") String resubmit(@ModelAttribute("expertForm")ExpertRegistrationForm f,BindingResult b,@RequestParam String ticket,HttpServletRequest req,Model m,RedirectAttributes r){
        try{synchronized(req.getSession()){fields(req,"hoTen","soDienThoai","linhVuc","gioiThieu","ticket");bound(b);var t=ticket(req.getSession());Checks.require(t.operation().equals(ticket),"Phiên chỉnh sửa không hợp lệ.");if(!t.complete())experts.resubmit(t.proof(),t.operation(),f);req.getSession().setAttribute(ExpertLoginFailure.EDIT,new EditTicket(t.proof(),t.operation(),true));req.getSession().removeAttribute(ExpertLoginFailure.PROOF);r.addFlashAttribute("success",ExpertApplications.PENDING);return "redirect:/dang-nhap";}}
        catch(BusinessException ex){m.addAttribute("formError",ex.getMessage());try{var t=ticket(req.getSession());f.setEmail(experts.edit(t.proof()).getEmail());}catch(BusinessException expired){r.addFlashAttribute("error",expired.getMessage());return "redirect:/dang-nhap";}}
        f.clearSecrets();m.addAttribute("ticket",ticket);return "expert-edit";
    }
    private Long admin(Principal p){Checks.require(p!=null,"Cần đăng nhập.");return tx.execute(s->db.first("from NguoiDung u where u.email=:e and u.laQuanTriVien=true and u.trangThai='HOAT_DONG'",NguoiDung.class,"e",p.getName()).orElseThrow(()->new BusinessException("Chỉ quản trị viên được thực hiện.")).getMaNguoiDung());}
    @GetMapping("/quan-tri/chuyen-gia") String queue(Principal p,Model m){Long admin=admin(p);m.addAttribute("applications",experts.queue(admin));m.addAttribute("processed",experts.processed(admin));m.addAttribute("transitional",experts.transitional(admin));return "expert-queue";}
    @GetMapping("/quan-tri/chuyen-gia/{id}") String detail(Principal p,@PathVariable Long id,Model m){m.addAttribute("detail",experts.adminDetail(admin(p),id));return "expert-review";}
    @PostMapping("/quan-tri/chuyen-gia/{id}/duyet") String decide(Principal p,@PathVariable Long id,@RequestParam int phienBan,@RequestParam String action,@RequestParam(defaultValue="")String lyDo,HttpServletRequest req,RedirectAttributes r){
        try{fields(req,"phienBan","action","lyDo");Checks.require(Set.of("DUYET","TU_CHOI").contains(action),"Quyết định không hợp lệ.");experts.decide(admin(p),id,phienBan,"DUYET".equals(action),lyDo);r.addFlashAttribute("success","Đã ghi nhận quyết định xét duyệt.");}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}return "redirect:/quan-tri/chuyen-gia/"+id;
    }
}
