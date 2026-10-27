package vn.hospital.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import vn.hospital.model.*;
import vn.hospital.util.BusinessException;

public class ReportExportService {
  private static String xml(Object value) {
    return String.valueOf(value == null ? "" : value)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static void entry(ZipOutputStream z, String name, String text) throws IOException {
    z.putNextEntry(new ZipEntry(name));
    z.write(text.getBytes(StandardCharsets.UTF_8));
    z.closeEntry();
  }

  public void exportExcel(List<Map<String, Object>> rows, OutputStream out) throws IOException {
    writeExcel(rows, List.of(), out);
  }

  private void writeExcel(
      List<Map<String, Object>> rows, List<Map<String, Object>> metadata, OutputStream out)
      throws IOException {
    List<String> headers =
        rows.isEmpty() ? List.of("Không có dữ liệu") : new ArrayList<>(rows.get(0).keySet());
    StringBuilder sheet =
        new StringBuilder(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet"
                + " xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView"
                + " workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\""
                + " state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"20\""
                + " width=\"25\" customWidth=\"1\"/></cols><sheetData>");
    List<List<?>> data = new ArrayList<>();
    data.add(headers);
    for (var row : rows) data.add(new ArrayList<>(row.values()));
    int n = 1;
    for (var row : data) {
      sheet.append("<row r=\"").append(n++).append("\">");
      for (Object v : row) {
        if (v instanceof Number) sheet.append("<c><v>").append(v).append("</v></c>");
        else
          sheet
              .append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">")
              .append(xml(v))
              .append("</t></is></c>");
      }
      sheet.append("</row>");
    }
    sheet.append("</sheetData></worksheet>");
    try (var zip = new ZipOutputStream(out)) {
      entry(
          zip,
          "[Content_Types].xml",
          "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default"
              + " Extension=\"rels\""
              + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default"
              + " Extension=\"xml\" ContentType=\"application/xml\"/><Override"
              + " PartName=\"/xl/workbook.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override"
              + " PartName=\"/xl/worksheets/sheet1.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override"
              + " PartName=\"/xl/worksheets/sheet2.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      entry(
          zip,
          "_rels/.rels",
          "<Relationships"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship"
              + " Id=\"rId1\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\""
              + " Target=\"xl/workbook.xml\"/></Relationships>");
      entry(
          zip,
          "xl/workbook.xml",
          "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
              + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet"
              + " name=\"Hospital Report\" sheetId=\"1\" r:id=\"rId1\"/><sheet name=\"Thông tin báo"
              + " cáo\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>");
      entry(
          zip,
          "xl/_rels/workbook.xml.rels",
          "<Relationships"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship"
              + " Id=\"rId1\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
              + " Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\""
              + " Target=\"worksheets/sheet2.xml\"/></Relationships>");
      entry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
      StringBuilder info =
          new StringBuilder(
              "<worksheet"
                  + " xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols><col"
                  + " min=\"1\" max=\"2\" width=\"40\" customWidth=\"1\"/></cols><sheetData>");
      int index = 1;
      for (var row : metadata) {
        for (var field : row.entrySet()) {
          info.append("<row r=\"")
              .append(index++)
              .append("\"><c t=\"inlineStr\"><is><t>")
              .append(xml(field.getKey()))
              .append("</t></is></c><c t=\"inlineStr\"><is><t>")
              .append(xml(field.getValue()))
              .append("</t></is></c></row>");
        }
      }
      info.append("</sheetData></worksheet>");
      entry(zip, "xl/worksheets/sheet2.xml", info.toString());
    }
  }

  public File exportPDF(ReportData report) {
    return createPDF(
        report.getTitle() + " (" + report.getFromDate() + " — " + report.getToDate() + ")",
        exportRows(report));
  }

  public File exportExcel(ReportData report) {
    try {
      File f = temp(".xlsx");
      try (var out = new FileOutputStream(f)) {
        writeExcel(report.tableRows(), metadata(report), out);
      }
      return f;
    } catch (IOException e) {
      throw new BusinessException("Không xuất được Excel.");
    }
  }

