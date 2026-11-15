(() => {
  document.querySelectorAll('.account-form').forEach(section => {
    const select=section.querySelector('[data-customer-type]');
    const org=section.querySelector('[data-organization]');
    if(select&&org){const refresh=()=>{const show=select.value==='TO_CHUC';org.hidden=!show;org.querySelector('input').required=show;};select.addEventListener('change',refresh);refresh();}
    section.querySelectorAll('[data-cccd-editor]').forEach(editor=>{
      const clear=()=>{editor.querySelector('input').value='';};
      editor.addEventListener('toggle',()=>{if(!editor.open)clear();});
      editor.querySelector('[data-cancel-cccd]').addEventListener('click',()=>{clear();editor.open=false;});
    });
    section.querySelectorAll('[data-otp-countdown]').forEach(el=>{
      const end=performance.now()+Math.max(0,Math.min(300,Number(el.dataset.remainingSeconds)))*1000;
      const tick=()=>{const n=Math.max(0,Math.ceil((end-performance.now())/1000));el.textContent=n?`${Math.floor(n/60)}:${String(n%60).padStart(2,'0')}`:'Mã không còn hiệu lực';};tick();setInterval(tick,1000);
    });
  });
})();
