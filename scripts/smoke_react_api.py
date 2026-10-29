"""Acceptance tests for the React JSON contract. Localhost + synthetic seed only."""
import base64
import datetime
import http.cookiejar
import io
import json
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile
import xml.etree.ElementTree as ET

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://127.0.0.1:3000/hospital').rstrip('/')
if urllib.parse.urlparse(BASE).hostname not in {'localhost', '127.0.0.1', '::1'}:
    raise SystemExit('Use a localhost synthetic database only.')
checks = []


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args):
        return None


class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()), NoRedirect())
        self.csrf = ''

    def send(self, path, body=None, status=200, content_type=None, api=True):
        headers = {'Accept': 'application/json'} if api else {}
        if content_type:
            headers['Content-Type'] = content_type
        request = urllib.request.Request(BASE + ('/api' if api else '') + path, data=body, headers=headers)
        try:
            response = self.opener.open(request, timeout=30)
        except urllib.error.HTTPError as failure:
            response = failure
        payload = response.read()
        assert response.code == status, (path, response.code, json.loads(payload).get('error') if api else payload[:300])
        checks.append(f'PASS {request.get_method()} {"/api" if api else ""}{path}: {status}')
        if not api:
            return payload
        assert 'application/json' in response.headers.get('Content-Type', ''), (path, payload[:300])
        result = json.loads(payload)
        if result.get('csrf'):
            self.csrf = result['csrf']
        assert_no_secrets(result)
        return result

    def get(self, path, status=200):
        return self.send(path, status=status)

    def post(self, path, data=None, status=200):
        fields = {'csrf': self.csrf, **(data or {})}
        return self.send(path, urllib.parse.urlencode(fields, doseq=True).encode(), status,
                         'application/x-www-form-urlencoded')

    def multipart(self, path, fields, filename, content, field='attachment', content_type='image/png', status=200):
        boundary = 'HospitalReactBoundary' + uuid.uuid4().hex
        parts = []
        for key, value in {'csrf': self.csrf, **fields}.items():
            parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n').encode())
        parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"; filename="{filename}"\r\nContent-Type: {content_type}\r\n\r\n').encode() + content + b'\r\n')
        parts.append(f'--{boundary}--\r\n'.encode())
        return self.send(path, b''.join(parts), status, 'multipart/form-data; boundary=' + boundary)

    def download(self, path, status=200):
        return self.send(path, status=status, api=False)

    def login(self, name):
        self.get('/login')
        result = self.post('/login', {'email': name + '@hospital.test', 'password': 'Demo@1234'})
        assert result['redirect'].endswith('/dashboard')
        assert result['user'] and result['csrf']
        self.get('/dashboard')


def assert_no_secrets(value):
    if isinstance(value, dict):
        for key, item in value.items():
            assert key.lower().replace('_', '') not in {'password', 'passwordhash', 'hashsecret', 'apikey', 'checksumkey'}, key
            assert_no_secrets(item)
    elif isinstance(value, list):
        for item in value:
            assert_no_secrets(item)


def redirected_path(result):
    path = urllib.parse.urlsplit(result['redirect'])
    return path.path.split('/hospital', 1)[1] + ('?' + path.query if path.query else '')


