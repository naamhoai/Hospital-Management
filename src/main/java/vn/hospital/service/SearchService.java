package vn.hospital.service;
import vn.hospital.dao.SearchDAO;
import vn.hospital.util.*;
import java.util.*;
public class SearchService {
    public Map<String,Object> searchDoctorsAndServices(String keyword,String date) {
        Checks.require(keyword.length()<=100,"Từ khóa tối đa 100 ký tự."); if(!date.isBlank()) Checks.date(date);
        var dao=new SearchDAO(); return Map.of("doctors",dao.doctors(keyword,date),"services",dao.services(keyword));
    }
}
