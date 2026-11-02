package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.SearchService;

@WebServlet("/search")
public class SearchController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    new SearchService()
        .searchDoctorsAndServices(param(request, "keyword"), param(request, "date"))
        .forEach(request::setAttribute);
    view(request, response, "search");
  }
}