def check_management_forms(admin, patient):
    """Exercise the same fields used by the schedule, appointment and catalog React forms."""
    marker = 'REACT' + uuid.uuid4().hex[:8]
    days = [(datetime.date.today() + datetime.timedelta(days=n)).isoformat() for n in (10, 11)]
    schedule_ids = []
    for day in days:
        admin.post('/schedules', {'action': 'create', 'doctorId': '6', 'date': day, 'shift': marker,
                                  'start': '18:00', 'end': '19:00', 'capacity': '3'})
        rows = admin.get('/schedules')['data']['schedules']
        schedule_ids.append(next(s['schedule_id'] for s in rows if s['shift'] == marker and s['work_date'] == day))
    admin.post('/schedules', {'action': 'capacity', 'scheduleId': schedule_ids[0], 'capacity': '4'})
    patient.post('/appointments', {'action': 'book', 'scheduleId': schedule_ids[0], 'symptoms': 'Management UI contract demo'})
    appointment = patient.get('/appointments')['data']['appointments'][0]['appointment_id']
    patient.post('/appointments', {'action': 'reschedule', 'appointmentId': appointment, 'scheduleId': schedule_ids[1]})
    updated = patient.get('/appointments')['data']['appointments'][0]
    assert updated['schedule_id'] == schedule_ids[1] and updated['reschedule_count'] == 1
    patient.post('/appointments', {'action': 'cancel', 'appointmentId': appointment})
    assert patient.get('/appointments')['data']['appointments'][0]['status'] == 'CANCELLED'
    admin.post('/schedules', {'action': 'leave', 'scheduleId': schedule_ids[1]})
    service_name = 'Dịch vụ kiểm thử ' + marker
    fields = {'serviceId': '', 'departmentId': '1', 'name': service_name, 'category': 'LAB', 'price': '12345', 'status': 'ACTIVE'}
    admin.post('/services', fields)
    services = admin.get('/services')['data']['services']
    service_id = next(s['service_id'] for s in services if s['name'] == service_name)
    admin.post('/services', {**fields, 'serviceId': service_id, 'price': '15000', 'status': 'INACTIVE'})
    saved = next(s for s in admin.get('/services')['data']['services'] if s['service_id'] == service_id)
    assert saved['unit_price'] == 15000 and saved['status'] == 'INACTIVE'


