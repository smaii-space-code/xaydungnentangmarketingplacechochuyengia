package vn.biz04.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.*;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.security.ExpertLoginFailure;
import vn.biz04.service.*;
import java.security.Principal;
import java.util.*;

@Controller
public class AccountWeb {
    private final AccountService accounts;private final OtpService otp;private final Store db;private final TransactionTemplate tx;private final UserDetailsService users;
    private static final String PENDING="SANG_REGISTER_CHALLENGE",CHANGE="SANG_EMAIL_CHALLENGE";
    public AccountWeb(AccountService accounts,OtpService otp,Store db,TransactionTemplate tx,UserDetailsService users){this.accounts=accounts;this.otp=otp;this.db=db;this.tx=tx;this.users=users;}
    @ModelAttribute("accountPage") boolean accountPage(){return true;}
    @InitBinder("form") void bind(WebDataBinder b,HttpServletRequest req){
        if("/dang-ky".equals(req.getRequestURI()))b.setAllowedFields("hoTen","email","password","passwordAgain","soDienThoai","loaiKhachHang","tenToChuc");
        else{
            b.setAllowedFields("hoTen","soDienThoai","loaiKhachHang","tenToChuc","diaChi","gioiThieu","soCccdMoi");
        }
        // Form DTOs do not accept HTTP headers as update fields.
        if(b instanceof ExtendedServletRequestDataBinder servletBinder)servletBinder.addHeaderPredicate(header->false);
    }
    private void allowed(BindingResult b){Checks.require(Arrays.stream(b.getSuppressedFields()).allMatch(x->x.equals("_csrf")),"Form chứa trường không được phép cập nhật.");}
    private Long actor(Principal p){Checks.require(p!=null,"Cần đăng nhập.");return tx.execute(s->db.first("from NguoiDung u where u.email=:e and u.trangThai='HOAT_DONG'",NguoiDung.class,"e",p.getName()).orElseThrow(()->new BusinessException("Tài khoản không hoạt động.")).getMaNguoiDung());}
    @GetMapping("/dang-ky") String choose(){return "register-choice";}
    @GetMapping("/dang-ky/khach-hang") String register(Model m){m.addAttribute("form",new AccountForm());return "register";}
    @PostMapping("/dang-ky") String registerPost(@ModelAttribute("form")AccountForm f,BindingResult b,HttpServletRequest req,Model m,RedirectAttributes r){
        try{allowed(b);OtpService.Result result=otp.register(f,req.getRemoteAddr());req.getSession().setAttribute(PENDING,result.token());r.addFlashAttribute(result.success()?"success":"error",result.message());return "redirect:/xac-thuc-email";}
        catch(BusinessException ex){m.addAttribute("formError",ex.getMessage());}catch(DataIntegrityViolationException ex){m.addAttribute("formError","Email đã được sử dụng. Vui lòng kiểm tra lại.");}
        f.clearSecrets();return "register";
    }
    private String token(HttpSession session,boolean change){return (String)session.getAttribute(change?CHANGE:PENDING);}
    @GetMapping("/xac-thuc-email") String otpPage(HttpSession session,Model m,RedirectAttributes r){return showOtp(session,null,false,m,r);}
    @GetMapping("/xac-thuc-email/tu-dang-nhap")
    String pendingLoginPage(Principal principal,HttpSession session,Model model,RedirectAttributes flash){
        if(principal!=null)return "redirect:/bang-dieu-khien";
        try {
            model.addAttribute("email",otp.pendingLoginEmail(loginProof(session)));
            return "otp-pending-login";
        } catch(BusinessException ex) {
            return expiredLoginProof(session,flash,ex);
        }
    }
    @PostMapping("/xac-thuc-email/tu-dang-nhap")
    String sendFromLogin(Principal principal,HttpServletRequest request,RedirectAttributes flash){
        if(principal!=null)return "redirect:/bang-dieu-khien";
        HttpSession session=request.getSession();
        try {
            var result=otp.sendPendingLoginCode(loginProof(session),request.getRemoteAddr());
            flash.addFlashAttribute(result.success()?"success":"error",result.message());
            if(result.token()!=null){
                session.removeAttribute(ExpertLoginFailure.PENDING_EMAIL);
                session.setAttribute(PENDING,result.token());
                return "redirect:/xac-thuc-email";
            }
            return "redirect:/xac-thuc-email/tu-dang-nhap";
        } catch(BusinessException ex) {
            return expiredLoginProof(session,flash,ex);
        }
    }
    private static OtpService.PendingLogin loginProof(HttpSession session){
        return session.getAttribute(ExpertLoginFailure.PENDING_EMAIL) instanceof OtpService.PendingLogin proof?proof:null;
    }
    private static String expiredLoginProof(HttpSession session,RedirectAttributes flash,BusinessException ex){
        session.removeAttribute(ExpertLoginFailure.PENDING_EMAIL);
        flash.addFlashAttribute("error",ex.getMessage());
        return "redirect:/dang-nhap";
    }
    @PostMapping("/xac-thuc-email/tiep-tuc") String resumeRegistration(@RequestParam String email,@RequestParam String password,HttpServletRequest req,RedirectAttributes r){
        try{OtpService.Result result=otp.resumeRegistration(email,password,req.getRemoteAddr());r.addFlashAttribute(result.success()?"success":"error",result.message());if(result.token()!=null){req.getSession().setAttribute(PENDING,result.token());if(req.getSession().getAttribute(ExpertRegistrationWeb.RESULT)!=null)req.getSession().setAttribute(ExpertRegistrationWeb.RESULT,result);return "redirect:/xac-thuc-email";}}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}return "redirect:/dang-ky";
    }
    @GetMapping("/ho-so/xac-thuc-email") String changePage(Principal p,HttpSession session,Model m,RedirectAttributes r){return showOtp(session,actor(p),true,m,r);}
    private String showOtp(HttpSession session,Long actor,boolean change,Model m,RedirectAttributes r){
        try{m.addAttribute("challenge",otp.view(token(session,change),actor));m.addAttribute("change",change);return "otp";}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());return change?"redirect:/ho-so":"redirect:/dang-ky";}
    }
    @PostMapping("/xac-thuc-email") String verifyRegister(@RequestParam(defaultValue="")String code,HttpServletRequest req,RedirectAttributes r){return verify(req,null,false,code,r);}
    @PostMapping("/ho-so/xac-thuc-email") String verifyChange(Principal p,@RequestParam(defaultValue="")String code,HttpServletRequest req,RedirectAttributes r){return verify(req,actor(p),true,code,r);}
    private String verify(HttpServletRequest req,Long actor,boolean change,String code,RedirectAttributes r){
        try{OtpService.Result result=otp.verify(token(req.getSession(),change),actor,code,req.getRemoteAddr());r.addFlashAttribute(result.success()?"success":"error",result.message());
            if(result.success()){req.getSession().removeAttribute(ExpertRegistrationWeb.INTENT);req.getSession().removeAttribute(ExpertRegistrationWeb.RESULT);req.getSession().removeAttribute(change?CHANGE:PENDING);if(change){SecurityContextHolder.clearContext();req.getSession().invalidate();}return "redirect:/dang-nhap";}
        }catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}catch(DataIntegrityViolationException ex){r.addFlashAttribute("error","Email đã được sử dụng. Vui lòng chọn email khác trong hồ sơ.");}
        return change?"redirect:/ho-so/xac-thuc-email":"redirect:/xac-thuc-email";
    }
    @PostMapping("/xac-thuc-email/gui-lai") String resendRegister(@RequestParam(required=false)String emailMoi,HttpServletRequest req,RedirectAttributes r){return resend(req,null,false,emailMoi,r);}
    @PostMapping("/ho-so/xac-thuc-email/gui-lai") String resendChange(Principal p,HttpServletRequest req,RedirectAttributes r){return resend(req,actor(p),true,null,r);}
    private String resend(HttpServletRequest req,Long actor,boolean change,String email,RedirectAttributes r){
        if(email!=null)r.addFlashAttribute("emailMoi",email);
        try{OtpService.Result result=otp.resend(token(req.getSession(),change),actor,email,req.getRemoteAddr());if(result.token()!=null){req.getSession().setAttribute(change?CHANGE:PENDING,result.token());if(!change&&req.getSession().getAttribute(ExpertRegistrationWeb.RESULT)!=null)req.getSession().setAttribute(ExpertRegistrationWeb.RESULT,result);}r.addFlashAttribute(result.success()?"success":"error",result.message());if(result.success())r.getFlashAttributes().remove("emailMoi");}
        catch(BusinessException ex){r.addFlashAttribute("error",ex.getMessage());}catch(DataIntegrityViolationException ex){r.addFlashAttribute("error","Email đã được sử dụng.");}
        return change?"redirect:/ho-so/xac-thuc-email":"redirect:/xac-thuc-email";
    }
    @GetMapping("/ho-so") String profile(Principal p,Model m){Long id=actor(p);m.addAttribute("form",accounts.form(id));profileMeta(id,m);return "profile";}
    private void profileMeta(Long id,Model m){m.addAttribute("cccdMasked",accounts.maskedCccd(id));tx.executeWithoutResult(s->{NguoiDung u=db.get(NguoiDung.class,id);m.addAttribute("emailState",u.getTrangThaiEmail());m.addAttribute("created",u.getNgayTao());m.addAttribute("updated",u.getNgayCapNhat());});}
    @PostMapping("/ho-so") String profilePost(Principal p,@ModelAttribute("form")AccountForm f,BindingResult b,HttpServletRequest req,Model m,RedirectAttributes r){
        Long id=actor(p);try{allowed(b);AccountForm current=accounts.form(id);f.setKhach(current.isKhach());f.setChuyenGia(current.isChuyenGia());accounts.profile(id,f);
            var details=users.loadUserByUsername(p.getName());var auth=UsernamePasswordAuthenticationToken.authenticated(details,null,details.getAuthorities());var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);req.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);
            r.addFlashAttribute("success","Đã lưu hồ sơ.");return "redirect:/ho-so";
        }catch(BusinessException ex){m.addAttribute("formError",ex.getMessage());}f.clearSecrets();f.setEmail(accounts.form(id).getEmail());profileMeta(id,m);return "profile";
    }
    @PostMapping("/ho-so/doi-email") String changeEmail(Principal p,@RequestParam String emailMoi,@RequestParam String matKhauHienTai,HttpServletRequest req,RedirectAttributes r){
        try{OtpService.Result result=otp.startEmailChange(actor(p),emailMoi,matKhauHienTai,req.getRemoteAddr());if(result.token()!=null){r.addFlashAttribute(result.success()?"success":"error",result.message());req.getSession().setAttribute(CHANGE,result.token());return "redirect:/ho-so/xac-thuc-email";}r.addFlashAttribute("emailError",result.message());r.addFlashAttribute("emailMoi",emailMoi);}
        catch(BusinessException ex){r.addFlashAttribute("emailError",ex.getMessage());r.addFlashAttribute("emailMoi",emailMoi);}catch(DataIntegrityViolationException ex){r.addFlashAttribute("emailError","Email đã được sử dụng.");r.addFlashAttribute("emailMoi",emailMoi);}
        return "redirect:/ho-so";
    }
}

