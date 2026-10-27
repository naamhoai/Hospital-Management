package vn.hospital.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import vn.hospital.service.*;
import vn.hospital.service.FeedbackService;

@WebServlet("/feedback")
public class FeedbackController extends BaseController {
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.setAttribute(
        "records", new FeedbackService().findEligibleFeedbackRecords(user(r).getUserId()));
    view(r, s, "feedback");
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    new FeedbackService()
        .submitFeedback(
            user(r).getUserId(), id(r, "recordId"), (int) id(r, "rating"), p(r, "comment"));
    redirect(r, s, "/feedback", "Cảm ơn bạn đã đánh giá.");
  }
}
