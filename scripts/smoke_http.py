"""HTTP acceptance test against a fresh database populated with the synthetic seed.
Requires the app to be running. Mutates demo appointments/medical data only.
Run with: python scripts/smoke_http.py http://localhost:8080/hospital
"""
import sys, re, urllib.request, urllib.parse, urllib.error, http.cookiejar
from html.parser import HTMLParser
from pathlib import Path

BASE=(sys.argv[1] if len(sys.argv)>1 else "http://localhost:8080/hospital").rstrip("/")
checks=[]
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args): return None
class Client:
    def __init__(self):
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()),NoRedirect())
        self.csrf=""
    def request(self,path,data=None,status=200):
        if data is not None: data={"csrf":self.csrf,**data}
        req=urllib.request.Request(BASE+path,data=None if data is None else urllib.parse.urlencode(data,doseq=True).encode())
        try: response=self.opener.open(req,timeout=30)
        except urllib.error.HTTPError as e: response=e
        body=response.read(); text=body.decode("utf-8",errors="replace")
        assert response.code==status,(path,response.code,text[:1000])
        match=re.search(r'name="csrf" value="([^"]+)"',text)
        if match:self.csrf=match[1]
        checks.append(f"PASS {req.get_method()} {path}: {status}")
        return response,text,body
    def login(self,name):
        self.request("/login")
        self.request("/login",{"email":name+"@hospital.test","password":"Demo@1234"},302)
        self.request("/dashboard")

def field(text,name):
    match=re.search(r'name="'+name+r'" value="([^"]+)"',text)
    assert match,(name,text[:400]);return match[1]

guest=Client();guest.request("/dashboard",status=302);guest.request("/search")
patient=Client();patient.login("patient");patient.request("/reports",status=403)
patient.request("/appointments",{"action":"book","csrf":"wrong"},403)
_,text,_=patient.request("/appointments")
schedule=re.search(r'<option value="(\d+)">#\d+',text).group(1)
patient.request("/appointments",{"action":"book","scheduleId":schedule,"symptoms":"HTTP demo symptoms"},302)
_,text,_=patient.request("/appointments")
appt=re.search(r'<select name="appointmentId"[^>]*>.*?<option value="(\d+)"',text,re.S).group(1)
reception=Client();reception.login("reception");reception.request("/reception")
reception.request("/reception",{"action":"checkin","appointmentId":appt,"priority":"STANDARD"},302)
reception.request("/queue")
doctor=Client();doctor.login("doctor");doctor.request("/schedules");doctor.request("/queue")
response,_,_=doctor.request("/queue",{"action":"next"},302)
emr_path=response.headers["Location"].split("/hospital",1)[1]
_,text,_=doctor.request(emr_path)
queue=field(text,"queueId");record=field(text,"recordId")
doctor.request("/emr",{"action":"diagnosis","queueId":queue,"symptoms":"HTTP demo symptoms","icdCode":"J00","diagnosis":"HTTP demo diagnosis","notes":"Demo"},302)
doctor.request("/clinical-orders",{"recordId":record,"queueId":queue,"serviceId":"2","instructions":"Demo"},302)
cashier=Client();cashier.login("cashier");cashier.request("/billing")
response,_,_=cashier.request("/billing",{"action":"create","recordId":record},302)
invoice_path=response.headers["Location"].split("/hospital",1)[1];_,text,_=cashier.request(invoice_path);invoice=field(text,"invoiceId")
cashier.request("/billing",{"action":"cash","invoiceId":invoice},302)
cashier.request(invoice_path)
lab=Client();lab.login("lab");_,text,_=lab.request("/lab")
order=re.search(r'<select name="orderId"[^>]*>.*?<option value="(\d+)"',text,re.S).group(1)
lab.request("/lab",{"orderId":order,"resultData":"Demo values","conclusion":"Demo conclusion","fileUrl":""},302)
doctor.request(emr_path)
response,_,_=doctor.request("/prescriptions",{"recordId":record,"medicationId":["1"],"quantity":["2"],"dosage":["Demo dosage"],"advice":"Demo"},302)
doctor.request(response.headers["Location"].split("/hospital",1)[1])
doctor.request("/emr",{"action":"close","queueId":queue},302)
patient.request("/feedback");patient.request("/feedback",{"recordId":record,"rating":"5","comment":"<script>bad</script>"},302)
manager=Client();manager.login("manager");manager.request("/services");manager.request("/reports")
_,text,_=manager.request("/reports?type=revenue");assert "CONSULTATION" in text and "ORDER" in text
_,_,data=manager.request("/reports?format=xlsx");assert data.startswith(b"PK")
_,_,data=manager.request("/reports?format=pdf");assert data.startswith(b"%PDF")
admin=Client();admin.login("admin");admin.request("/services");admin.request("/schedules")
patient.request("/logout",{},302);patient.request("/appointments",status=302)
print("\n".join(checks));print(f"\n{len(checks)} HTTP checks passed, including JSP compilation and full hospital workflow.")
Path("target/http-smoke-results.txt").write_text("\n".join(checks)+f"\n{len(checks)} HTTP checks passed.\n",encoding="utf-8")