def run():
    guest = Client()
    guest.get('/dashboard', 401)
    search = guest.get('/search')
    assert search['page'] == 'search' and isinstance(search['data']['services'], list)
    shell = guest.download('/login').decode()
    assert 'id="root"' in shell and '/hospital/assets/react/' in shell and '__APP_CONTEXT__' not in shell
    payment_shell = guest.download('/payment/return?vnp_ResponseCode=24').decode()
    assert 'id="root"' in payment_shell
    guest.get('/payment/return?vnp_ResponseCode=24', 422)
    ipn = json.loads(guest.download('/payment/ipn?vnp_ResponseCode=24'))
    assert ipn['RspCode'] == '99'
    patient = Client()
    patient.login('patient')
    patient.get('/reports', 403)
    patient.post('/appointments', {'action': 'book', 'csrf': 'wrong'}, 403)
    data = patient.get('/appointments')['data']
    today = datetime.date.today().isoformat()
    schedules = [s for s in data['schedules'] if s['work_date'] == today and 'Bác sĩ An' in s['doctor_name']]
    assert schedules, 'Prepare a fresh synthetic database with a future same-day shift.'
    schedule = schedules[0]['schedule_id']
    patient.post('/appointments', {'action': 'book', 'scheduleId': schedule, 'symptoms': 'React HTTP demo symptoms'})
    appointment = patient.get('/appointments')['data']['appointments'][0]
    reception = Client()
    reception.login('reception')
    reception.get('/reception')
    reception.post('/reception', {'action': 'checkin', 'appointmentId': appointment['appointment_id'], 'priority': 'STANDARD'})
    reception.post('/reception', {'action': 'checkin', 'appointmentId': appointment['appointment_id'], 'priority': 'STANDARD'}, 422)
    reception.get('/queue')
    doctor = Client()
    doctor.login('doctor')
    doctor.get('/schedules')
    doctor.get('/queue')
    emr_path = redirected_path(doctor.post('/queue', {'action': 'next'}))
    workspace = doctor.get(emr_path)['data']
    queue = workspace['queueContext']['queueId']
    record = workspace['record']['recordId']
    assert workspace['icds'] and workspace['clinicalProgress'] == 'NOT_DIAGNOSED'
    invalid = doctor.post('/emr', {'action': 'diagnosis', 'queueId': queue, 'symptoms': 'Keep entered text', 'icdCode': 'INVALID', 'diagnosis': 'Demo'}, 422)
    assert invalid['error'] and invalid['page'] == 'emr'
    doctor.post('/emr', {'action': 'diagnosis', 'queueId': queue, 'symptoms': 'React HTTP symptoms', 'icdCode': 'J00', 'diagnosis': 'React HTTP diagnosis', 'notes': 'Demo'})
    doctor.post('/clinical-orders', {'recordId': record, 'queueId': queue, 'serviceId': ['2', '3'], 'instructions': 'Demo'})
    doctor.post('/prescriptions', {'recordId': record, 'medicationId': ['1'], 'quantity': ['1'], 'dosage': ['Demo before results']}, 422)
    doctor.post('/clinical-orders', {'recordId': record, 'queueId': queue, 'serviceId': ['4'], 'instructions': 'Temporary order'})
    orders = doctor.get(emr_path)['data']['orders']
    temporary = next(o for o in orders if o['serviceId'] == 4)
    active = [o for o in orders if o['serviceId'] != 4]
    assert doctor.download('/clinical-orders?id=' + str(active[0]['orderId'])).startswith(b'%PDF')
    cashier = Client()
    cashier.login('cashier')
    invoice_path = redirected_path(cashier.post('/billing', {'action': 'create', 'recordId': record}))
    invoice = cashier.get(invoice_path)['data']['invoice']
    doctor.post('/clinical-orders', {'action': 'cancel', 'queueId': queue, 'orderId': temporary['orderId']})
    assert cashier.get(invoice_path)['data']['invoice']['total_amount'] == 360000
    lab = Client()
    lab.login('lab')
    lab.post('/lab', {'orderId': active[0]['orderId'], 'resultData': 'Cannot approve unpaid'}, 422)
    cashier.post('/billing', {'action': 'cash', 'invoiceId': invoice['invoice_id']})
    cashier.post('/billing', {'action': 'cash', 'invoiceId': invoice['invoice_id']}, 422)
    assert cashier.get(invoice_path)['data']['invoice']['status'] == 'PAID'
    lab.get('/lab')
    png = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=')
    order_id = active[0]['orderId']
    lab.multipart('/lab', {'orderId': order_id, 'resultData': 'Chỉ số thử nghiệm React', 'conclusion': 'Đã duyệt kết quả', 'fileUrl': ''}, 'demo.png', png)
    result_id = lab.get('/lab?orderId=' + str(order_id))['data']['selectedOrder']['result']['resultId']
    assert doctor.download('/attachments?resultId=' + str(result_id)) == png
    patient2 = Client()
    patient2.login('patient2')
    patient2.download('/attachments?resultId=' + str(result_id), 422)
    lab.post('/lab', {'orderId': active[1]['orderId'], 'resultData': 'Demo values', 'conclusion': 'Demo conclusion'})
    lab.post('/lab', {'orderId': order_id, 'resultData': 'Edit approved result'}, 422)
    rx_path = redirected_path(doctor.post('/prescriptions', {'recordId': record, 'medicationId': ['1', '2'], 'quantity': ['2', '1'], 'dosage': ['Demo dose 1', 'Demo dose 2'], 'advice': 'Demo'}))
    assert len(doctor.get(rx_path)['data']['prescription']['items']) == 2
    assert doctor.download(rx_path + '&format=pdf').startswith(b'%PDF')
    second_invoice_path = redirected_path(cashier.post('/billing', {'action': 'create', 'recordId': record}))
    second = cashier.get(second_invoice_path)['data']
    assert all(line['source_type'] == 'MEDICATION' for line in second['lines'])
    cashier.post('/billing', {'action': 'cash', 'invoiceId': second['invoice']['invoice_id']})
    doctor.post('/emr', {'action': 'close', 'queueId': queue})
    patient.get('/my-records?id=' + str(record))
    patient.get(rx_path)
    patient2.get('/my-records?id=' + str(record), 422)
    patient2.get(rx_path, 422)
    patient.get('/feedback')
    patient.post('/feedback', {'recordId': record, 'rating': '5', 'comment': '<script>display as text</script>'})
    patient.post('/feedback', {'recordId': record, 'rating': '5', 'comment': 'Duplicate'}, 422)
    manager = Client()
    manager.login('manager')
    manager.get('/services')
    manager.get('/reports')
    revenue = manager.get('/reports?type=revenue')['data']['report']
    assert revenue['paidInvoiceCount'] == 2
    for query in ['groupBy=MONTH&departmentId=1&serviceId=2&method=CASH', 'groupBy=QUARTER&method=VNPAY', 'serviceType=MEDICATION', 'serviceType=ORDER']:
        manager.get('/reports?type=revenue&' + query)
    manager.get('/reports?from=2026-02-02&to=2026-01-01', 422)
    workbook = manager.download('/reports?format=xlsx')
    with zipfile.ZipFile(io.BytesIO(workbook)) as archive:
        for entry in archive.namelist():
            if entry.endswith('.xml') or entry.endswith('.rels'):
                ET.fromstring(archive.read(entry))
        assert 'Thông tin báo cáo' in archive.read('xl/workbook.xml').decode()
    assert manager.download('/reports?format=pdf').startswith(b'%PDF')
    admin = Client()
    admin.login('admin')
    admin.get('/services')
    admin.get('/schedules')
    check_management_forms(admin, patient2)
    patient.post('/logout')
    patient.get('/appointments', 401)
    output = '\n'.join(checks) + f'\n{len(checks)} React API HTTP checks passed.\n'
    print(output)
    Path('target/http-smoke-results.txt').write_text(output, encoding='utf-8')
    Path('target/react-http-fixture.json').write_text(json.dumps({'record': record, 'order': order_id, 'result': result_id, 'queue': queue}), encoding='utf-8')


