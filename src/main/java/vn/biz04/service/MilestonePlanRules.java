package vn.biz04.service;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class MilestonePlanRules {
    private MilestonePlanRules(){}
    public static final BigDecimal RATE=new BigDecimal("0.1000");
    public static final String FEE_RULE="DEDUCT_EXPERT_ALLOCATION_HALF_UP";
    public record Item(String stableKey,int position,String title,String work,String deliverable,String criteria,String unit,int due,BigDecimal amount,BigDecimal fee,BigDecimal net,int revisions){}
    public static final String DAY_RULE="DAY_OFFSET",MONTH_RULE="MONTH_CAPPED_PROJECT_END";
    public record Terms(String mode,String reason,List<Item> items,BigDecimal total,BigDecimal fee,BigDecimal net,BigDecimal rate,String feeRule,String deadlineRule){public Terms{items=List.copyOf(items);}public Terms(String mode,String reason,List<Item> items,BigDecimal total,BigDecimal fee,BigDecimal net,BigDecimal rate,String feeRule){this(mode,reason,items,total,fee,net,rate,feeRule,"MONTH".equals(mode)?MONTH_RULE:DAY_RULE);}}
    public static LocalDate relativeDate(LocalDate start,String unit,int offset){Checks.require(start!=null,"Chưa có ngày bắt đầu; chỉ hiển thị thời hạn tương đối.");Checks.require(offset>0,"Hạn tương đối phải lớn hơn 0.");return switch(unit){case "DAY"->start.plusDays(offset);case "MONTH"->start.plusMonths(offset);default->throw new BusinessException("Đơn vị thời hạn không hợp lệ.");};}
    public static List<Integer> fortnight(int days){Checks.require(days>0,"Nguồn chưa có thời gian thực hiện hợp lệ.");int count=(days+14)/15;Checks.require(count<=10,"Chia mỗi 15 ngày vượt 10 mốc. Hãy tự chia theo sản phẩm bàn giao.");List<Integer> dates=new ArrayList<>();for(int i=1;i<=count;i++)dates.add(Math.min(i*15,days));return List.copyOf(dates);}
    private static final java.util.concurrent.ConcurrentMap<Integer,Integer> MONTH_SPANS=new java.util.concurrent.ConcurrentHashMap<>();
    private static int maximumMonthSpan(int months){return MONTH_SPANS.computeIfAbsent(months,n->{int max=0;for(int year=2000;year<2400;year++)for(int month=1;month<=12;month++){var start=LocalDate.of(year,month,1);max=Math.max(max,(int)ChronoUnit.DAYS.between(start,start.plusMonths(n)));}return max;});}
    /** Calendar boundaries guaranteed before the agreed end, then one exact project-end milestone. */
    public static int monthEndOffset(int days){Checks.require(days>0,"Cần thời lượng thuê hợp lệ.");int months=1;while(maximumMonthSpan(months)<days)months++;return months;}
    public static List<Integer> monthly(int days){int count=monthEndOffset(days);Checks.require(count<=10,"Gợi ý mỗi tháng vượt 10 mốc. Hãy tự chia theo sản phẩm bàn giao.");List<Integer> result=new ArrayList<>();for(int i=1;i<=count;i++)result.add(i);return List.copyOf(result);}
    public static LocalDate resolve(LocalDate start,Item item,int agreedDays,boolean last,String rule){var end=relativeDate(start,"DAY",agreedDays);if(MONTH_RULE.equals(rule)){Checks.require(item.unit().equals("MONTH"),"Đơn vị mốc không khớp quy tắc tháng.");if(last)return end;var month=relativeDate(start,"MONTH",item.due());return month.isAfter(end)?end:month;}Checks.require(DAY_RULE.equals(rule)&&item.unit().equals("DAY"),"Quy tắc thời hạn không hợp lệ.");var day=relativeDate(start,"DAY",item.due());Checks.require(!day.isAfter(end),"Hạn mốc vượt thời lượng đã thống nhất.");return day;}
    public static int integer(String value,int min,int max,String label){Checks.require(value!=null&&value.matches("[0-9]{1,10}"),label+" phải là số nguyên từ "+min+" đến "+max+".");long number=Long.parseLong(value);Checks.require(number>=min&&number<=max,label+" phải từ "+min+" đến "+max+".");return (int)number;}
    public static Terms validate(MilestonePlanForm form,BigDecimal price,int days,boolean revision){
        String mode=form.getMode();Checks.require(Set.of("MANUAL","FORTNIGHT","MONTH").contains(mode),"Cách chia mốc không hợp lệ.");
        boolean months="MONTH".equals(mode);int endOffset=months?monthEndOffset(days):days;
        Checks.require(form.getItems()!=null&&form.getItems().size()>=1&&form.getItems().size()<=10,"Kế hoạch phải có từ 1 đến 10 mốc.");
        String reason=revision?Checks.text(form.getReason(),5,2000,"Lý do điều chỉnh"):null;Set<String> keys=new HashSet<>();List<Item> items=new ArrayList<>();int previous=0;BigDecimal total=BigDecimal.ZERO,fees=BigDecimal.ZERO;
        for(var row:form.getItems()){
            Checks.require(row!=null&&row.getStableKey()!=null&&row.getStableKey().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")&&keys.add(row.getStableKey()),"Định danh mốc thiếu, trùng hoặc không hợp lệ.");
            int due=integer(row.getDue(),1,endOffset,months?"Hạn tương đối (tháng lịch)":"Hạn tương đối (ngày)");Checks.require(due>=previous,"Hạn bàn giao phải theo thứ tự không giảm và không vượt thời lượng thuê.");previous=due;
            Checks.require(row.getAmount()!=null&&row.getAmount().matches("[0-9]{1,13}"),"Giá trị mốc phải là số nguyên VND lớn hơn 0.");BigDecimal amount=Checks.money(new BigDecimal(row.getAmount()),false),fee=amount.multiply(RATE).setScale(0,RoundingMode.HALF_UP);
            items.add(new Item(row.getStableKey(),items.size()+1,Checks.text(row.getTitle(),3,255,"Tên mốc"),Checks.text(row.getWork(),5,10000,"Công việc"),Checks.text(row.getDeliverable(),5,5000,"Sản phẩm bàn giao"),Checks.text(row.getCriteria(),5,10000,"Tiêu chí nghiệm thu"),months?"MONTH":"DAY",due,amount,fee,amount.subtract(fee),integer(row.getRevisions(),0,Integer.MAX_VALUE,"Số lần chỉnh sửa")));
            total=total.add(amount);fees=fees.add(fee);
        }
        Checks.require(total.compareTo(price)==0,"Tổng giá trị các mốc phải bằng đúng giá thuê đã thống nhất.");
        Checks.require(previous==endOffset,"Mốc cuối phải khớp giới hạn thời gian thuê đã thống nhất.");
        return new Terms(mode,reason,items,total,fees,total.subtract(fees),RATE,FEE_RULE,months?MONTH_RULE:DAY_RULE);
    }
    public static MilestonePlanForm form(Terms terms){var result=new MilestonePlanForm();result.setMode(terms.mode());result.setReason(terms.reason());for(var item:terms.items()){var row=new MilestonePlanForm.Item();row.setStableKey(item.stableKey());row.setTitle(item.title());row.setWork(item.work());row.setDeliverable(item.deliverable());row.setCriteria(item.criteria());row.setDue(Integer.toString(item.due()));row.setAmount(item.amount().toPlainString());row.setRevisions(Integer.toString(item.revisions()));result.getItems().add(row);}return result;}
}
