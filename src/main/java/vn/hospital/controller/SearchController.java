package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.SearchService;

@WebServlet("/search")
public class SearchController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    new SearchService()
        .searchDoctorsAndServices(p(r, "keyword"), p(r, "date"))
        .forEach(r::setAttribute);
    view(r, s, "search");
  }
}
