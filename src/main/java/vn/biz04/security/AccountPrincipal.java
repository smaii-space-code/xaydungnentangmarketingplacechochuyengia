package vn.biz04.security;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import vn.biz04.domain.NguoiDung;
import java.util.List;

public class AccountPrincipal extends User {
    private final Long id;private final int epoch;
    public AccountPrincipal(NguoiDung u,List<String> roles){super(u.getEmail(),u.getMatKhauBam(),"HOAT_DONG".equals(u.getTrangThai()),true,true,true,AuthorityUtils.createAuthorityList(roles.stream().map(r->"ROLE_"+r).toArray(String[]::new)));id=u.getMaNguoiDung();epoch=u.getPhienBaoMat();}
    public Long id(){return id;}public int epoch(){return epoch;}
}