  private List<Map<String, Object>> metadata(ReportData report) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("Báo cáo", report.getTitle());
    fields.put("Từ ngày", report.getFromDate());
    fields.put("Đến ngày", report.getToDate());
    fields.put("Ngày tạo", report.getGeneratedAt());
    var f = report.getFilter();
    if (f != null) {
      fields.put("Nhóm theo", f.getGroupBy());
      fields.put("Loại chi phí", f.getServiceType().isBlank() ? "Tất cả" : f.getServiceType());
      fields.put("Mã chuyên khoa", f.getDepartmentId() == null ? "Tất cả" : f.getDepartmentId());
      fields.put("Mã dịch vụ chỉ định", f.getServiceId() == null ? "Tất cả" : f.getServiceId());
      fields.put("Thanh toán", f.getPaymentMethod().isBlank() ? "Tất cả" : f.getPaymentMethod());
    }
    if (report instanceof RevenueReport r) {
      fields.put("Tổng doanh thu (VND)", r.getTotalRevenue());
      fields.put("Hóa đơn đã trả", r.getPaidInvoiceCount());
    }
    if (report instanceof PatientFlowReport r) {
      fields.put("Tổng lượt hẹn", r.getPatientCount());
      fields.put("Hoàn tất (%)", r.getCompletionRate());
      fields.put("Hủy (%)", r.getCancellationRate());
      fields.put("Vắng mặt (%)", r.getNoShowRate());
    }
    return List.of(fields);
  }

  private List<Map<String, Object>> exportRows(ReportData report) {
    var rows = new ArrayList<Map<String, Object>>(metadata(report));
    if (report.tableRows().isEmpty())
      rows.add(Map.of("Dữ liệu", "Không có dữ liệu trong bộ lọc này."));
    else rows.addAll(report.tableRows());
    return rows;
  }

  private File temp(String extension) throws IOException {
    Path dir = Path.of(System.getProperty("java.io.tmpdir"), "hospital-exports");
    Files.createDirectories(dir);
    return Files.createTempFile(dir, "hospital-", extension).toFile();
  }

  public File createPDF(String title, List<Map<String, Object>> rows) {
    try {
      File f = temp(".pdf");
      try (var out = new FileOutputStream(f)) {
        exportPDF(title, rows, out);
      }
      return f;
    } catch (IOException e) {
      throw new BusinessException("Không xuất được PDF.");
    }
  }

  public void exportPDF(String title, List<Map<String, Object>> rows, OutputStream out)
      throws IOException {
    try (var doc = new PDDocument();
        var resource =
            ReportExportService.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
      if (resource == null) throw new IOException("Missing embedded font");
      var font = PDType0Font.load(doc, resource, true);
      List<String> lines = new ArrayList<>();
      lines.add(title);
      lines.add("Ngày xuất: " + java.time.LocalDateTime.now().withNano(0));
      lines.add("");
      if (rows.isEmpty()) lines.add("Không có dữ liệu trong khoảng thời gian đã chọn.");
      for (var row : rows) {
        for (var field : row.entrySet()) {
          String text = field.getKey() + ": " + Objects.toString(field.getValue(), "");
          StringBuilder line = new StringBuilder();
          for (String word : text.replaceAll("[\\p{Cntrl}]", " ").split(" ")) {
            if (font.getStringWidth(line + word) / 1000 * 10 > 500 && line.length() > 0) {
              lines.add(line.toString());
              line.setLength(0);
            }
            while (font.getStringWidth(word) / 1000 * 10 > 500) {
              int end = Math.min(65, word.length());
              lines.add(word.substring(0, end));
              word = word.substring(end);
            }
            line.append(word).append(' ');
          }
          lines.add(line.toString());
        }
        lines.add("");
      }
      for (int start = 0; start < lines.size(); start += 43) {
        var page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (var stream = new PDPageContentStream(doc, page)) {
          stream.beginText();
          stream.setFont(font, 10);
          stream.newLineAtOffset(45, 790);
          stream.setLeading(17);
          for (int j = start; j < Math.min(start + 43, lines.size()); j++) {
            stream.showText(lines.get(j));
            stream.newLine();
          }
          stream.endText();
          stream.beginText();
          stream.setFont(font, 9);
          stream.newLineAtOffset(45, 30);
          stream.showText("Hospital • Trang " + doc.getNumberOfPages());
          stream.endText();
        }
      }
      doc.save(out);
    }
  }
}
