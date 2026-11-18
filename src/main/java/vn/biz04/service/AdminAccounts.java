package vn.biz04.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.biz04.data.Store;
import vn.biz04.domain.*;

@Service
@Transactional
public class AdminAccounts {
    private final Store db;
    private final Access access;
    private final AccountService accounts;
    private final ExpertApplications experts;
    private final RoleObligations obligations;
    private final ExpertAvatars avatars;

    public AdminAccounts(Store db, Access access, AccountService accounts,
                         ExpertApplications experts, RoleObligations obligations, ExpertAvatars avatars) {
        this.db = db;
        this.access = access;
        this.accounts = accounts;
        this.experts = experts;
        this.obligations = obligations;
        this.avatars = avatars;
    }

    public record Contact(String phone, String type, String organization, String address,
                          String introduction, String maskedCccd) {}
    public record ExpertProfile(String state, int version, String field, String bio) {}
    public record Detail(Long id, String name, String email, String role, String status,
                         String emailStatus, OffsetDateTime created, OffsetDateTime verified,
                         Contact contact, ExpertProfile expert, boolean customer, String conversionBlock,
                         boolean expertAccount, boolean hasAvatar, boolean canConfirmOldAdminAccount) {}
    public record Conversion(Long id, String name, String email, String savedPhone, int epoch,
                             ExpertProfile existing, String phone, String field, String bio, String reason) {}

    @Transactional(readOnly = true)
    public Long actor(String email) {
        if (email == null) throw new AccessDeniedException("Cần đăng nhập.");
        return db.first("from NguoiDung u where u.email=:e and u.trangThai='HOAT_DONG' and u.laQuanTriVien=true",
                NguoiDung.class, "e", email).map(NguoiDung::getMaNguoiDung)
                .orElseThrow(() -> new AccessDeniedException("Chỉ quản trị viên được thực hiện."));
    }

    public static String role(NguoiDung u) {
        List<String> labels = new ArrayList<>();
        if (u.getLaQuanTriVien()) labels.add("Quản trị viên");
        if (u.getLaKhachHang()) labels.add("Khách hàng");
        if (u.getLaChuyenGia()) labels.add("Chuyên gia");
        return labels.isEmpty() ? "Chưa có vai trò" : String.join(", ", labels);
    }

    @Transactional(readOnly = true)
    public Detail detail(Long admin, Long id) {
        requireAdmin(db.get(NguoiDung.class, admin));
        NguoiDung u = db.get(NguoiDung.class, id);
        var contact = contact(id).map(h -> new Contact(h.soDienThoai, h.loaiKhachHang, h.tenToChuc,
                h.diaChi, h.gioiThieu, h.cccdBonSoCuoi == null ? null : "********" + h.cccdBonSoCuoi)).orElse(null);
        var expert = profile(id).map(AdminAccounts::profileView).orElse(null);
        boolean expertAccount = !u.getLaQuanTriVien() && (u.getLaChuyenGia() || expert != null);
        return new Detail(id, u.getHoTen(), u.getEmail(), role(u), u.getTrangThai(), u.getTrangThaiEmail(),
                u.getNgayTao(), u.getThoiDiemXacThucEmail(), contact,
                expert,
                u.getLaKhachHang() && !u.getLaChuyenGia() && !u.getLaQuanTriVien(),
                conversionBlock(u), expertAccount, expertAccount && db.count(
                        "select count(a) from AnhDaiDienChuyenGia a where a.userId=:id", "id", id) > 0,
                canConfirmOldAdminAccount(u));
    }

    public Long create(Long admin, String kind, AccountForm form, String field) {
        return create(admin, kind, form, field, null);
    }

    public Long create(Long admin, String kind, AccountForm form, String field, MultipartFile avatar) {
        requireAdmin(db.lockEntity(NguoiDung.class, admin));
        boolean hasImage = ExpertAvatars.selected(avatar);
        Checks.require(!hasImage || "CHUYEN_GIA".equals(kind), "Chỉ đính kèm ảnh khi tạo chuyên gia.");
        NguoiDung user;
        if ("KHACH_HANG".equals(kind)) {
            user = accounts.registerByAdmin(admin, form, false);
        } else if ("CHUYEN_GIA".equals(kind)) {
            ExpertRegistrationForm expert = new ExpertRegistrationForm();
            expert.setHoTen(form.getHoTen());
            expert.setEmail(form.getEmail());
            expert.setPassword(form.getPassword());
            expert.setPasswordAgain(form.getPasswordAgain());
            expert.setSoDienThoai(form.getSoDienThoai());
            expert.setLinhVuc(field);
            expert.setGioiThieu(form.getGioiThieu());
            user = experts.registerByAdmin(admin, expert, UUID.randomUUID().toString());
        } else {
            throw new BusinessException("Chỉ được tạo tài khoản Khách hàng hoặc Chuyên gia.");
        }
        if (hasImage) avatars.replace(admin, user.getMaNguoiDung(), avatar);
        access.audit(admin, "NguoiDung", user.getMaNguoiDung(), "TAO_TAI_KHOAN_QUAN_TRI", kind);
        return user.getMaNguoiDung();
    }

    public void confirmOldAdminAccount(Long admin, Long id) {
        for (Long user : new TreeSet<>(List.of(admin, id))) db.lockEntity(NguoiDung.class, user);
        requireAdmin(db.get(NguoiDung.class, admin));
        var account = db.get(NguoiDung.class, id);
        Checks.require(canConfirmOldAdminAccount(account),
                "Chỉ xác nhận tài khoản do quản trị viên tạo và còn chờ OTP.");
        accounts.confirmEmailByAdmin(admin, account);
        if ("CHUYEN_GIA".equals(account.getNguonDangKy())) experts.submitInitial(account, admin);
        else account.setPhienBaoMat(account.getPhienBaoMat() + 1);
    }

