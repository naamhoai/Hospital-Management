document.addEventListener('submit',function(e){const message=e.target.dataset.confirm;if(message&&!window.confirm(message))e.preventDefault();});
document.addEventListener('click',function(e){if(e.target.matches('[data-print]'))window.print();if(e.target.matches('[data-add-med]')){const template=document.querySelector('#med-template');document.querySelector('#med-items').append(template.content.cloneNode(true));}if(e.target.matches('[data-remove-med]'))e.target.closest('.med-row').remove();});
document.querySelectorAll('[data-bar]').forEach(function(el){const value=Number(el.dataset.bar);el.style.width=Math.max(0,Math.min(100,value))+'%';});

document.querySelectorAll('nav a').forEach(function(a){if(new URL(a.href).pathname===location.pathname){a.classList.add('active');a.setAttribute('aria-current','page');}});
document.querySelectorAll('.chart').forEach(function(chart){const bars=chart.querySelectorAll('[data-chart-value]');const max=Math.max(1,...Array.from(bars).map(b=>Number(b.dataset.chartValue)));bars.forEach(function(b){const value=Number(b.dataset.chartValue);b.style.width=(100*value/max)+'%';b.setAttribute('title',String(value));});});

const serviceType=document.querySelector('#service-type');const serviceFilter=document.querySelector('#service');if(serviceType&&serviceFilter){const sync=function(){const allowed=!serviceType.value||serviceType.value==='ORDER';serviceFilter.disabled=!allowed;if(!allowed)serviceFilter.value='';};serviceType.addEventListener('change',sync);sync();}
