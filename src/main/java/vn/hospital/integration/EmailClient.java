package vn.hospital.integration;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import vn.hospital.util.Config;
import java.util.Properties;
public class EmailClient {
    public boolean configured() { return !Config.get("SMTP_HOST").isBlank(); }
    public void send(String recipient,String subject,String body) throws MessagingException {
        Properties p=new Properties(); p.put("mail.smtp.host",Config.get("SMTP_HOST")); p.put("mail.smtp.port",Config.get("SMTP_PORT")); p.put("mail.smtp.auth","true"); p.put("mail.smtp.starttls.enable","true"); p.put("mail.smtp.starttls.required","true"); p.put("mail.smtp.connectiontimeout","5000"); p.put("mail.smtp.timeout","5000");
        var session=Session.getInstance(p,new Authenticator() { protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(Config.get("SMTP_USER"),Config.get("SMTP_PASSWORD")); } });
        var message=new MimeMessage(session); message.setFrom(new InternetAddress(Config.get("SMTP_FROM"))); message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(recipient,false)); message.setSubject(subject,"UTF-8"); message.setText(body,"UTF-8"); Transport.send(message);
    }
}
