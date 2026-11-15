-- Chạy trong SSMS bằng tài khoản có quyền tạo database.
-- Chỉ tạo BIZ04 khi chưa có; không xóa/reset dữ liệu.
IF DB_ID(N'BIZ04') IS NULL
BEGIN
    CREATE DATABASE [BIZ04];
END;
SELECT @@SERVERNAME AS TenMayChu,
       SERVERPROPERTY('ProductVersion') AS PhienBanSQLServer,
       SERVERPROPERTY('IsIntegratedSecurityOnly') AS ChiChoPhepWindowsAuthentication,
       CONNECTIONPROPERTY('local_tcp_port') AS CongTCPDangKetNoi;
-- Nếu cột cổng là NULL, phiên SSMS có thể dùng Shared Memory;
-- cần xác nhận TCP/IP của SQL Server trước khi kết nối JDBC.
