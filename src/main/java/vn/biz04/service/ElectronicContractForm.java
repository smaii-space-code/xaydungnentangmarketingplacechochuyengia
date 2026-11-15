package vn.biz04.service;
/** Only supplemental declarations. Parties, source, plan and money stay server-owned. */
public class ElectronicContractForm {
    private String customerDuties="Cung cấp thông tin cần thiết trong phạm vi thuê; rà soát sản phẩm theo tiêu chí và phản hồi qua nền tảng.",expertDuties="Thực hiện đúng phạm vi, sản phẩm, tiêu chí và kế hoạch mốc đã thống nhất; xử lý chỉnh sửa trong phạm vi đã chốt.",additional="",representativeName="",representativeTitle="";
    private boolean reviewed;
    public String getCustomerDuties(){return customerDuties;} public void setCustomerDuties(String v){customerDuties=v;}
    public String getExpertDuties(){return expertDuties;} public void setExpertDuties(String v){expertDuties=v;}
    public String getAdditional(){return additional;} public void setAdditional(String v){additional=v;}
    public String getRepresentativeName(){return representativeName;} public void setRepresentativeName(String v){representativeName=v;}
    public String getRepresentativeTitle(){return representativeTitle;} public void setRepresentativeTitle(String v){representativeTitle=v;}
    public boolean isReviewed(){return reviewed;} public void setReviewed(boolean v){reviewed=v;}
}
