package vn.biz04.config;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import vn.biz04.data.Store;
import vn.biz04.domain.*;
import vn.biz04.service.*;
import java.util.*;
import org.slf4j.*;
@Component
@ConditionalOnProperty(name="biz.scheduler.enabled",havingValue="true",matchIfMissing=true)
public class ExpiryJobs {
    private static final Logger LOG=LoggerFactory.getLogger(ExpiryJobs.class);
    private final Store db;private final TransactionTemplate tx;private final Access access;private final DeliveryService delivery;private final ReviewService reviews;
    public ExpiryJobs(Store db,TransactionTemplate tx,Access access,DeliveryService delivery,ReviewService reviews){this.db=db;this.tx=tx;this.access=access;this.delivery=delivery;this.reviews=reviews;}
    @Scheduled(fixedDelayString="${biz.scheduler.delay-ms:60000}")
    public void run(){List<Long> mids=tx.execute(s->db.list("select m.maMocNghiemThu from MocNghiemThu m where m.trangThai='CHO_NGHIEM_THU' and m.hanNghiemThu<=:n",Long.class,"n",access.now()));for(Long id:mids)try{delivery.autoAccept(id);}catch(RuntimeException e){LOG.warn("Chưa xử lý tự động mốc {}: {}",id,e.getClass().getSimpleName());}
        List<Long> hs=tx.execute(s->db.list("select h.maHopDong from HopDong h where h.ngayKetThuc<=:n and h.trangThai in ('HOAN_THANH','DA_CHAM_DUT')",Long.class,"n",access.now().minusDays(14)));for(Long id:hs)try{reviews.publish(id);}catch(RuntimeException e){LOG.warn("Chưa công khai đánh giá hợp đồng {}",id);}
    }
}
