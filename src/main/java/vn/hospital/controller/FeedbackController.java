package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.FeedbackService;

@WebServlet("/feedback")
public class FeedbackController extends BaseController {
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute(
        "records", new FeedbackService().findEligibleFeedbackRecords(user(request).getUserId()));
    view(request, response, "feedback");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    new FeedbackService()
        .submitFeedback(
            user(request).getUserId(),
            id(request, "recordId"),
            (int) id(request, "rating"),
            param(request, "comment"));
    redirect(request, response, "/feedback", "Cảm ơn bạn đã đánh giá.");
  }
}
