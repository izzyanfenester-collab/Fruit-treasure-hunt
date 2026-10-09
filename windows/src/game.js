(() => {
  const $ = s => document.querySelector(s);
  const screens = [...document.querySelectorAll('.screen')];
  const modal = $('#modal');
  const storage = localStorage;
  let sound = storage.getItem('sound') !== 'off';
  let selected = 'GREEN';
  let loopId = 0, lastTime = 0, state = null, paused = false;
  const keys = new Set();

  const baskets = {
    GREEN:{name:'HIJAU',color:'#43a047',bonus:'Epal hijau'},
    RED:{name:'MERAH',color:'#d93636',bonus:'Epal merah'},
    PURPLE:{name:'UNGU',color:'#8525a6',bonus:'Anggur'},
    ORANGE:{name:'OREN',color:'#e57500',bonus:'Oren'}
  };
  const fruits = [
    {name:'Epal hijau',color:'#57b64a',bonus:'GREEN'},
    {name:'Epal merah',color:'#e43b3b',bonus:'RED'},
    {name:'Oren',color:'#f28c18',bonus:'ORANGE'},
    {name:'Anggur',color:'#8d4bb6',bonus:'PURPLE'},
    {name:'Mangga',color:'#fbc02d'},
    {name:'Pir',color:'#99bd40'},
    {name:'Strawberi',color:'#f34b65'},
    {name:'Pisang',color:'#ffd54f'}
  ];
  const wrongs = ['BOTOL','MAINAN','TIN','KOTAK','KASUT'];

  function show(id){ screens.forEach(x=>x.classList.add('hidden')); $(id).classList.remove('hidden'); }
  function menu(){
    cancelAnimationFrame(loopId);
    const video=$('#storyVideo'); video.pause();
    show('#menuScreen');
    $('#soundBtn').textContent=sound?'BUNYI ON':'BUNYI OFF';
  }
  function beep(freq=500,dur=.09){
    if(!sound) return;
    try{
      const Ctx=window.AudioContext||window.webkitAudioContext, ctx=new Ctx(), o=ctx.createOscillator(), g=ctx.createGain();
      o.frequency.value=freq;o.connect(g);g.connect(ctx.destination);g.gain.value=.055;o.start();o.stop(ctx.currentTime+dur);o.onended=()=>ctx.close();
    }catch{}
  }
  function openModal(title,body,buttons){
    $('#modalTitle').textContent=title; $('#modalBody').innerHTML=body;
    const box=$('#modalButtons'); box.innerHTML='';
    buttons.forEach(b=>{
      const el=document.createElement('button');
      el.className='menu-btn '+(b.purple?'purple':'');
      el.textContent=b.text;
      el.onclick=()=>{modal.classList.add('hidden'); if(b.run)b.run();};
      box.appendChild(el);
    });
    modal.classList.remove('hidden');
  }
  function story(){
    show('#videoScreen');
    const v=$('#storyVideo'); v.muted=!sound; v.currentTime=0; v.play().catch(()=>{});
  }
  $('#storyVideo').addEventListener('ended',menu);
  function how(){show('#howScreen');}
  function choose(){
    show('#basketScreen');
    const box=$('#basketChoices'); box.innerHTML='';
    Object.entries(baskets).forEach(([key,b])=>{
      const el=document.createElement('button');
      el.className='basket-choice'+(key===selected?' selected':'');
      el.style.background=b.color;
      el.textContent=b.name+' • '+b.bonus;
      el.onclick=()=>{selected=key;choose();};
      box.appendChild(el);
    });
  }
  function countdown(){
    show('#countScreen');
    $('#readyText').textContent='SEDIA, '+baskets[selected].name+'!';
    let n=3; $('#countText').textContent='3...'; beep(560);
    const t=setInterval(()=>{
      n--;
      if(n>0){$('#countText').textContent=n+'...';beep(560);}
      else if(n===0){$('#countText').textContent='MULA!';beep(760,.15);}
      else{clearInterval(t);startGame();}
    },850);
  }
  function startGame(){
    show('#gameScreen');
    const canvas=$('#gameCanvas'), ctx=canvas.getContext('2d');
    state={time:60,score:0,lives:3,items:[],spawn:0,basketX:.5,basketW:.16,basketH:.09,ended:false};
    paused=false; lastTime=performance.now(); updateHud();
    cancelAnimationFrame(loopId);
    const frame=t=>{
      const dt=Math.min(.05,(t-lastTime)/1000); lastTime=t;
      if(!paused)update(dt);
      draw(canvas,ctx);
      if(!state.ended)loopId=requestAnimationFrame(frame);
    };
    loopId=requestAnimationFrame(frame);
  }
  function difficulty(){
    if(state.time>45)return {interval:.85,travel:5};
    if(state.time>30)return {interval:.70,travel:4.2};
    if(state.time>15)return {interval:.55,travel:3.5};
    return {interval:.42,travel:2.8};
  }
  function spawn(){
    const isFruit=Math.random()<.78;
    const data=isFruit?fruits[Math.floor(Math.random()*fruits.length)]:{name:wrongs[Math.floor(Math.random()*wrongs.length)],wrong:true,color:'#607d8b'};
    state.items.push({...data,x:.05+Math.random()*.9,y:-.08,r:.037});
  }
  function update(dt){
    state.time=Math.max(0,state.time-dt);
    state.spawn+=dt;
    const d=difficulty();
    if(state.spawn>=d.interval){state.spawn=0;spawn();}
    const move=.72*dt;
    if(keys.has('ArrowLeft')||keys.has('a'))state.basketX-=move;
    if(keys.has('ArrowRight')||keys.has('d'))state.basketX+=move;
    state.basketX=Math.max(state.basketW/2,Math.min(1-state.basketW/2,state.basketX));
    const speed=1/d.travel;
    state.items.forEach(it=>it.y+=speed*dt);
    const by=.87,bx0=state.basketX-state.basketW/2,bx1=state.basketX+state.basketW/2;
    state.items=state.items.filter(it=>{
      const hit=it.y+it.r>=by && it.y-it.r<=by+state.basketH && it.x>=bx0 && it.x<=bx1;
      if(hit){catchItem(it);return false;}
      return it.y<1.12;
    });
    if(state.time<=10 && state.time+dt>10){feedback('10 SAAT LAGI!');beep(700,.2);}
    if(state.time<=0||state.lives<=0)finish();
    updateHud();
  }
  function catchItem(it){
    if(it.wrong){state.lives--;feedback('BUKAN BUAH!');beep(180,.18);}
    else{const bonus=it.bonus===selected,pts=bonus?20:10;state.score+=pts;feedback((bonus?'BONUS ':'')+'+'+pts);beep(bonus?920:650);}
  }
  function feedback(t){const e=$('#feedback');e.textContent=t;clearTimeout(e._t);e._t=setTimeout(()=>e.textContent='',700);}
  function updateHud(){
    $('#hudTime').textContent='MASA: '+Math.ceil(state.time);
    $('#hudScore').textContent='SKOR: '+String(state.score).padStart(3,'0');
    $('#hudLives').textContent='♥'.repeat(state.lives)+'♡'.repeat(3-state.lives);
  }
  let bgImg=null;
  function draw(canvas,ctx){
    const ratio=devicePixelRatio||1;
    const W=canvas.clientWidth,H=canvas.clientHeight;
    canvas.width=Math.max(1,Math.round(W*ratio));canvas.height=Math.max(1,Math.round(H*ratio));
    ctx.setTransform(ratio,0,0,ratio,0,0);
    if(!bgImg){bgImg=new Image();bgImg.src='assets/gameplay_lighting.webp';}
    if(bgImg.complete&&bgImg.naturalWidth)ctx.drawImage(bgImg,0,0,W,H);
    else{ctx.fillStyle='#d9ecd1';ctx.fillRect(0,0,W,H);}
    ctx.fillStyle='#0002';ctx.fillRect(0,0,W,H);
    ctx.textAlign='center';ctx.textBaseline='middle';
    state.items.forEach(it=>{
      const x=it.x*W,y=it.y*H,r=it.r*Math.min(W,H)*1.3;
      ctx.beginPath();ctx.fillStyle=it.color;ctx.arc(x,y,r,0,Math.PI*2);ctx.fill();
      ctx.lineWidth=3;ctx.strokeStyle='#fff';ctx.stroke();
      ctx.font='700 '+Math.max(15,W*.015)+'px Segoe UI';
      ctx.fillStyle='#fff';ctx.strokeStyle='#0009';ctx.lineWidth=4;ctx.strokeText(it.name,x,y);ctx.fillText(it.name,x,y);
    });
    const bw=state.basketW*W,bh=state.basketH*H,bx=state.basketX*W-bw/2,by=.87*H;
    ctx.fillStyle=baskets[selected].color;ctx.beginPath();ctx.roundRect(bx,by,bw,bh,18);ctx.fill();
    ctx.strokeStyle='#fff';ctx.lineWidth=5;ctx.stroke();
    ctx.fillStyle='#fff';ctx.font='900 '+Math.max(18,W*.018)+'px Segoe UI';ctx.fillText(baskets[selected].name,bx+bw/2,by+bh/2);
  }
  function finish(){
    if(state.ended)return;
    state.ended=true;cancelAnimationFrame(loopId);
    const failed=state.lives<=0,best=Math.max(Number(storage.getItem('best')||0),state.score);storage.setItem('best',best);
    $('#resultTitle').textContent=failed?'PERMAINAN TAMAT':'TAHNIAH!';
    $('#resultImg').src=failed?'assets/story_briefing.webp':'assets/story_progress.webp';
    $('#resultStats').innerHTML='Skor akhir: <b>'+state.score+'</b><br>Nyawa tinggal: <b>'+state.lives+'</b><br>Skor tertinggi: <b>'+best+'</b><br><br>'+(failed?'Cuba lagi, kamu boleh!':'Masa tamat! Syabas!');
    show('#resultScreen');beep(failed?180:950,.3);
  }
  $('#pauseBtn').onclick=()=>{
    paused=true;
    openModal('GAME DIJEDA','Permainan sedang dijeda.',[
      {text:'SAMBUNG',run:()=>{paused=false;lastTime=performance.now();}},
      {text:'MULA SEMULA',run:countdown},
      {text:'MENU UTAMA',purple:true,run:menu}
    ]);
  };
  window.addEventListener('keydown',e=>keys.add(e.key.length===1?e.key.toLowerCase():e.key));
  window.addEventListener('keyup',e=>keys.delete(e.key.length===1?e.key.toLowerCase():e.key));
  $('#gameCanvas').addEventListener('mousemove',e=>{if(state&&!paused){const r=e.currentTarget.getBoundingClientRect();state.basketX=(e.clientX-r.left)/r.width;}});
  $('#gameCanvas').addEventListener('touchmove',e=>{if(state&&!paused&&e.touches[0]){const r=e.currentTarget.getBoundingClientRect();state.basketX=(e.touches[0].clientX-r.left)/r.width;}});
  document.body.addEventListener('click',e=>{
    const a=e.target.dataset&&e.target.dataset.action;
    if(a==='story')story();
    if(a==='how')how();
    if(a==='play')choose();
    if(a==='score')openModal('SKOR TERTINGGI',(storage.getItem('best')||0)+' mata',[{text:'OK'}]);
    if(e.target.hasAttribute&&e.target.hasAttribute('data-back'))menu();
  });
  $('#soundBtn').onclick=()=>{sound=!sound;storage.setItem('sound',sound?'on':'off');$('#soundBtn').textContent=sound?'BUNYI ON':'BUNYI OFF';$('#storyVideo').muted=!sound;};
  $('#startBtn').onclick=countdown;
  menu();
})();