    private boolean canConfirmOldAdminAccount(NguoiDung u) {
        String kind = u.getNguonDangKy();
        return ("KHACH_HANG".equals(kind) || "CHUYEN_GIA".equals(kind))
                && "CHINH_THUC".equals(u.getLoaiTaiKhoan()) && !u.getLaQuanTriVien()
                && "CHO_XAC_THUC".equals(u.getTrangThai()) && "CHO_XAC_THUC".equals(u.getTrangThaiEmail())
                && u.getThoiDiemXacThucEmail() == null
                && db.count("select count(n) from NhatKyXuLy n where n.loaiDoiTuong='NguoiDung' and n.maDoiTuong=:id"
                        + " and n.hanhDong='TAO_TAI_KHOAN_QUAN_TRI' and n.maNguoiThucHien is not null and n.noiDungLyDo=:kind",
                        "id", u.getMaNguoiDung(), "kind", kind) > 0;
    }

    @Transactional(readOnly = true)
    public Conversion review(Long admin, Long id, String phone, String field, String bio, String reason) {
        requireAdmin(db.get(NguoiDung.class, admin));
        return draft(db.get(NguoiDung.class, id), phone, field, bio, reason);
    }

    public void convert(Long admin, Conversion reviewed) {
        Long id = reviewed.id();
        for (Long user : new TreeSet<>(List.of(admin, id))) db.lockEntity(NguoiDung.class, user);
        requireAdmin(db.get(NguoiDung.class, admin));
        NguoiDung u = db.get(NguoiDung.class, id);
        var current = draft(u, reviewed.phone(), reviewed.field(), reviewed.bio(), reviewed.reason());
        Checks.require(current.equals(reviewed), "Tài khoản hoặc hồ sơ đã thay đổi. Vui lòng rà soát lại.");

        var contact = contact(id).orElseGet(() -> {
            var fresh = new HoSoKhachHang();
            fresh.maNguoiDung = id;
            return db.save(fresh);
        });
        if (!validPhone(contact.soDienThoai)) contact.soDienThoai = reviewed.phone();
        var expert = profile(id).orElseGet(() -> {
            var fresh = new HoSoChuyenGia();
            fresh.userId = id;
            fresh.created = access.now().atOffset(ZoneOffset.UTC);
            fresh.operation = UUID.randomUUID().toString();
            fresh.field = reviewed.field();
            fresh.bio = reviewed.bio();
            return db.save(fresh);
        });
        expert.field = reviewed.field();
        expert.bio = reviewed.bio();
        experts.approveConversion(u, expert, admin, reviewed.reason(), UUID.randomUUID().toString());
    }

    private Conversion draft(NguoiDung u, String phone, String field, String bio, String reason) {
        String block = conversionBlock(u);
        Checks.require(block == null, block);
        ExpertApplications.professional(field, bio);
        Checks.text(u.getHoTen(), 1, 100, "Họ tên");
        Long id = u.getMaNguoiDung();
        String savedPhone = contact(id).map(h -> h.soDienThoai).orElse(null);
        String reviewedPhone = validPhone(savedPhone) ? savedPhone : Objects.toString(phone, "").strip();
        Checks.require(validPhone(reviewedPhone), "Cần bổ sung số điện thoại di động Việt Nam gồm 10 chữ số.");
        return new Conversion(id, u.getHoTen(), u.getEmail(), savedPhone, u.getPhienBaoMat(),
                profile(id).map(AdminAccounts::profileView).orElse(null),
                reviewedPhone, field.trim(), bio.trim(), Checks.text(reason, 1, 2000, "Lý do xét duyệt"));
    }

    private String conversionBlock(NguoiDung u) {
        if (!u.getLaKhachHang() || u.getLaChuyenGia() || u.getLaQuanTriVien())
            return "Chỉ chuyển tài khoản hiện có vai trò Khách hàng.";
        if (!"HOAT_DONG".equals(u.getTrangThai()) || !"CHINH_THUC".equals(u.getLoaiTaiKhoan()))
            return "Tài khoản khách hàng phải là tài khoản chính thức đang hoạt động.";
        if (!"DA_XAC_THUC".equals(u.getTrangThaiEmail()) || u.getThoiDiemXacThucEmail() == null)
            return "Khách hàng cần xác thực email trước khi được xét duyệt chuyên gia.";
        return obligations.blocker(u.getMaNguoiDung(), true).orElse(null);
    }

    private Optional<HoSoKhachHang> contact(Long id) {
        return db.first("from HoSoKhachHang h where h.maNguoiDung=:id", HoSoKhachHang.class, "id", id);
    }

    private Optional<HoSoChuyenGia> profile(Long id) {
        return db.first("from HoSoChuyenGia h where h.userId=:id", HoSoChuyenGia.class, "id", id);
    }

    private static ExpertProfile profileView(HoSoChuyenGia h) {
        return new ExpertProfile(h.state, h.version, h.field, h.bio);
    }

    private static void requireAdmin(NguoiDung u) {
        if (!"HOAT_DONG".equals(u.getTrangThai()) || !u.getLaQuanTriVien())
            throw new AccessDeniedException("Chỉ quản trị viên được thực hiện.");
    }

    private static boolean validPhone(String phone) {
        return phone != null && phone.matches("^0[35789][0-9]{8}$");
    }

}
