package vn.biz04.service;
/** Only supplemental declarations. Parties, source, plan and money stay server-owned. */
public class ElectronicContractForm {
    private String customerDuties="Cung cấp đúng hạn thông tin, tài liệu và quyền truy cập đã thỏa thuận. Thực hiện ký quỹ mô phỏng theo từng mốc. "
            + "Kiểm tra sản phẩm theo tiêu chí đã chốt, có quyền yêu cầu chỉnh sửa trong phạm vi và phản hồi qua nền tảng trong thời hạn nghiệm thu.";
    private String expertDuties="Thực hiện đúng phạm vi, tiêu chí và lịch bàn giao. Thông báo kịp thời khi có trở ngại ảnh hưởng đến tiến độ. "
            + "Bàn giao sản phẩm và thực hiện chỉnh sửa trong phạm vi đã chốt. Có quyền yêu cầu thông tin cần thiết và nhận khoản giải ngân mô phỏng theo từng mốc đủ điều kiện.";
    private String additional="",representativeName="",representativeTitle="";
    private boolean reviewed;
    private String usageRights=ServiceContractTerms.DEFAULT_USAGE;
    private String confidentiality=ServiceContractTerms.DEFAULT_CONFIDENTIALITY;
    private String liability=ServiceContractTerms.DEFAULT_LIABILITY;
    public String getUsageRights(){return usageRights;} public void setUsageRights(String v){usageRights=v;}
    public String getConfidentiality(){return confidentiality;} public void setConfidentiality(String v){confidentiality=v;}
    public String getLiability(){return liability;} public void setLiability(String v){liability=v;}
    public String getCustomerDuties(){return customerDuties;} public void setCustomerDuties(String v){customerDuties=v;}
    public String getExpertDuties(){return expertDuties;} public void setExpertDuties(String v){expertDuties=v;}
    public String getAdditional(){return additional;} public void setAdditional(String v){additional=v;}
    public String getRepresentativeName(){return representativeName;} public void setRepresentativeName(String v){representativeName=v;}
    public String getRepresentativeTitle(){return representativeTitle;} public void setRepresentativeTitle(String v){representativeTitle=v;}
    public boolean isReviewed(){return reviewed;} public void setReviewed(boolean v){reviewed=v;}
}
