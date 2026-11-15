package vn.biz04.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.CatalogDiscovery;
import java.security.Principal;

@Controller
public class DiscoveryWeb {
    private final CatalogDiscovery discovery;private final Store db;private final TransactionTemplate tx;
    public DiscoveryWeb(CatalogDiscovery discovery,Store db,TransactionTemplate tx){this.discovery=discovery;this.db=db;this.tx=tx;}
    private Long viewer(Principal principal){
        if(principal==null)throw new AccessDeniedException("Cần đăng nhập.");
        return tx.execute(s->db.first("from NguoiDung u where u.email=:email and u.trangThai='HOAT_DONG'",NguoiDung.class,"email",principal.getName()).orElseThrow(()->new AccessDeniedException("Tài khoản không hoạt động.")).getMaNguoiDung());
    }
    @GetMapping("/") public String home(@RequestParam(defaultValue="")String linhVuc,Model model){var listing=discovery.home(CatalogDiscovery.Filter.parse("",linhVuc,"1","1"));model.addAttribute("discovery",listing);return "home";}
    @GetMapping("/trang/trangchu") public String listing(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String linhVuc,@RequestParam(defaultValue="1")String trangChuyenGia,@RequestParam(defaultValue="1")String trangDichVu,Model model){model.addAttribute("q",q);model.addAttribute("discovery",discovery.listing(CatalogDiscovery.Filter.parse(q,linhVuc,trangChuyenGia,trangDichVu)));return "services";}
    @GetMapping("/chuyen-gia/{id}") public String expert(Principal principal,@PathVariable Long id,@RequestParam(defaultValue="1")String trangDichVu,@RequestParam(defaultValue="1")String trangDanhGia,Model model){Long viewer=viewer(principal);model.addAttribute("profile",discovery.profile(id,CatalogDiscovery.Filter.page(trangDichVu),CatalogDiscovery.Filter.page(trangDanhGia)));model.addAttribute("canHireExpert",tx.execute(s->Boolean.TRUE.equals(db.get(NguoiDung.class,viewer).getLaKhachHang()))&&!viewer.equals(id));return "expert";}
    @GetMapping("/dich-vu/xem/{id}") public String service(Principal principal,@PathVariable Long id,Model model){Long viewer=viewer(principal);var detail=discovery.detail(id);model.addAttribute("detail",detail);model.addAttribute("canRequest",tx.execute(s->Boolean.TRUE.equals(db.get(NguoiDung.class,viewer).getLaKhachHang()))&&!viewer.equals(detail.service().ownerId()));return "service-detail";}
}
