package vn.biz04.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.BusinessException;
import java.security.Principal;
import java.util.*;
@ControllerAdvice
public class Global {
    private final Store db;public Global(Store db){this.db=db;}
    @ModelAttribute("currentPath") public String currentPath(HttpServletRequest request){return request.getRequestURI();}
    public static Map<String,Object> view(NguoiDung u){return Map.of("id",u.getMaNguoiDung(),"hoTen",u.getHoTen(),"email",u.getEmail(),"khach",u.getLaKhachHang(),"chuyenGia",u.getLaChuyenGia(),"admin",u.getLaQuanTriVien(),"trangThai",u.getTrangThai(),"findProjects",u.getLaChuyenGia()&&!u.getLaQuanTriVien());}
    @ModelAttribute("me") @Transactional(readOnly=true) public Map<String,Object> me(Principal p){return p==null?null:db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",p.getName()).map(Global::view).orElse(null);}
    @ModelAttribute("names") @Transactional(readOnly=true) public Map<Long,String> names(){Map<Long,String> names=new HashMap<>();for(Object[] u:db.list("select u.maNguoiDung,u.hoTen from NguoiDung u",Object[].class))names.put((Long)u[0],(String)u[1]);return names;}
    @ExceptionHandler(BusinessException.class) public String business(BusinessException e,RedirectAttributes r,HttpServletRequest req){r.addFlashAttribute("error",e.getMessage());return "redirect:"+("/dang-ky".equals(req.getRequestURI())?"/dang-ky":"/bang-dieu-khien");}
    @ExceptionHandler(DataIntegrityViolationException.class) public String integrity(RedirectAttributes r){r.addFlashAttribute("error","Dữ liệu bị trùng hoặc đang được thao tác ở phiên khác. Hãy tải lại trang trước khi thử lại.");return "redirect:/bang-dieu-khien";}
    @ExceptionHandler(MaxUploadSizeExceededException.class) public String large(RedirectAttributes r){r.addFlashAttribute("error","Tệp quá lớn. Giới hạn tải lên là 20 MB.");return "redirect:/bang-dieu-khien";}
}
