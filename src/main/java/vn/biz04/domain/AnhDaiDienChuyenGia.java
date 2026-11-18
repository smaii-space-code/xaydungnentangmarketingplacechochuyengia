package vn.biz04.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "AnhDaiDienChuyenGia")
public class AnhDaiDienChuyenGia {
    @Id
    @Column(name = "MaNguoiDung")
    public Long userId;

    @Column(name = "ThamChieuTep", nullable = false, length = 64)
    public String fileRef;
}
