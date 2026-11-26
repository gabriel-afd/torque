// CT54 (360 px) e CT55 (telas até abrir OS) no Chrome headless via DevTools Protocol
import { spawn } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import { createRequire } from 'node:module';
const require = createRequire(process.env.WEB_DIR + '/package.json');
const WebSocket = require('ws');

const DIR = process.env.OUT_DIR; mkdirSync(DIR, { recursive: true });
const URL0 = 'http://localhost:4200';
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--remote-debugging-port=9333', `--user-data-dir=${DIR}/perfil`, '--no-first-run', 'about:blank']);
const espera = (ms) => new Promise((r) => setTimeout(r, ms));
let alvo;
for (let i = 0; i < 40 && !alvo; i++) {
  await espera(250);
  try { alvo = (await (await fetch('http://127.0.0.1:9333/json')).json()).find((t) => t.type === 'page'); } catch {}
}
const ws = new WebSocket(alvo.webSocketDebuggerUrl);
await new Promise((r) => ws.on('open', r));
let seq = 0; const pend = new Map();
ws.on('message', (m) => { const d = JSON.parse(m); if (d.id && pend.has(d.id)) { pend.get(d.id)(d); pend.delete(d.id); } });
const cdp = (method, params = {}) => new Promise((r) => { const id = ++seq; pend.set(id, r); ws.send(JSON.stringify({ id, method, params })); });
const js = async (expr) => (await cdp('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true })).result?.result?.value;
const ate = async (expr, ms = 10000) => { for (let t = 0; t < ms; t += 200) { if (await js(expr)) return true; await espera(200); } return false; };
const digita = async (sel, txt) => { await js(`document.querySelector('${sel}').focus()`); await cdp('Input.insertText', { text: txt }); };
const clica = (sel) => js(`document.querySelector('${sel}').click()`);
const print = async (nome) => { const { result } = await cdp('Page.captureScreenshot', { captureBeyondViewport: true }); writeFileSync(`${DIR}/${nome}.png`, Buffer.from(result.data, 'base64')); };
const largura = () => js(`({ vw: innerWidth, sw: document.documentElement.scrollWidth, bw: document.body.scrollWidth,
  largos: [...document.querySelectorAll('body *')].filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.right > innerWidth + 1 && !e.closest('.quadro, .colunas, table, .tabela, .rolagem, [class*=scroll]'); }).slice(0,5).map(e => e.tagName + '.' + [...e.classList].join('.')) })`);
const res = [];
const reg = (ct, tela, ok, det) => { res.push({ ct, tela, ok, det }); console.log(`${ok ? 'OK ' : 'FALHA'} | ${ct} | ${tela} | ${det}`); };

await cdp('Page.enable'); await cdp('Runtime.enable');
await cdp('Emulation.setDeviceMetricsOverride', { width: 360, height: 780, deviceScaleFactor: 2, mobile: true });

// Login
await cdp('Page.navigate', { url: URL0 + '/login' });
await ate(`!!document.querySelector('input[formControlName=email]')`);
await espera(500);
let w = await largura(); await print('ct54-1-login');
reg('CT54', 'Login', w.sw <= w.vw, JSON.stringify(w));

await digita('input[formControlName=email]', 'ana.demo@oficina.teste');
await digita('input[formControlName=senha]', 'demo12345');
await clica('button[type=submit]');
await ate(`location.pathname === '/quadro' && document.querySelectorAll('[data-testid=cartao]').length > 0`);
await espera(800);
w = await largura(); await print('ct54-2-quadro');
const menu = await js(`[...document.querySelectorAll('button')].some(b => /menu/i.test(b.textContent + b.getAttribute('aria-label')) && b.offsetParent)`);
const colunas = await js(`(() => { const q = document.querySelector('[data-testid=cartao]').closest('[class*=quadro], [class*=colunas]'); return q ? { classe: q.className, sw: q.scrollWidth, cw: q.clientWidth, overflowX: getComputedStyle(q).overflowX } : null })()`);
reg('CT54', 'Quadro de OS', w.sw <= w.vw && menu, JSON.stringify({ ...w, menuHamburguer: menu, colunas }));

// CT55: quadro -> diálogo -> OS

const caminho = [await js('location.pathname')];
await clica('[data-testid=nova-os]');
await ate(`!!document.querySelector('mat-dialog-container [data-testid=busca-placa]')`);
caminho.push('diálogo Nova OS');
await print('ct55-1-dialogo');
await digita('[data-testid=busca-placa]', 'TST1A23');
await clica('[data-testid=buscar-placa]');
await ate(`!!document.querySelector('[data-testid=problema]')`);
await digita('[data-testid=problema]', 'Barulho na suspensão (teste CT55)');
await print('ct55-2-dialogo-preenchido');
await clica('[data-testid=abrir-os]');
await ate(`/^\\/ordens\\/\\d+/.test(location.pathname)`);
await espera(1000);
caminho.push(await js('location.pathname'));
await print('ct55-3-os-aberta');
const status = await js(`document.body.innerText.match(/Aberta/) ? 'Aberta' : '?'`);
reg('CT55', 'Abrir OS', caminho.length <= 3 && status === 'Aberta', `${caminho.length} telas: ${caminho.join(' → ')}; status ${status}`);

// Tela da OS (a do CT53, com orçamento)
await cdp('Page.navigate', { url: URL0 + '/ordens/' + process.env.OS_ID });
await ate(`document.body.innerText.includes('TST1A23')`); await espera(1000);
w = await largura(); await print('ct54-3-ordem');
const ordem = await js(`(() => { const r = document.querySelector('[class*=resumo]'); const i = document.querySelector('app-orcamento-itens, [class*=itens]'); return r && i ? { resumoAbaixoDosItens: r.getBoundingClientRect().top >= i.getBoundingClientRect().top } : null })()`);
reg('CT54', 'Tela da OS', w.sw <= w.vw, JSON.stringify({ ...w, ordem }));

// Histórico do veículo
await cdp('Page.navigate', { url: URL0 + '/veiculos/TST1A23' });
await ate(`document.body.innerText.includes('Teste manual')`); await espera(1000);
w = await largura(); await print('ct54-4-historico');
const fab = await js(`(() => { const f = document.querySelector('[data-testid=fab-nova-os]'); if (!f) return null; window.scrollTo(0, document.body.scrollHeight); const cards = [...document.querySelectorAll('[class*=card], article, li')].filter(e => e.offsetParent && !e.contains(f)); const ult = cards[cards.length-1].getBoundingClientRect(); const fr = f.getBoundingClientRect(); return { posicao: getComputedStyle(f).position, fabTopo: fr.top, ultimoCartaoFim: ult.bottom, sobrepoe: ult.bottom > fr.top && ult.right > fr.left } })()`);
await espera(300); await print('ct54-5-historico-fim');
reg('CT54', 'Histórico do veículo', w.sw <= w.vw && fab && !fab.sobrepoe, JSON.stringify({ ...w, fab }));

writeFileSync(`${DIR}/resultado.json`, JSON.stringify(res, null, 2));
console.log(`\nTOTAL ${res.length}, falhas ${res.filter((r) => !r.ok).length}`);
ws.close(); chrome.kill();
process.exit(0);
