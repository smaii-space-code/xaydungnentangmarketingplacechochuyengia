package vn.biz04.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.biz04.service.AccountForm;
import vn.biz04.service.AdminAccounts;
import vn.biz04.service.BusinessException;

@Controller
@RequestMapping("/quan-tri/tai-khoan")
public class AdminAccountWeb {
    private static final String BASE = "/quan-tri/tai-khoan/";
    private static final String REVIEW = AdminAccountWeb.class.getName() + ".review";
    private static final Set<String> ACCOUNT_FIELDS = Set.of("_csrf", "kind", "hoTen", "email",
            "password", "passwordAgain", "soDienThoai", "loaiKhachHang", "tenToChuc", "linhVuc", "gioiThieu");
    private final AdminAccounts accounts;
    public AdminAccountWeb(AdminAccounts accounts) {
        this.accounts = accounts;
    }

    private record Ticket(String token, Long admin, AdminAccounts.Conversion draft, Instant expires) {}

    @GetMapping("/{id}")
    String detail(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("account", accounts.detail(actor(principal), id));
        return "admin-account-detail";
    }

    @GetMapping("/tao")
    String createPage(Principal principal, Model model) {
        actor(principal);
        model.addAttribute("fields", Map.of("kind", "KHACH_HANG", "loaiKhachHang", "CA_NHAN"));
        return "admin-account-form";
    }

    @PostMapping("/tao")
    String create(Principal principal, HttpServletRequest request,
                  @RequestParam(name = "avatar", required = false) MultipartFile avatar,
                  Model model, RedirectAttributes flash) {
        Long admin = actor(principal);
        Long id;
        AccountForm form = new AccountForm();
        try {
            allow(request, ACCOUNT_FIELDS);
            var multipart = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
            if (multipart != null && multipart.getMultiFileMap().entrySet().stream()
                    .anyMatch(e -> !e.getKey().equals("avatar") || e.getValue().size() != 1)) {
                throw new BusinessException("Biểu mẫu chỉ nhận một tệp ảnh đại diện.");
            }
            form.setHoTen(request.getParameter("hoTen"));
            form.setEmail(request.getParameter("email"));
            form.setPassword(request.getParameter("password"));
            form.setPasswordAgain(request.getParameter("passwordAgain"));
            form.setSoDienThoai(request.getParameter("soDienThoai"));
            form.setLoaiKhachHang(request.getParameter("loaiKhachHang") == null ? "CA_NHAN" : request.getParameter("loaiKhachHang"));
            form.setTenToChuc(request.getParameter("tenToChuc"));
            form.setGioiThieu(request.getParameter("gioiThieu"));
            id = accounts.create(admin, request.getParameter("kind"), form, request.getParameter("linhVuc"), avatar);
        } catch (BusinessException ex) {
            return formError(request, model, ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            return formError(request, model, "Không lưu được tài khoản hoặc hồ sơ. Kiểm tra email trùng rồi thử lại.");
        } finally {
            form.clearSecrets();
        }

        flash.addFlashAttribute("success", "CHUYEN_GIA".equals(request.getParameter("kind"))
                ? "Đã tạo tài khoản và xác nhận email. Hồ sơ chuyên gia đang chờ duyệt."
                : "Đã tạo tài khoản và xác nhận email. Khách hàng có thể đăng nhập ngay.");
        return "redirect:" + BASE + id;
    }

    @PostMapping("/{id}/xac-nhan-email")
    String confirmOldAdminAccount(Principal principal, @PathVariable Long id, RedirectAttributes flash) {
        Long admin = actor(principal);
        try {
            accounts.confirmOldAdminAccount(admin, id);
            flash.addFlashAttribute("success", "Đã xác nhận email cho tài khoản do quản trị viên tạo.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + BASE + id;
    }

    @GetMapping("/{id}/chuyen-chuyen-gia")
    String convertPage(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("account", accounts.detail(actor(principal), id));
        return "admin-account-convert";
    }

    @PostMapping("/{id}/chuyen-chuyen-gia/ra-soat")
    String review(Principal principal, @PathVariable Long id, HttpServletRequest request,
                  HttpSession session, Model model) {
        Long admin = actor(principal);
        try {
            allow(request, Set.of("_csrf", "phone", "field", "bio", "reason"));
            var draft = accounts.review(admin, id, request.getParameter("phone"), request.getParameter("field"),
                    request.getParameter("bio"), request.getParameter("reason"));
            var ticket = new Ticket(UUID.randomUUID().toString(), admin, draft, Instant.now().plusSeconds(900));
            session.setAttribute(REVIEW, ticket);
            model.addAttribute("ticket", ticket.token());
            model.addAttribute("review", draft);
        } catch (BusinessException ex) {
            model.addAttribute("account", accounts.detail(admin, id));
            model.addAttribute("formError", ex.getMessage());
            model.addAttribute("input", values(request, Set.of("phone", "field", "bio", "reason")));
        }
        return "admin-account-convert";
    }

    @PostMapping("/{id}/chuyen-chuyen-gia")
    String convert(Principal principal, @PathVariable Long id, HttpServletRequest request,
                   HttpSession session, RedirectAttributes flash) {
        Long admin = actor(principal);
        try {
            allow(request, Set.of("_csrf", "ticket"));
            Ticket ticket;
            synchronized (session) {
                if (!(session.getAttribute(REVIEW) instanceof Ticket pending)
                        || !pending.admin().equals(admin) || !pending.draft().id().equals(id)
                        || !pending.token().equals(request.getParameter("ticket"))
                        || !Instant.now().isBefore(pending.expires())) {
                    throw new BusinessException("Phiên rà soát đã hết hạn hoặc đã được dùng. Vui lòng tải lại tài khoản.");
                }
                ticket = pending;
                session.removeAttribute(REVIEW);
            }
            accounts.convert(admin, ticket.draft());
            flash.addFlashAttribute("success", "Đã xét duyệt và chuyển sang Chuyên gia. Tài khoản cần đăng nhập lại.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("error", "Chưa lưu được quyết định. Vui lòng tải lại tài khoản và rà soát lại.");
        }
        return "redirect:" + BASE + id;
    }

    private Long actor(Principal principal) {
        return accounts.actor(principal == null ? null : principal.getName());
    }

    private static void allow(HttpServletRequest request, Set<String> fields) {
        if (request.getParameterMap().entrySet().stream()
                .anyMatch(e -> !fields.contains(e.getKey()) || e.getValue().length != 1)) {
            throw new BusinessException("Biểu mẫu có trường không được phép hoặc bị gửi lặp.");
        }
    }

    private static Map<String, String> values(HttpServletRequest request, Set<String> fields) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String field : fields) {
            String value = request.getParameter(field);
            if (value != null) result.put(field, value);
        }
        return result;
    }

    private static String formError(HttpServletRequest request, Model model, String message) {
        model.addAttribute("formError", message);
        model.addAttribute("fields", values(request, Set.of("kind", "hoTen", "email", "soDienThoai",
                "loaiKhachHang", "tenToChuc", "linhVuc", "gioiThieu")));
        return "admin-account-form";
    }
}
