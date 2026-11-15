package vn.biz04.service;
public interface OtpMailGateway {
    enum Delivery { ACCEPTED, RECIPIENT_MISSING, FAILED }
    Delivery send(String recipient,String code);
}
