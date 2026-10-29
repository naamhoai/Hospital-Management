document.addEventListener('submit', function(e) {
  const message = e.target.dataset.confirm;
  if (message && !window.confirm(message)) e.preventDefault();
});

// Thêm/xóa một dòng thuốc trên form; server vẫn kiểm tra lại trước khi lưu đơn.
document.addEventListener('click', function(e) {
  if (e.target.matches('[data-print]')) window.print();
  if (e.target.matches('[data-add-med]')) {
    const template = document.querySelector('#med-template');
    document.querySelector('#med-items').append(template.content.cloneNode(true));
  }
  if (e.target.matches('[data-remove-med]')) e.target.closest('.med-row').remove();
});

document.querySelectorAll('[data-bar]').forEach(function(el) {
  const value = Number(el.dataset.bar);
  el.style.width = Math.max(0, Math.min(100, value)) + '%';
});

document.querySelectorAll('nav a').forEach(function(a) {
  if (new URL(a.href).pathname === location.pathname) {
    a.classList.add('active');
    a.setAttribute('aria-current', 'page');
  }
});

document.querySelectorAll('.chart').forEach(function(chart) {
  const bars = chart.querySelectorAll('[data-chart-value]');
  const max = Math.max(1, ...Array.from(bars).map(b => Number(b.dataset.chartValue)));
  bars.forEach(function(b) {
    const value = Number(b.dataset.chartValue);
    b.style.width = (100 * value / max) + '%';
    b.setAttribute('title', String(value));
  });
});

// Chỉ lọc serviceId khi xem phí chỉ định, vì phí khám/thuốc không có serviceId này.
const serviceType = document.querySelector('#service-type');
const serviceFilter = document.querySelector('#service');
if (serviceType && serviceFilter) {
  const sync = function() {
    const allowed = !serviceType.value || serviceType.value === 'ORDER';
    serviceFilter.disabled = !allowed;
    if (!allowed) serviceFilter.value = '';
  };
  serviceType.addEventListener('change', sync);
  sync();
}


// Báo có kết quả mới cho bác sĩ; chỉ thêm thông báo, không tải lại form đang nhập.
const noticeBox = document.querySelector('#clinical-notices');
if (noticeBox) {
  let cursor = null;
  let lastActivity = Date.now();
  document.addEventListener('keydown', () => lastActivity = Date.now());
  document.addEventListener('pointerdown', () => lastActivity = Date.now());

  async function checkClinicalUpdates() {
    if (document.hidden || Date.now() - lastActivity > 10 * 60 * 1000) return;
    try {
      const url = noticeBox.dataset.url + (cursor === null ? '' : '?afterId=' + cursor);
      const response = await fetch(url, { headers: { Accept: 'application/json' } });
      if (!response.ok || !response.headers.get('Content-Type')?.includes('application/json')) return;
      const data = await response.json();
      cursor = data.cursor;
      for (const update of data.updates) {
        const line = document.createElement('p');
        const link = document.createElement('a');
        link.href = noticeBox.dataset.emrUrl + '?queueId=' + update.queueId;
        link.textContent = update.message + ' Mở bệnh án →';
        line.append(link);
        noticeBox.append(line);
        while (noticeBox.children.length > 5) noticeBox.firstElementChild.remove();
        noticeBox.hidden = false;
      }
    } catch (error) {
      // Mạng tạm lỗi không ảnh hưởng thao tác khám; lần kế tiếp sẽ kiểm tra lại.
    }
  }
  checkClinicalUpdates();
  window.setInterval(checkClinicalUpdates, 15000);
}
