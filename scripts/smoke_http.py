"""HTTP acceptance test against a fresh database populated with the synthetic seed.
Requires the app to be running. Mutates demo appointments/medical data only.
Run with: python scripts/smoke_http.py http://localhost:8080/hospital
"""
import sys, re, urllib.request, urllib.parse, urllib.error, http.cookiejar, datetime, uuid, base64
from html.parser import HTMLParser
from pathlib import Path

BASE=(sys.argv[1] if __name__=="__main__" and len(sys.argv)>1 else "http://127.0.0.1:3000/hospital").rstrip("/")
if urllib.parse.urlparse(BASE).hostname not in {'localhost','127.0.0.1','::1'}:raise SystemExit('This acceptance script is restricted to localhost synthetic data.')
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
    def multipart(self,path,fields,filename,content,status=302):
        boundary='HospitalBoundary'+uuid.uuid4().hex
        parts=[]
        for key,value in {'csrf':self.csrf,**fields}.items():
            parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="'+key+'"\r\n\r\n'+str(value)+'\r\n').encode())
        parts.append(('--'+boundary+'\r\nContent-Disposition: form-data; name="attachment"; filename="'+filename+'"\r\nContent-Type: image/png\r\n\r\n').encode()+content+b'\r\n')
        parts.append(('--'+boundary+'--\r\n').encode())
        req=urllib.request.Request(BASE+path,data=b''.join(parts),headers={'Content-Type':'multipart/form-data; boundary='+boundary})
        try:response=self.opener.open(req,timeout=30)
        except urllib.error.HTTPError as e:response=e
        body=response.read();assert response.code==status,(path,response.code,body[:1000])
        checks.append(f'PASS multipart POST {path}: {status}');return response,body
    def login(self,name):
        self.request("/login")
        self.request("/login",{"email":name+"@hospital.test","password":"Demo@1234"},302)
        self.request("/dashboard")

def field(text,name):
    match=re.search(r'name="'+name+r'" value="([^"]+)"',text)
    assert match,(name,text[:400]);return match[1]

