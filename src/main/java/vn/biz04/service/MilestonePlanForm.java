package vn.biz04.service;
import java.util.*;

public class MilestonePlanForm {
    private String mode="MANUAL",reason="";
    private List<Item> items=new ArrayList<>();
    public String getMode(){return mode;} public void setMode(String value){mode=value;}
    public String getReason(){return reason;} public void setReason(String value){reason=value;}
    public List<Item> getItems(){return items;} public void setItems(List<Item> value){items=value;}
    public static class Item {
        private String stableKey=UUID.randomUUID().toString(),title="",work="",deliverable="",criteria="",due="",amount="",revisions="0";
        public String getStableKey(){return stableKey;} public void setStableKey(String v){stableKey=v;}
        public String getTitle(){return title;} public void setTitle(String v){title=v;}
        public String getWork(){return work;} public void setWork(String v){work=v;}
        public String getDeliverable(){return deliverable;} public void setDeliverable(String v){deliverable=v;}
        public String getCriteria(){return criteria;} public void setCriteria(String v){criteria=v;}
        public String getDue(){return due;} public void setDue(String v){due=v;}
        public String getAmount(){return amount;} public void setAmount(String v){amount=v;}
        public String getRevisions(){return revisions;} public void setRevisions(String v){revisions=v;}
    }
}
