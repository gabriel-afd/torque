// CT53 (fluxo completo via API) e CT57 (dados pessoais nas respostas)
const B = process.env.BASE ?? 'http://localhost:4200/api'; // passa pelo proxy do frontend
const out = [];
const log = (ok, passo, det = '') => { out.push({ ok, passo, det }); console.log(`${ok ? 'OK ' : 'FALHA'} | ${passo} | ${det}`); };

async function req(token, metodo, rota, corpo) {
  const r = await fetch(B + rota, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  const txt = await r.text();
  let json; try { json = JSON.parse(txt); } catch { json = txt; }
  return { s: r.status, j: json, txt, h: r.headers };
}
const login = async (email, senha) => (await req(null, 'POST', '/auth/login', { email, senha })).j.token;

function cpf() {
  const n = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
  for (const len of [9, 10]) {
    let s = 0; for (let i = 0; i < len; i++) s += n[i] * (len + 1 - i);
    const d = (s * 10) % 11; n.push(d === 10 ? 0 : d);
  }
  return n.join('');
}

if (!process.env.ADMIN_EMAIL || !process.env.ADMIN_SENHA) throw new Error('Defina ADMIN_EMAIL e ADMIN_SENHA');
const ana = await login('ana.demo@oficina.teste', 'demo12345');
const adm = await login(process.env.ADMIN_EMAIL, process.env.ADMIN_SENHA);
const rai = await login('raimundo.demo@oficina.teste', 'demo12345');
log(!!ana && !!adm && !!rai, 'CT53.1 login atendente, admin e mecânico');

const quadro = await req(ana, 'GET', '/ordens?size=100');
log(quadro.s === 200 && quadro.j.content.length > 0, 'CT53.1 quadro de OS', `${quadro.j.totalElements} OS`);

const doc = cpf();
const cli = await req(ana, 'POST', '/clientes', { nome: 'Cliente Teste Manual', documento: doc, telefone: '(83) 99999-0000', email: 'teste.manual@exemplo.teste' });
log(cli.s === 201, 'CT53.2 cadastrar cliente', `HTTP ${cli.s} id=${cli.j.id}`);

const placa = 'TST1A23';
const vei = await req(ana, 'POST', `/clientes/${cli.j.id}/veiculos`, { placa: 'tst-1a23', marca: 'Fiat', modelo: 'Uno', ano: 2015, cor: 'Branco' });
log(vei.s === 201 && vei.j.placa === placa, 'CT53.3 cadastrar veículo (placa normalizada)', `HTTP ${vei.s} placa=${vei.j.placa}`);

const busca = await req(ana, 'GET', `/veiculos?placa=${placa}`);
const veiculoId = (busca.j.content ?? busca.j)[0]?.id ?? vei.j.id;
const os = await req(ana, 'POST', '/ordens', { veiculoId, problemaRelatado: 'Teste manual', quilometragem: 85000 });
log(os.s === 201 && os.j.status === 'ABERTA', 'CT53.4 abrir OS pela placa', `HTTP ${os.s} OS nº ${os.j.numero} ${os.j.status}`);
const id = os.j.id;

const serv = (await req(ana, 'GET', '/servicos?size=5')).j.content[0];
const peca = (await req(ana, 'GET', '/pecas?size=5')).j.content[0];
const bruto = Number(serv.precoMaoDeObra) + 2 * Number(peca.precoVenda);
const d5 = (bruto * 0.05).toFixed(2);
let orc = await req(ana, 'PUT', `/ordens/${id}/orcamentos/1`, { servicos: [{ servicoId: serv.id }], pecas: [{ pecaId: peca.id, quantidade: 2 }], desconto: d5 });
const esperado = (bruto - Number(d5)).toFixed(2);
log(orc.s === 200 && Number(orc.j.orcamento.total).toFixed(2) === esperado, 'CT53.5 orçamento 1 serviço + 2 peças, desconto 5% (RN04)', `total=${orc.j.orcamento?.total} esperado=${esperado}`);

const d15 = (bruto * 0.15).toFixed(2);
const orc15 = await req(ana, 'PUT', `/ordens/${id}/orcamentos/1`, { servicos: [{ itemId: orc.j.orcamento.servicos[0].id }], pecas: [{ itemId: orc.j.orcamento.pecas[0].id, quantidade: 2 }], desconto: d15 });
log(orc15.s === 422, 'CT53.6 desconto 15% como atendente recusado (RN05)', `HTTP ${orc15.s} "${orc15.j.mensagem}"`);

const st = async (t, status) => req(t, 'PATCH', `/ordens/${id}/status`, { status });
let r = await st(ana, 'AGUARDANDO_APROVACAO');
log(r.s === 200 && r.j.status === 'AGUARDANDO_APROVACAO', 'CT53.7 enviar para aprovação', `${r.j.status}, histórico com ${r.j.historico?.length} registro(s)`);

const pdf = await fetch(`${B}/ordens/${id}/orcamentos/1/pdf`, { headers: { Authorization: 'Bearer ' + ana } });
const bytes = Buffer.from(await pdf.arrayBuffer());
log(pdf.status === 200 && bytes.subarray(0, 4).toString() === '%PDF', 'CT53.8 PDF do orçamento', `${pdf.headers.get('content-disposition')} ${bytes.length} bytes`);
const comp = await req(ana, 'GET', `/ordens/${id}/compartilhamento`);
log(comp.s === 200, 'CT53.8 link do WhatsApp', JSON.stringify(comp.j).slice(0, 160));

r = await req(ana, 'POST', `/ordens/${id}/orcamentos/1/aprovacao`);
log(r.s === 200 && r.j.status === 'APROVADA', 'CT53.9 registrar aprovação', r.j.status);

const usuarios = (await req(adm, 'GET', '/usuarios?size=50')).j.content;
const raiId = usuarios.find(u => u.email === 'raimundo.demo@oficina.teste').id;
r = await req(adm, 'PATCH', `/ordens/${id}/mecanico`, { mecanicoId: raiId });
log(r.s === 200 && r.j.mecanico?.nome?.startsWith('Raimundo'), 'CT53.10 admin atribui Raimundo', r.j.mecanico?.nome);

const tiago = await login('tiago.demo@oficina.teste', 'demo12345');
r = await st(tiago, 'EM_EXECUCAO');
log(r.s === 403 || r.s === 422, 'CT53.11 (extra) outro mecânico não altera a OS (RN09)', `HTTP ${r.s}`);
r = await st(rai, 'EM_EXECUCAO');
log(r.s === 200 && r.j.status === 'EM_EXECUCAO', 'CT53.11 Raimundo inicia serviço', r.j.status);
r = await st(rai, 'AGUARDANDO_PECA');
log(r.j.status === 'AGUARDANDO_PECA', 'CT53.12 aguardando peça', r.j.status);
r = await st(rai, 'EM_EXECUCAO');
log(r.j.status === 'EM_EXECUCAO', 'CT53.12 retomar serviço', r.j.status);
r = await st(rai, 'CONCLUIDA');
log(r.j.status === 'CONCLUIDA', 'CT53.13 concluir', r.j.status);
r = await st(ana, 'ENTREGUE');
log(r.j.status === 'ENTREGUE' && r.j.dataEntrega, 'CT53.14 entregar', `${r.j.status} em ${r.j.dataEntrega}`);
const fim = await st(ana, 'CANCELADA');
log(fim.s === 422, 'CT53.14 (extra) ENTREGUE é final (RN08)', `HTTP ${fim.s}`);
log(r.j.historico?.length >= 8, 'CT53.14 histórico de status', `${r.j.historico?.length} registros`);

const hist = await req(ana, 'GET', `/veiculos/${veiculoId}/ordens`);
const lista = hist.j.content ?? hist.j.ordens ?? hist.j;
const topo = Array.isArray(lista) ? lista[0] : null;
log(hist.s === 200 && topo && (topo.id === id || topo.numero === os.j.numero), 'CT53.15 OS no topo do histórico do veículo', JSON.stringify(topo).slice(0, 200));

// ---------- CT57 ----------
const loginResp = await req(null, 'POST', '/auth/login', { email: 'ana.demo@oficina.teste', senha: 'demo12345' });
log(!/senha|\$2[aby]\$/i.test(loginResp.txt), 'CT57.1 resposta do login sem senha/hash', Object.keys(loginResp.j).join(','));
const us = await req(adm, 'GET', '/usuarios?size=50');
log(!/"senha"|\$2[aby]\$/i.test(us.txt), 'CT57.1 GET /usuarios sem senha/hash', `${us.j.content.length} usuários, campos: ${Object.keys(us.j.content[0]).join(',')}`);
const osMec = await req(rai, 'GET', `/ordens/${id}`);
const c = osMec.j.cliente ?? {};
log(osMec.s === 200 && !c.documento && !c.telefone && !c.email, 'CT57.2 mecânico vê cliente sem CPF, telefone e e-mail', JSON.stringify(c));
const osAte = await req(ana, 'GET', `/ordens/${id}`);
log(!!osAte.j.cliente?.documento, 'CT57.2 (controle) atendente vê o documento', '');
const dup = await req(ana, 'POST', '/clientes', { nome: 'Duplicado', documento: doc, telefone: '(83) 98888-0000' });
log(dup.s === 409 && !dup.txt.includes('at br.') && !dup.txt.includes('Exception'), 'CT57.4 CPF duplicado: 409 sem stack trace', `HTTP ${dup.s} ${dup.txt}`);
const sem = await req(null, 'GET', '/ordens');
log(sem.s === 401, 'CT57 (extra) sem token: 401', `HTTP ${sem.s} ${sem.txt}`);

console.log(JSON.stringify({ osId: id, numero: os.j.numero }));
console.log(`\nTOTAL ${out.length} verificações, ${out.filter(x => !x.ok).length} falhas`);