def run():
    guest=Client();guest.request("/dashboard",status=302);guest.request("/search")
    patient=Client();patient.login("patient");patient.request("/reports",status=403)
    patient.request("/appointments",{"action":"book","csrf":"wrong"},403)
    _,text,_=patient.request("/appointments")
    # Prefer a same-day slot so check-in remains a faithful end-to-end workflow.
    today=datetime.date.today().isoformat()
    options=re.findall(r'<option value="(\d+)">([^<]+)',text)
    schedule=next((value for value,label in options if today in label and 'Bác sĩ An' in label),None)
    if schedule is None:
        # After-hours acceptance runs need a synthetic shift later today.
        now=datetime.datetime.now();start=(now+datetime.timedelta(minutes=2)).strftime('%H:%M')
        if start<'23:00':raise AssertionError('No seeded same-day slot; prepare a fresh synthetic database.')
        if start>='23:57':raise SystemExit('Run after midnight or earlier in the day; not enough time for same-day check-in.')
        setup=Client();setup.login('admin');setup.request('/schedules')
        setup.request('/schedules',{'action':'create','doctorId':'6','date':today,'shift':'QA'+uuid.uuid4().hex[:10],'start':start,'end':'23:59','capacity':'1'},302)
        _,text,_=patient.request('/appointments');options=re.findall(r'<option value="(\d+)">([^<]+)',text)
        schedule=next(value for value,label in options if today in label and 'Bác sĩ An' in label)
    patient.request("/appointments",{"action":"book","scheduleId":schedule,"symptoms":"HTTP demo symptoms"},302)
    _,text,_=patient.request("/appointments")
    appt=re.search(r'<select\b(?=[^>]*\bname="appointmentId")[^>]*>.*?<option value="(\d+)"',text,re.S).group(1)
    reception=Client();reception.login("reception");reception.request("/reception")
    reception.request("/reception",{"action":"checkin","appointmentId":appt,"priority":"STANDARD"},302)
    reception.request("/queue")
    doctor=Client();doctor.login("doctor");doctor.request("/schedules");doctor.request("/queue")
    response,_,_=doctor.request("/queue",{"action":"next"},302)
    emr_path=response.headers["Location"].split("/hospital",1)[1]
    _,text,_=doctor.request(emr_path)
    queue=field(text,"queueId");record=field(text,"recordId")
    doctor.request("/emr",{"action":"diagnosis","queueId":queue,"symptoms":"HTTP demo symptoms","icdCode":"J00","diagnosis":"HTTP demo diagnosis","notes":"Demo"},302)
    doctor.request("/clinical-orders",{"recordId":record,"queueId":queue,"serviceId":["2","3"],"instructions":"Demo"},302)
    _,text,_=doctor.request(emr_path)
    order_pdf=re.search(r'/clinical-orders\?id=(\d+)',text).group(1)
    _,_,data=doctor.request('/clinical-orders?id='+order_pdf);assert data.startswith(b'%PDF')
    doctor.request('/prescriptions',{'recordId':record,'medicationId':['1'],'quantity':['1'],'dosage':['Demo before results'],'advice':''},422)
    doctor.request(emr_path+'&medicationKeyword=DEMO')
    cashier=Client();cashier.login("cashier");cashier.request("/billing")
    response,_,_=cashier.request("/billing",{"action":"create","recordId":record},302)
    invoice_path=response.headers["Location"].split("/hospital",1)[1];_,text,_=cashier.request(invoice_path);invoice=field(text,"invoiceId")
    cashier.request("/billing",{"action":"cash","invoiceId":invoice},302)
    cashier.request(invoice_path)
    lab=Client();lab.login("lab");_,text,_=lab.request("/lab")
    order_ids=list(dict.fromkeys(re.findall(r'/lab\?orderId=(\d+)',text)))
    assert len(order_ids)>=2
    _,lab_detail,_=lab.request('/lab?orderId='+order_ids[0])
    assert 'Lưu &amp; duyệt' in lab_detail or 'Lưu & duyệt' in lab_detail
    png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=')
    lab.multipart('/lab',{'orderId':order_ids[0],'resultData':'Chỉ số thử nghiệm','conclusion':'Đã duyệt kết quả','fileUrl':''},'demo.png',png)
    _,lab_detail,_=lab.request('/lab?orderId='+order_ids[0])
    attachment=re.search(r'/attachments\?resultId=(\d+)',lab_detail).group(1)
    _,_,download=doctor.request('/attachments?resultId='+attachment);assert download==png
    patient2=Client();patient2.login('patient2');patient2.request('/attachments?resultId='+attachment,status=422)
    lab.request('/lab',{'orderId':order_ids[1],'resultData':'Demo values','conclusion':'Demo conclusion','fileUrl':''},302)
    lab.request('/lab',{'orderId':order_ids[1],'resultData':'Change approved result','conclusion':'','fileUrl':''},422)

    doctor.request(emr_path)
    response,_,_=doctor.request("/prescriptions",{"recordId":record,"medicationId":["1"],"quantity":["2"],"dosage":["Demo dosage"],"advice":"Demo"},302)
    rx_path=response.headers["Location"].split("/hospital",1)[1]
    doctor.request(rx_path)
    _,_,pdf=doctor.request(rx_path+'&format=pdf');assert pdf.startswith(b'%PDF')
    doctor.request("/emr",{"action":"close","queueId":queue},302)
    patient.request('/my-records?id='+record);patient.request(rx_path)
    patient2.request('/my-records?id='+record,status=422);patient2.request(rx_path,status=422)
    patient.request("/feedback");patient.request("/feedback",{"recordId":record,"rating":"5","comment":"<script>bad</script>"},302)
    manager=Client();manager.login("manager");manager.request("/services");manager.request("/reports")
    _,text,_=manager.request("/reports?type=revenue");assert "Doanh thu" in text and "Khám bệnh" in text
    manager.request('/reports?type=revenue&groupBy=MONTH&departmentId=1&serviceId=2&method=CASH')
    manager.request('/reports?type=revenue&groupBy=QUARTER&method=VNPAY')
    manager.request('/reports?type=revenue&serviceType=MEDICATION')
    manager.request('/reports?type=revenue&serviceType=ORDER')
    manager.request('/reports?from=2026-02-02&to=2026-01-01',status=422)
    _,_,data=manager.request("/reports?format=xlsx");assert data.startswith(b"PK")
    import io,zipfile,xml.etree.ElementTree as ET
    with zipfile.ZipFile(io.BytesIO(data)) as workbook:
        for entry in workbook.namelist():
            if entry.endswith('.xml') or entry.endswith('.rels'):ET.fromstring(workbook.read(entry))
        assert 'Thông tin báo cáo' in workbook.read('xl/workbook.xml').decode()
        assert 'Từ ngày' in workbook.read('xl/worksheets/sheet2.xml').decode()
    _,_,data=manager.request("/reports?format=pdf");assert data.startswith(b"%PDF")
    admin=Client();admin.login("admin");admin.request("/services");admin.request("/schedules")
    patient.request("/logout",{},302);patient.request("/appointments",status=302)
    print("\n".join(checks));print(f"\n{len(checks)} HTTP checks passed, including JSP compilation and full hospital workflow.")
    Path("target/http-smoke-results.txt").write_text("\n".join(checks)+f"\n{len(checks)} HTTP checks passed.\n",encoding="utf-8")

if __name__=="__main__":
    run()
