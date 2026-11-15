package vn.biz04.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.NguoiDung;
import vn.biz04.service.*;
import java.security.Principal;
import java.util.*;

@Controller
public class ProjectWeb {
    private final Store db;private final Access access;private final TransactionTemplate tx;private final ProjectDiscovery projects;
    public ProjectWeb(Store db,Access access,TransactionTemplate tx,ProjectDiscovery projects){this.db=db;this.access=access;this.tx=tx;this.projects=projects;}
    @GetMapping("/tim-du-an") public String projects(Principal principal,Model m,@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="")String linhVuc,@RequestParam(defaultValue="")String tu,@RequestParam(defaultValue="")String den,@RequestParam(defaultValue="1")String trang){
        Long actor=tx.execute(s->access.user(db.first("from NguoiDung u where u.email=:e",NguoiDung.class,"e",principal.getName()).orElseThrow(()->new BusinessException("Tài khoản không tồn tại.")).getMaNguoiDung()).getMaNguoiDung());
        projects.requireExpert(actor);
        m.addAttribute("filters",Map.of("q",q,"field",linhVuc,"min",tu,"max",den,"page",trang));
        try{m.addAttribute("result",projects.find(actor,ProjectDiscovery.Filter.parse(q,linhVuc,tu,den,trang)));}
        catch(BusinessException ex){m.addAttribute("filterError",ex.getMessage());m.addAttribute("result",new ProjectDiscovery.Page(List.of(),0,1,1,null,null,List.of()));}
        return "projects";
    }
}
