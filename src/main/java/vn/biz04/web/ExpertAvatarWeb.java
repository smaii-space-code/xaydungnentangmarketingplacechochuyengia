package vn.biz04.web;

import java.security.Principal;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.biz04.service.AdminAccounts;
import vn.biz04.service.BusinessException;
import vn.biz04.service.ExpertAvatars;

@Controller
public class ExpertAvatarWeb {
    private final ExpertAvatars avatars;
    private final AdminAccounts accounts;

    public ExpertAvatarWeb(ExpertAvatars avatars, AdminAccounts accounts) {
        this.avatars = avatars;
        this.accounts = accounts;
    }

    @GetMapping("/anh-chuyen-gia/{id}")
    ResponseEntity<byte[]> publicImage(@PathVariable Long id) {
        return response(avatars.forPublic(id));
    }

    @GetMapping("/quan-tri/tai-khoan/{id}/anh-dai-dien")
    ResponseEntity<byte[]> adminImage(Principal principal, @PathVariable Long id) {
        return response(avatars.forAdmin(actor(principal), id));
    }

    @PostMapping("/quan-tri/tai-khoan/{id}/anh-dai-dien")
    String update(Principal principal, @PathVariable Long id,
                  @RequestParam(name = "avatar", required = false) MultipartFile avatar, RedirectAttributes flash) {
        Long admin = actor(principal);
        try {
            avatars.replace(admin, id, avatar);
            flash.addFlashAttribute("success", "Đã lưu ảnh đại diện.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataAccessException | TransactionException ex) {
            flash.addFlashAttribute("error", "Chưa lưu được ảnh mới. Vui lòng tải lại tài khoản rồi thử lại.");
        }
        return "redirect:/quan-tri/tai-khoan/" + id;
    }

    private Long actor(Principal principal) {
        return accounts.actor(principal == null ? null : principal.getName());
    }

    private static ResponseEntity<byte[]> response(ExpertAvatars.Image image) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }
}
