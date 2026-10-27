package vn.hospital.listener;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.*;
import vn.hospital.dao.*;
import vn.hospital.integration.EmailClient;
@WebListener
public class BackgroundJobs implements ServletContextListener {
    private ScheduledExecutorService executor;
    public void contextInitialized(ServletContextEvent event) {
        executor=Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"hospital-outbox"); t.setDaemon(true); return t; });
        executor.scheduleWithFixedDelay(() -> { try {
            Database.tx(c -> { Jdbc.update(c,"UPDATE appointment SET status='NO_SHOW' WHERE status='BOOKED' AND appointment_date<CURDATE()"); return null; });
            EmailClient client=new EmailClient(); if(!client.configured()) return;
            for(var row:Jdbc.read("SELECT * FROM notification_outbox WHERE status='PENDING' AND attempts<5 ORDER BY id LIMIT 10")) {
                boolean ok; try { client.send((String)row.get("recipient"),(String)row.get("subject"),(String)row.get("body")); ok=true; } catch(Exception e) { ok=false; }
                boolean sent=ok; Database.tx(c -> { Jdbc.update(c,"UPDATE notification_outbox SET status=?,attempts=attempts+1,last_error=? WHERE id=?",sent?"SENT":"PENDING",sent?null:"SMTP delivery failed; check server configuration",row.get("id")); return null; });
            }
        } catch(Exception e) { event.getServletContext().log("Background job could not run",e); } },10,60,TimeUnit.SECONDS);
    }
    public void contextDestroyed(ServletContextEvent event) { if(executor!=null) executor.shutdownNow(); }
}
