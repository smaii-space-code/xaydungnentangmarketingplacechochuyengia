package vn.biz04.data;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import vn.biz04.domain.*;
import vn.biz04.service.BusinessException;
import java.util.*;
@Repository
public class Store {
    @PersistenceContext private EntityManager em;
    public <T> T get(Class<T> type,Long id) { T x=id==null?null:em.find(type,id); if(x==null) throw new BusinessException("Không tìm thấy dữ liệu."); return x; }
    public <T> T save(T x) { em.persist(x); return x; }
    public void remove(Object x) { em.remove(x); }
    public void flush() { em.flush(); }
    public void refresh(Object x) { em.refresh(x); }
    public <T> T lockEntity(Class<T> type,Long id) { T x=em.find(type,id,LockModeType.PESSIMISTIC_WRITE); if(x==null) throw new BusinessException("Không tìm thấy dữ liệu."); em.flush(); em.refresh(x,LockModeType.PESSIMISTIC_WRITE); return x; }
    public HopDong lock(Long id) { HopDong initial=get(HopDong.class,id); if(initial.getMaKeHoach()!=null)for(Long user:new TreeSet<>(List.of(initial.getMaKhachHang(),initial.getMaChuyenGia())))lockEntity(NguoiDung.class,user); return lockEntity(HopDong.class,id); }
    public <T> List<T> list(String hql,Class<T> type,Object... pairs) {
        TypedQuery<T> q=em.createQuery(hql,type); for(int i=0;i<pairs.length;i+=2) q.setParameter((String)pairs[i],pairs[i+1]); return q.getResultList();
    }
    public <T> Optional<T> first(String hql,Class<T> type,Object... pairs) { return list(hql,type,pairs).stream().findFirst(); }
    public long count(String hql,Object... pairs) { return list(hql,Long.class,pairs).get(0); }
}
