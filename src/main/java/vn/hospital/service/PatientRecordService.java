package vn.hospital.service;

import java.util.*;
import vn.hospital.dao.*;
import vn.hospital.model.*;
import vn.hospital.util.*;

public class PatientRecordService {
  private final AuthUser actor;

  public PatientRecordService(AuthUser actor) {
    this.actor = actor;
  }

  public Map<String, Object> workspace(Integer id) {
    Actors.require(actor, "PATIENT");
    return Database.read(
        connection -> {
          var dao = new MedicalRecordDAO();
          Map<String, Object> page = new LinkedHashMap<>();
          page.put("records", dao.findPatientRecordSummaries(connection, actor.getUserId()));
          page.put("invoices", new InvoiceDAO().findByPatient(connection, actor.getUserId()));
          if (id != null) {
            MedicalRecord m = dao.findOwnedPatientRecord(connection, id, actor.getUserId());
            Checks.require(m != null, "Không được xem bệnh án này.");
            page.put("record", m);
            page.put("doctorName", dao.doctorName(connection, m.getDoctorId()));
            page.put("orders", new ClinicalOrderDAO().findByRecordId(connection, id));
            page.put("prescriptions", new PrescriptionDAO().findByRecordId(connection, id));
          }
          return page;
        });
  }
}
