package vn.hospital.service;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
public class ReportExportService {
    private static String xml(Object value) { return String.valueOf(value==null?"":value).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
    private static void entry(ZipOutputStream z,String name,String text) throws IOException { z.putNextEntry(new ZipEntry(name)); z.write(text.getBytes(StandardCharsets.UTF_8)); z.closeEntry(); }
    public void exportExcel(List<Map<String,Object>> rows,OutputStream out) throws IOException {
        List<String> headers=rows.isEmpty()?List.of("No data"):new ArrayList<>(rows.get(0).keySet());
        StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        List<List<?>> data=new ArrayList<>(); data.add(headers); for(var row:rows) data.add(new ArrayList<>(row.values()));
        int n=1; for(var row:data) { sheet.append("<row r=\"").append(n++).append("\">"); for(Object v:row) { if(v instanceof Number) sheet.append("<c><v>").append(v).append("</v></c>"); else sheet.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(v)).append("</t></is></c>"); } sheet.append("</row>"); } sheet.append("</sheetData></worksheet>");
        try(var zip=new ZipOutputStream(out)) {
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Hospital Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"); entry(zip,"xl/worksheets/sheet1.xml",sheet.toString());
        }
    }
    private static String ascii(String text) { return Normalizer.normalize(text.replace('đ','d').replace('Đ','D'),Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^\\x20-\\x7E]","?"); }
    public void exportPDF(String title,List<Map<String,Object>> rows,OutputStream out) throws IOException {
        List<String> lines=new ArrayList<>(); lines.add(title); lines.add("Generated: "+java.time.LocalDateTime.now()); lines.add("");
        for(var row:rows) { for(var field:row.entrySet()) { String line=ascii(field.getKey()+": "+field.getValue()); for(int i=0;i<line.length();i+=95) lines.add(line.substring(i,Math.min(i+95,line.length()))); } lines.add(""); }
        if(rows.isEmpty()) lines.add("No data for the selected date range.");
        try(var doc=new PDDocument()) { for(int i=0;i<lines.size();i+=48) { var page=new PDPage(); doc.addPage(page); try(var stream=new PDPageContentStream(doc,page)) { stream.beginText(); stream.setFont(PDType1Font.HELVETICA,10); stream.newLineAtOffset(40,750); stream.setLeading(14); for(int j=i;j<Math.min(i+48,lines.size());j++) { stream.showText(ascii(lines.get(j))); stream.newLine(); } stream.endText(); } } doc.save(out); }
    }
}
