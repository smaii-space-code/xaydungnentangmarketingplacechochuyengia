package vn.biz04.web;
import org.springframework.stereotype.Component;
import vn.biz04.service.*;
@Component("milestoneLinks")
public class MilestoneLinks {
    private final MilestonePlanService plans;
    public MilestoneLinks(MilestonePlanService plans){this.plans=plans;}
    public record Link(String text,String url,String label,String state){}
    public Link link(Long actor,String kind,Long source){try{var v=plans.details(actor,kind,source,1);if(v.contractId()!=null)return new Link("Mở hợp đồng","/hop-dong/"+v.contractId(),v.label(),v.state());return new Link(v.canEdit()&&v.current()==0?"Thiết lập mốc":v.current()==0?"Chờ chuyên gia thiết lập mốc":"Xem kế hoạch mốc",v.source().planUrl(),v.label(),v.state());}catch(BusinessException unavailable){return null;}}
}