def run_recall():
    fixture = json.loads(Path('target/react-http-fixture.json').read_text())
    order, result = fixture['order'], fixture['result']
    doctor, lab, head, patient = Client(), Client(), Client(), Client()
    for client, name in [(doctor, 'doctor'), (lab, 'lab'), (head, 'head'), (patient, 'patient')]:
        client.login(name)
    head.get('/lab', 403)
    lab.get('/result-recalls', 422)
    cursor = doctor.get('/clinical-updates')['cursor']
    original = doctor.download('/attachments?resultId=' + str(result))
    lab.post('/result-recalls', {'action': 'request', 'resultId': result, 'orderId': order, 'reason': 'Kiểm tra lại - dữ liệu React demo'})
    requests = head.get('/result-recalls')['data']['requests']
    request_id = next(r['requestId'] for r in requests if r['status'] == 'PENDING' and r['resultId'] == result)
    pdf = doctor.download('/clinical-orders?id=' + str(order))
    head.multipart('/result-recalls', {'action': 'approve', 'requestId': request_id, 'reviewNote': 'Biên bản thử nghiệm React'}, 'demo-proof.pdf', pdf, field='signedReport', content_type='application/pdf')
    head.get('/result-recalls')
    for _ in range(2):
        assert head.download('/result-recalls?fileRequestId=' + str(request_id)) == pdf
        assert head.download('/result-recalls?originalRequestId=' + str(request_id)) == original
    patient.download('/attachments?resultId=' + str(result), 422)
    doctor.download('/attachments?resultId=' + str(result), 422)
    updates = doctor.get('/clinical-updates?afterId=' + str(cursor))
    assert any('thu hồi' in u['message'] for u in updates['updates'])
    lab.get('/lab?orderId=' + str(order))
    lab.post('/lab', {'orderId': order, 'resultData': 'Kết quả thay thế React demo', 'conclusion': 'Đã kiểm tra', 'fileUrl': ''})
    new_data = lab.get('/lab?orderId=' + str(order))['data']
    assert new_data['selectedOrder']['result']['resultId'] != result and new_data['recallHistory']
    worklist = lab.get('/lab')['data']['orders']
    assert len(worklist) == len({item['order_id'] for item in worklist}), 'Recalled result history must not duplicate lab orders.'
    new_updates = doctor.get('/clinical-updates?afterId=' + str(updates['cursor']))
    assert any('kết quả mới' in u['message'] for u in new_updates['updates'])
    patient.get('/clinical-updates', 403)
    output = '\n'.join(checks) + f'\n{len(checks)} React recall HTTP checks passed.\n'
    print(output)
    Path('target/http-nam-workflows-results.txt').write_text(output, encoding='utf-8')


if __name__ == '__main__':
    run()
