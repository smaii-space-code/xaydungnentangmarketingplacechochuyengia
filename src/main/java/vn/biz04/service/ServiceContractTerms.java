package vn.biz04.service;

/** Text stored inside the reviewed contract snapshot. */
public record ServiceContractTerms(
        String templateVersion, String usageRights, String confidentiality, String liability,
        String disputeResolution, String electronicAgreement, String legalBasis) {

    static final String DEFAULT_USAGE = "Sau khi mốc liên quan được nghiệm thu và tất toán theo quy trình mô phỏng, Bên A được sử dụng sản phẩm đã bàn giao cho mục đích của công việc đã thỏa thuận. "
            + "Quyền đối với tài liệu có trước, công cụ và thành phần của bên thứ ba tuân theo quyền và giấy phép tương ứng. "
            + "Nếu cần chuyển nhượng quyền tác giả, giao mã nguồn hoặc cấp quyền độc quyền, hai bên ghi rõ đối tượng, phạm vi và điều kiện trong hợp đồng.";
    static final String DEFAULT_CONFIDENTIALITY = "Hai bên chỉ sử dụng tài liệu và thông tin riêng nhận được để thực hiện hợp đồng, giới hạn người được tiếp cận theo nhu cầu công việc "
            + "và bảo vệ thông tin trong và sau khi thực hiện. Việc tiết lộ phải được bên cung cấp đồng ý hoặc có căn cứ pháp luật. "
            + "Không đưa thông tin riêng của bên kia vào nội dung công khai khi chưa được phép.";
    static final String DEFAULT_LIABILITY = "Bên vi phạm thông báo cho bên kia khi phát hiện vi phạm, phối hợp khắc phục và chịu trách nhiệm đối với thiệt hại theo thỏa thuận và pháp luật áp dụng. "
            + "Nếu có thỏa thuận phạt vi phạm, hai bên ghi rõ hành vi, mức phạt và điều kiện áp dụng phù hợp quy định pháp luật.";

    static ServiceContractTerms draft(ElectronicContractForm form) {
        return new ServiceContractTerms("SC-DV-01", usage(form), confidentiality(form), liability(form),
                "Hai bên ưu tiên trao đổi và thương lượng qua kênh của hợp đồng. Có thể dùng chức năng tranh chấp của Sang Connect "
                        + "để ghi nhận chứng cứ và xử lý giao dịch mô phỏng. Khi không giải quyết được, mỗi bên có thể yêu cầu cơ quan có thẩm quyền "
                        + "theo pháp luật Việt Nam. Cơ chế trên nền tảng không hạn chế quyền khiếu nại hoặc yêu cầu giải quyết theo luật.",
                "Hai bên dùng tài khoản của mình để đọc và xác nhận cùng một phiên bản. Nền tảng ghi nhận tài khoản, thời điểm và dấu kiểm tra nội dung. "
                        + "Bản đang chờ xác nhận chưa hoàn tất giao kết trên hệ thống. Sau khi hai bên xác nhận cùng phiên bản, thời hạn thực hiện "
                        + "tính từ lần ký quỹ mô phỏng thành công của mốc đầu tiên. Bản này ghi nhận xác nhận trên nền tảng. Hệ thống chưa tích hợp chữ ký số.",
                "Tham chiếu Bộ luật Dân sự số 91/2015/QH13, Điều 117, 119, 398 và các điều về hợp đồng dịch vụ từ Điều 513. "
                        + "Tham chiếu Luật Giao dịch điện tử số 20/2023/QH15 về thông điệp dữ liệu và giao kết hợp đồng điện tử.");
    }

    ServiceContractTerms amended(ElectronicContractForm form) {
        return new ServiceContractTerms(templateVersion, usage(form), confidentiality(form), liability(form),
                disputeResolution, electronicAgreement, legalBasis);
    }

    private static String usage(ElectronicContractForm form) {
        return Checks.text(form.getUsageRights(), 5, 2000, "Quyền sử dụng sản phẩm");
    }

    private static String confidentiality(ElectronicContractForm form) {
        return Checks.text(form.getConfidentiality(), 5, 2000, "Bảo mật thông tin");
    }

    private static String liability(ElectronicContractForm form) {
        return Checks.text(form.getLiability(), 5, 2000, "Trách nhiệm do vi phạm");
    }
}
