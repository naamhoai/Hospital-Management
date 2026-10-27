package vn.hospital;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import vn.hospital.dao.*;
import vn.hospital.service.*;
import vn.hospital.model.*;
import vn.hospital.util.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
/** Runs only against the explicitly prepared disposable hospital_test database. */
@EnabledIfEnvironmentVariable(named="HOSPITAL_INTEGRATION",matches="1")
class WorkflowTest {
    private final AuthUser doctor=new AuthUser(6,"Doctor","DOCTOR"),reception=new AuthUser(4,"Reception","RECEPTIONIST"),cashier=new AuthUser(3,"Cashier","CASHIER"),lab=new AuthUser(5,"Lab","LAB_TECHNICIAN");
    @BeforeAll static void isolatedDatabase() { Database.tx(c -> { assertEquals("hospital_test",c.getCatalog(),"Integration tests require a disposable hospital_test database"); return null; }); }
    private long schedule(int capacity) {
        return Database.tx(c -> {
            Set<LocalTime> taken=new HashSet<>(); for(var a:Jdbc.list(c,"SELECT appointment_time FROM appointment WHERE doctor_id=6 AND appointment_date=CURDATE() AND active_slot IS NOT NULL")) taken.add(((java.sql.Time)a.get("appointment_time")).toLocalTime());
            LocalTime start=LocalTime.now().plusMinutes(2).withSecond(0).withNano(0); LocalTime end=LocalTime.of(23,59,59);
            for(int trial=0;trial<100;trial++) { long step=Duration.between(start,end).toMinutes()/capacity; boolean free=true; for(int i=0;i<capacity;i++) if(taken.contains(start.plusMinutes(step*i))) free=false; if(free) break; start=start.plusMinutes(1); }
            return Jdbc.insert(c,"INSERT INTO work_schedule(doctor_id,work_date,shift,start_time,end_time,max_slots) VALUES(6,CURDATE(),?,?,?,?)","T"+UUID.randomUUID().toString().substring(0,12),start,end,capacity);
        });
    }
    @Test void completeHospitalJourneyAndBusinessGuards() {
        assertNotNull(new SearchService().searchDoctorsAndServices("",""));
        BigDecimal baseline=new AnalyticsService().generateReport("revenue",LocalDate.now().toString(),LocalDate.now().toString(),"CASH").stream().map(x->(BigDecimal)x.get("total_revenue")).reduce(BigDecimal.ZERO,BigDecimal::add);
        assertEquals("DOCTOR",new AuthService().login("doctor@hospital.test","Demo@1234").getRole());
        assertThrows(BusinessException.class,()->new AuthService().login("doctor@hospital.test","wrong"));
        long schedule=schedule(1),appt=new AppointmentService().createAppointment(8,schedule,"Demo symptoms");
        assertThrows(BusinessException.class,()->new AppointmentService().createAppointment(9,schedule,"Full schedule"));
        long queue=new QueueService().checkIn(reception,appt,"STANDARD");
        assertThrows(BusinessException.class,()->new QueueService().checkIn(reception,appt,"STANDARD"));
        assertEquals(queue,new QueueService().callNext(doctor));
        var emr=new EMRService(); long record=Jdbc.number(emr.getRecord(doctor,queue),"record_id");
        assertThrows(BusinessException.class,()->emr.getRecord(new AuthUser(7,"Other doctor","DOCTOR"),queue));
        assertThrows(BusinessException.class,()->emr.recordClinicalDiagnosis(doctor,queue,new DiagnosisRequest("Symptoms","INVALID","Diagnosis","")));
        emr.recordClinicalDiagnosis(doctor,queue,new DiagnosisRequest("Symptoms","J00","Demo diagnosis","Test notes"));
        new ClinicalService().createClinicalOrders(doctor,record,new long[]{2},"Demo test");
        long order=Jdbc.number(Jdbc.read("SELECT order_id FROM clinical_order WHERE record_id=?",record).get(0),"order_id");
        assertThrows(BusinessException.class,()->new ClinicalService().recordTestResult(lab,order,"Result","",""));
        assertThrows(BusinessException.class,()->emr.closeRecord(doctor,queue));
        long firstInvoice=new BillingService().createInvoice(cashier,record);
        assertEquals(firstInvoice,new BillingService().createInvoice(cashier,record));
        new BillingService().processCashPayment(cashier,firstInvoice);
        assertThrows(BusinessException.class,()->new BillingService().processCashPayment(cashier,firstInvoice));
        new ClinicalService().recordTestResult(lab,order,"Demo result","Demo conclusion","");
        assertThrows(BusinessException.class,()->new ClinicalService().recordTestResult(lab,order,"Changed","",""));
        long stock=Jdbc.number(Jdbc.read("SELECT stock_quantity FROM medication WHERE medication_id=1").get(0),"stock_quantity");
        assertThrows(BusinessException.class,()->new PrescriptionService().issuePrescription(doctor,record,List.of(new PrescriptionItem(1,2,"Demo"),new PrescriptionItem(2,1000,"Demo")),""));
        assertEquals(stock,Jdbc.number(Jdbc.read("SELECT stock_quantity FROM medication WHERE medication_id=1").get(0),"stock_quantity"),"Failed multi-item prescription must roll back stock changes");
        new PrescriptionService().issuePrescription(doctor,record,List.of(new PrescriptionItem(1,2,"Demo dosage")),"Demo advice");
        long secondInvoice=new BillingService().createInvoice(cashier,record); assertNotEquals(firstInvoice,secondInvoice); new BillingService().processCashPayment(cashier,secondInvoice);
        assertThrows(BusinessException.class,()->new BillingService().createInvoice(cashier,record));
        emr.closeRecord(doctor,queue);
        assertThrows(BusinessException.class,()->emr.recordClinicalDiagnosis(doctor,queue,new DiagnosisRequest("Changed","J00","Changed","")));
        new FeedbackService().submitFeedback(8,record,5,"Good demo");
        assertThrows(BusinessException.class,()->new FeedbackService().submitFeedback(8,record,5,"Duplicate"));
        assertThrows(BusinessException.class,()->new FeedbackService().submitFeedback(9,record,5,"Not my record"));
        var revenue=new AnalyticsService().generateReport("revenue",LocalDate.now().toString(),LocalDate.now().toString(),"CASH");
        BigDecimal total=revenue.stream().map(x->(BigDecimal)x.get("total_revenue")).reduce(BigDecimal.ZERO,BigDecimal::add); assertEquals(0,total.subtract(baseline).compareTo(new BigDecimal("242000.00")));
    }
    @Test void concurrentBookingDoesNotOversellLastPlace() throws Exception {
        long schedule=schedule(1); var executor=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try { List<Future<Boolean>> futures=new ArrayList<>(); for(long patient:new long[]{9,10}) futures.add(executor.submit(()-> { gate.await(); try { Database.tx(c->new AppointmentService().book(c,patient,schedule,"Concurrent test",false)); return true; } catch(BusinessException e) { return false; } })); gate.countDown(); int success=0; for(var f:futures) if(f.get(15,TimeUnit.SECONDS)) success++; assertEquals(1,success); assertEquals(1,Jdbc.number(Jdbc.read("SELECT booked_count FROM work_schedule WHERE schedule_id=?",schedule).get(0),"booked_count")); }
        finally { executor.shutdownNow(); }
    }
    @Test void concurrentBookingAllocatesDifferentTimesWhenTwoPlacesRemain() throws Exception {
        long schedule=schedule(2); var executor=Executors.newFixedThreadPool(2); var gate=new CountDownLatch(1);
        try { List<Future<Long>> futures=new ArrayList<>(); for(long patient:new long[]{9,10}) futures.add(executor.submit(()-> { gate.await(); return Database.tx(c->new AppointmentService().book(c,patient,schedule,"Concurrent distinct times",false)); })); gate.countDown(); for(var future:futures) assertTrue(future.get(15,TimeUnit.SECONDS)>0); var rows=Jdbc.read("SELECT COUNT(DISTINCT appointment_time) n FROM appointment WHERE schedule_id=?",schedule); assertEquals(2,Jdbc.number(rows.get(0),"n")); }
        finally { executor.shutdownNow(); }
    }
}
