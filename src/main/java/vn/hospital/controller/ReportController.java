package vn.hospital.controller;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.time.LocalDate;
import vn.hospital.service.*;
@WebServlet("/reports")
public class ReportController extends BaseController {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws ServletException,IOException {
        String from=p(r,"from").isBlank()?LocalDate.now().withDayOfMonth(1).toString():p(r,"from"),to=p(r,"to").isBlank()?LocalDate.now().toString():p(r,"to"),type=p(r,"type").isBlank()?"flow":p(r,"type");
        var rows=new AnalyticsService().generateReport(type,from,to,p(r,"method"));
        if(p(r,"format").equals("xlsx")) { s.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); s.setHeader("Content-Disposition","attachment; filename=\"hospital-"+type+".xlsx\""); new ReportExportService().exportExcel(rows,s.getOutputStream()); return; }
        if(p(r,"format").equals("pdf")) { s.setContentType("application/pdf"); s.setHeader("Content-Disposition","attachment; filename=\"hospital-"+type+".pdf\""); new ReportExportService().exportPDF("Hospital "+type+" report "+from+" - "+to,rows,s.getOutputStream()); return; }
        r.setAttribute("rows",rows); r.setAttribute("from",from); r.setAttribute("to",to); r.setAttribute("type",type); view(r,s,"reports");
    }
}
