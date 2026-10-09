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
    GREEN:{name:'HIJAU',color:'#43a047',bonus:'Epal hijau',sprite:'baskets/bakul_hijau.png'},
    RED:{name:'MERAH',color:'#d93636',bonus:'Epal merah',sprite:'baskets/bakul_merah.png'},
    PURPLE:{name:'UNGU',color:'#8525a6',bonus:'Anggur',sprite:'baskets/bakul_ungu.png'},
    ORANGE:{name:'OREN',color:'#e57500',bonus:'Oren',sprite:'baskets/bakul_oren.png'}
  };
  const fruits = [
    {name:'Epal hijau',bonus:'GREEN',sprite:'fruits/epal_hijau.png'},
    {name:'Epal merah',bonus:'RED',sprite:'fruits/epal_merah.png'},
    {name:'Oren',bonus:'ORANGE',sprite:'fruits/oren.png'},
    {name:'Anggur',bonus:'PURPLE',sprite:'fruits/anggur.png'},
    {name:'Mangga',sprite:'fruits/mangga.png'},
    {name:'Pir',sprite:'fruits/pir.png'},
    {name:'Strawberi',sprite:'fruits/strawberi.png'},
    {name:'Pisang',sprite:'fruits/pisang.png'}
  ];
  const wrongs = [
    {name:'BOTOL',wrong:true,sprite:'wrong/botol.png'},
    {name:'MAINAN',wrong:true,sprite:'wrong/mainan.png'},
    {name:'TIN',wrong:true,sprite:'wrong/tin.png'},
    {name:'KOTAK',wrong:true,sprite:'wrong/kotak.png'},
    {name:'KASUT',wrong:true,sprite:'wrong/kasut.png'}
  ];

  // Cache decoded images before play; NEVER construct Image() in the frame loop.
  const images = new Map();
  let preloadPromise;
  function loadGameSprites() {
    if (preloadPromise) return preloadPromise;
    const spritePaths = [
      ...Object.values(baskets).map(b => b.sprite),
      ...fruits.map(f => f.sprite),
      ...wrongs.map(w => w.sprite),
      'gameplay_lighting.webp'
    ];
    preloadPromise = Promise.all(spritePaths.map(relative => new Promise(resolve => {
      const img = new Image();
      img.onload = () => { images.set(relative, img); resolve(); };
      img.onerror = () => {
        console.error('PNG/imej tidak dijumpai: windows/src/assets/' +
          (relative.endsWith('.webp') ? relative : 'sprites/' + relative));
        resolve();
      };
      img.src = relative.endsWith('.webp')
        ? 'assets/' + relative : 'assets/sprites/' + relative;
    })));
    return preloadPromise;
  }
  function sprite(relative) { return images.get(relative); }
  function bound(value, minimum, maximum) {
    return Math.max(minimum, Math.min(maximum, value));
  }
  function basketSize(W, H) {
    const img = sprite(baskets[selected].sprite);
    const width = bound(W * .155, 145, 285);
    const height = img ? width * img.naturalHeight / img.naturalWidth : width * .8;
    return {width, height:Math.min(height, H * .33)};
  }
  function fallingSize(item, W, H) {
    const img = sprite(item.sprite);
    const width = bound(W * .054, 62, 110);
    const height = img ? width * img.naturalHeight / img.naturalWidth : width;
    return {width, height:Math.min(height,H*.19)};
  }

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
  async function startGame(){
    await loadGameSprites();
    // If user left the countdown while loading assets, do not start a round.
    if ($('#countScreen').classList.contains('hidden')) return;
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
    const data=isFruit
      ? fruits[Math.floor(Math.random()*fruits.length)]
      : wrongs[Math.floor(Math.random()*wrongs.length)];
    state.items.push({...data,x:.06+Math.random()*.88,y:-.10});
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
    const canvas=$('#gameCanvas');
    const W=canvas.clientWidth,H=canvas.clientHeight;
    const basket=basketSize(W,H),cx=state.basketX*W;
    // Upper basket opening only: transparent handles should not trigger catches.
    const catchLeft=cx-basket.width*.38,catchRight=cx+basket.width*.38;
    const catchTop=H-basket.height*.84,catchBottom=H-basket.height*.33;
    state.items=state.items.filter(it=>{
      const d=fallingSize(it,W,H),px=it.x*W,py=it.y*H;
      const hit=px+d.width*.4>catchLeft && px-d.width*.4<catchRight &&
        py+d.height*.40>catchTop && py-d.height*.40<catchBottom;
      if(hit){catchItem(it);return false;}
      return py-d.height/2 < H;
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
  // Responsive cover-fit background and individually cropped transparent PNG sprites.
  function draw(canvas,ctx){
    const ratio=devicePixelRatio||1;
    const W=canvas.clientWidth,H=canvas.clientHeight;
    const deviceW=Math.max(1,Math.round(W*ratio));
    const deviceH=Math.max(1,Math.round(H*ratio));
    if(canvas.width!==deviceW||canvas.height!==deviceH){
      canvas.width=deviceW;
      canvas.height=deviceH;
    }
    ctx.setTransform(ratio,0,0,ratio,0,0);
    ctx.clearRect(0,0,W,H);
    ctx.imageSmoothingEnabled=true;
    ctx.imageSmoothingQuality='high';

    const background=sprite('gameplay_lighting.webp');
    if(background){
      const scale=Math.max(W/background.naturalWidth,H/background.naturalHeight);
      const bw=background.naturalWidth*scale,bh=background.naturalHeight*scale;
      ctx.drawImage(background,(W-bw)/2,(H-bh)/2,bw,bh);
    }else{
      ctx.fillStyle='#d9ecd1';ctx.fillRect(0,0,W,H);
    }
    ctx.fillStyle='#0002';ctx.fillRect(0,0,W,H);

    // Actual falling-object sprites: no placeholder circles or labels.
    state.items.forEach(it=>{
      const img=sprite(it.sprite);
      if(!img)return;
      const d=fallingSize(it,W,H);
      ctx.drawImage(img,it.x*W-d.width/2,it.y*H-d.height/2,d.width,d.height);
    });

    const basketImg=sprite(baskets[selected].sprite);
    if(basketImg){
      const d=basketSize(W,H);
      ctx.drawImage(basketImg,state.basketX*W-d.width/2,H-d.height-4,d.width,d.height);
    }
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
  loadGameSprites();
  menu();
})();
