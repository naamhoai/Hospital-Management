"""Run after smoke_http.py against the same localhost-only synthetic demo database."""
import json
import re
import sys
from pathlib import Path
import smoke_http as smoke

if len(sys.argv)>1:
    smoke.BASE=sys.argv[1].rstrip('/')
if smoke.urllib.parse.urlparse(smoke.BASE).hostname not in {'localhost','127.0.0.1','::1'}:
    raise SystemExit('Only localhost synthetic databases are allowed.')

doctor=smoke.Client();doctor.login('doctor')
lab=smoke.Client();lab.login('lab')
head=smoke.Client();head.login('head')
patient=smoke.Client();patient.login('patient')
head.request('/lab',status=403)
lab.request('/result-recalls',status=422)
_,payload,_=doctor.request('/clinical-updates');cursor=json.loads(payload)['cursor']
_,page,_=lab.request('/lab')
order=None
result=None
for candidate in dict.fromkeys(re.findall(r'/lab\?orderId=(\d+)',page)):
    _,detail,_=lab.request('/lab?orderId='+candidate)
    attachment=re.search(r'/attachments\?resultId=(\d+)',detail)
    if attachment:
        order=candidate
        result=attachment.group(1)
        break
assert order and result,'Run smoke_http.py first to create an approved result with an attachment.'
_,_,original=doctor.request('/attachments?resultId='+result)
lab.request('/result-recalls',{'action':'request','resultId':result,'orderId':order,'reason':'Đề nghị kiểm tra lại - dữ liệu thử nghiệm'},302)
_,requests,_=head.request('/result-recalls')
request=smoke.field(requests,'requestId')
_,_,pdf=doctor.request('/clinical-orders?id='+order)
head.multipart('/result-recalls',{'action':'approve','requestId':request,'reviewNote':'Đã đối chiếu biên bản thử nghiệm'},'signed-demo.pdf',pdf,file_field='signedReport',content_type='application/pdf')
head.request('/result-recalls')
# Reading a permanent approval/original twice must not delete either stored file.
for _ in range(2):
    _,_,proof=head.request('/result-recalls?fileRequestId='+request);assert proof==pdf
    _,_,old=head.request('/result-recalls?originalRequestId='+request);assert old==original

doctor.request('/attachments?resultId='+result,status=422)
patient.request('/attachments?resultId='+result,status=422)
_,payload,_=doctor.request('/clinical-updates?afterId='+str(cursor))
updates=json.loads(payload);assert any('thu hồi' in item['message'] for item in updates['updates'])
cursor=updates['cursor']
_,detail,_=lab.request('/lab?orderId='+order)
assert 'Kết quả cũ đã được thu hồi' in detail
lab.request('/lab',{'orderId':order,'resultData':'Kết quả thay thế đã kiểm tra - Demo','conclusion':'Kết luận thay thế - Demo','fileUrl':''},302)
_,detail,_=lab.request('/lab?orderId='+order)
assert 'Kết quả thay thế đã kiểm tra' in detail and 'Lịch sử đề nghị thu hồi' in detail
_,payload,_=doctor.request('/clinical-updates?afterId='+str(cursor))
assert any('kết quả mới' in item['message'] for item in json.loads(payload)['updates'])
patient.request('/clinical-updates',status=403)
text='\n'.join(smoke.checks)+f'\n{len(smoke.checks)} Nam recall/notification HTTP checks passed.\n'
print(text)
Path('target/http-nam-workflows-results.txt').write_text(text,encoding='utf-8')
