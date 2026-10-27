package vn.hospital;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import vn.hospital.util.*;
import vn.hospital.integration.VNPayClient;
import vn.hospital.service.ReportExportService;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.PDDocument;
class CoreTest {
    @Test void saltedPasswordsAndWrongPassword() { String a=Passwords.hash("Demo@1234"),b=Passwords.hash("Demo@1234"); assertNotEquals(a,b); assertTrue(Passwords.matches("Demo@1234",a)); assertFalse(Passwords.matches("wrong",a)); assertFalse(Passwords.matches("x","broken")); }
    @Test void validatesMoneyAndDates() { assertEquals("125000",Checks.money("125000").toPlainString()); assertThrows(BusinessException.class,()->Checks.money("-1")); assertThrows(BusinessException.class,()->Checks.money("1.234")); assertThrows(BusinessException.class,()->Checks.date("2026-02-30")); }
    @Test void vnpayCanonicalIsOrderedAndExcludesSignature() { assertEquals("vnp_Amount=15000000&vnp_OrderInfo=Hospital+payment",VNPayClient.canonical(Map.of("vnp_OrderInfo","Hospital payment","vnp_Amount","15000000","vnp_SecureHash","ignore","ignored","x"))); }
    @Test void hmacMatchesPublishedTestVector() { String digest=VNPayClient.sign("The quick brown fox jumps over the lazy dog","key"); assertEquals("b42af09057bac1e2d41708e48a902e09b5ff7f12ab428a4fe86653c73dd248fb82f948a549f7b791a5b41915ee4d1ec3935357e4e2317250d0372afa2ebeeb3a",digest); }
    @Test void createsActualXlsxWithSafeTextCells() throws Exception { var out=new ByteArrayOutputStream(); new ReportExportService().exportExcel(List.of(Map.of("label","=HYPERLINK(\"bad\")","total",125000)),out); Set<String> paths=new HashSet<>(); try(var zip=new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) { ZipEntry e; while((e=zip.getNextEntry())!=null) { paths.add(e.getName()); String value=new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8); if(e.getName().contains("sheet1")) { assertTrue(value.contains("inlineStr")); assertFalse(value.contains("<f>")); } } } assertTrue(paths.contains("xl/workbook.xml")); assertTrue(paths.contains("xl/worksheets/sheet1.xml")); }
    @Test void createsPdfWithPageBreaks() throws Exception { var out=new ByteArrayOutputStream(); List<Map<String,Object>> data=new ArrayList<>(); for(int i=0;i<80;i++) data.add(Map.of("department","Nội tổng quát","total",i)); new ReportExportService().exportPDF("Hospital revenue",data,out); try(var pdf=PDDocument.load(out.toByteArray())) { assertTrue(pdf.getNumberOfPages()>1); } }
}
