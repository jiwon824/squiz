package com.ssafy.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 이메일 전송 (공통 메서드)
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
} catch (Exception e) {
    throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    /**
     * 비밀번호 재설정 이메일 전송 (HTML 버전)
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[SQUIZ] 비밀번호 재설정 안내");

            String htmlContent = String.format(
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;'>" +
                            "<div style='background-color: white; padding: 30px; border-radius: 10px;'>" +
                            "<h2 style='color: #4A90E2; margin-bottom: 20px;'>🔐 비밀번호 재설정</h2>" +
                            "<p>안녕하세요, SQUIZ입니다.</p>" +
                            "<p>비밀번호 재설정을 요청하셨습니다.<br>" +
                            "아래 버튼을 클릭하여 새 비밀번호를 설정해주세요.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='%s' style='background-color: #4A90E2; color: white; padding: 15px 40px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;'>비밀번호 재설정하기</a>" +
                            "</div>" +
                            "<p style='color: #666; font-size: 14px;'>또는 아래 링크를 복사하여 브라우저에 붙여넣으세요:</p>" +
                            "<p style='background-color: #f5f5f5; padding: 10px; border-radius: 5px; word-break: break-all; font-size: 12px;'>%s</p>" +
                            "<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>" +
                            "<p style='color: #999; font-size: 12px;'>⏰ 본 링크는 30분간 유효합니다.</p>" +
                            "<p style='color: #999; font-size: 12px;'>❓ 요청하지 않으셨다면 이 메일을 무시해주세요.</p>" +
                            "</div>" +
                            "<p style='text-align: center; color: #999; font-size: 11px; margin-top: 20px;'>© 2026 SQUIZ. All rights reserved.</p>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    resetLink,
                    resetLink
            );

            helper.setText(htmlContent, true); // true = HTML 형식

            mailSender.send(message);
} catch (Exception e) {
    throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    /**
     * 회원가입 인증 이메일 전송
     */
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        String subject = "[SQUIZ] 이메일 인증";
        String text = String.format(
                "안녕하세요, SQUIZ입니다.\n\n" +
                        "회원가입 인증 코드: %s\n\n" +
                        "인증 코드를 입력하여 회원가입을 완료해주세요.\n" +
                        "본 코드는 10분간 유효합니다.\n\n" +
                        "감사합니다.",
                verificationCode
        );

        sendEmail(toEmail, subject, text);
    }
}
