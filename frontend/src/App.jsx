import { useEffect, useState, useCallback } from 'react';
import { api } from './api';
import './App.css';

function useEntidades() {
  const [entidades, setEntidades] = useState([]);
  const recarregar = useCallback(() => {
    api.listarEntidades().then(setEntidades).catch(() => setEntidades([]));
  }, []);
  useEffect(recarregar, [recarregar]);
  return [entidades, recarregar];
}

function SeletorEntidade({ entidades, value, onChange, placeholder }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)} required>
      <option value="">{placeholder}</option>
      {entidades.map((e) => (
        <option key={e.id} value={e.id}>{e.nome} ({e.tipo})</option>
      ))}
    </select>
  );
}

function AbaEntidades({ entidades, recarregar, mostrarErro }) {
  const [form, setForm] = useState({ nome: '', tipo: '', descricao: '' });
  const [posicaoDe, setPosicaoDe] = useState(null);
  const [posicao, setPosicao] = useState([]);

  const criar = async (ev) => {
    ev.preventDefault();
    try {
      await api.criarEntidade(form);
      setForm({ nome: '', tipo: '', descricao: '' });
      recarregar();
    } catch (e) { mostrarErro(e.message); }
  };

  const verPosicao = async (ent) => {
    try {
      setPosicaoDe(ent);
      setPosicao(await api.posicao(ent.id));
    } catch (e) { mostrarErro(e.message); }
  };

  return (
    <section>
      <form onSubmit={criar} className="linha-form">
        <input placeholder="Nome (ex: TV 50, João, Real BRL)" value={form.nome}
          onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
        <input placeholder="Tipo (PRODUTO, PESSOA, MOEDA...)" value={form.tipo}
          onChange={(e) => setForm({ ...form, tipo: e.target.value })} required />
        <input placeholder="Descrição (opcional)" value={form.descricao}
          onChange={(e) => setForm({ ...form, descricao: e.target.value })} />
        <button type="submit">Cadastrar</button>
      </form>

      <table>
        <thead><tr><th>#</th><th>Nome</th><th>Tipo</th><th>Descrição</th><th></th></tr></thead>
        <tbody>
          {entidades.map((e) => (
            <tr key={e.id}>
              <td>{e.id}</td>
              <td>{e.nome}</td>
              <td><span className="tag">{e.tipo}</span></td>
              <td>{e.descricao}</td>
              <td><button className="leve" onClick={() => verPosicao(e)}>Posição</button></td>
            </tr>
          ))}
        </tbody>
      </table>

      {posicaoDe && (
        <div className="painel">
          <h3>Posição de {posicaoDe.nome} <small>(derivada das trocas — nada cadastrado)</small></h3>
          {posicao.length === 0 ? <p>Nenhuma troca registrada ainda.</p> : (
            <table>
              <thead><tr><th>Objeto</th><th>Tipo</th><th>Saldo</th></tr></thead>
              <tbody>
                {posicao.map((p) => (
                  <tr key={p.objetoId}>
                    <td>{p.objetoNome}</td>
                    <td><span className="tag">{p.objetoTipo}</span></td>
                    <td className={p.saldo < 0 ? 'negativo' : 'positivo'}>{p.saldo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </section>
  );
}

function AbaEstruturas({ entidades, mostrarErro }) {
  const [paiId, setPaiId] = useState('');
  const [itens, setItens] = useState([]);
  const [form, setForm] = useState({ filhoId: '', quantidade: '1', papel: 'COMPONENTE' });

  const carregar = useCallback((id) => {
    if (!id) { setItens([]); return; }
    api.listarEstrutura(id).then(setItens).catch((e) => mostrarErro(e.message));
  }, [mostrarErro]);

  useEffect(() => carregar(paiId), [paiId, carregar]);

  const criar = async (ev) => {
    ev.preventDefault();
    try {
      await api.criarEstrutura({
        paiId: Number(paiId),
        filhoId: Number(form.filhoId),
        quantidade: Number(form.quantidade),
        papel: form.papel,
      });
      setForm({ filhoId: '', quantidade: '1', papel: 'COMPONENTE' });
      carregar(paiId);
    } catch (e) { mostrarErro(e.message); }
  };

  const remover = async (id) => {
    try { await api.removerEstrutura(id); carregar(paiId); }
    catch (e) { mostrarErro(e.message); }
  };

  return (
    <section>
      <div className="linha-form">
        <label>Entidade pai:</label>
        <SeletorEntidade entidades={entidades} value={paiId} onChange={setPaiId}
          placeholder="Selecione a entidade composta" />
      </div>

      {paiId && (
        <>
          <form onSubmit={criar} className="linha-form">
            <SeletorEntidade entidades={entidades.filter((e) => String(e.id) !== paiId)}
              value={form.filhoId} onChange={(v) => setForm({ ...form, filhoId: v })}
              placeholder="É formada por..." />
            <input type="number" min="0.0001" step="any" value={form.quantidade}
              onChange={(e) => setForm({ ...form, quantidade: e.target.value })} />
            <input placeholder="Papel" value={form.papel}
              onChange={(e) => setForm({ ...form, papel: e.target.value })} />
            <button type="submit">Adicionar componente</button>
          </form>

          <table>
            <thead><tr><th>Componente</th><th>Qtd</th><th>Papel</th><th></th></tr></thead>
            <tbody>
              {itens.map((s) => (
                <tr key={s.id}>
                  <td>{s.filho.nome} <span className="tag">{s.filho.tipo}</span></td>
                  <td>{s.quantidade}</td>
                  <td>{s.papel}</td>
                  <td><button className="leve" onClick={() => remover(s.id)}>Remover</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}

function AbaTrocas({ entidades, mostrarErro }) {
  const [trocas, setTrocas] = useState([]);
  const [tipo, setTipo] = useState('VENDA');
  const [descricao, setDescricao] = useState('');
  const pernaVazia = { deId: '', paraId: '', objetoId: '', quantidade: '1' };
  const [pernas, setPernas] = useState([{ ...pernaVazia }]);

  const carregar = useCallback(() => {
    api.listarTrocas().then(setTrocas).catch(() => setTrocas([]));
  }, []);
  useEffect(carregar, [carregar]);

  const setPerna = (i, campo, valor) => {
    setPernas((ps) => ps.map((p, j) => (j === i ? { ...p, [campo]: valor } : p)));
  };

  const registrar = async (ev) => {
    ev.preventDefault();
    try {
      await api.registrarTroca({
        tipo,
        descricao,
        pernas: pernas.map((p) => ({
          deId: Number(p.deId), paraId: Number(p.paraId),
          objetoId: Number(p.objetoId), quantidade: Number(p.quantidade),
        })),
      });
      setPernas([{ ...pernaVazia }]);
      setDescricao('');
      carregar();
    } catch (e) { mostrarErro(e.message); }
  };

  return (
    <section>
      <form onSubmit={registrar} className="painel">
        <div className="linha-form">
          <input placeholder="Tipo (VENDA, PAGAMENTO, ALOCACAO...)" value={tipo}
            onChange={(e) => setTipo(e.target.value)} required />
          <input placeholder="Descrição" value={descricao}
            onChange={(e) => setDescricao(e.target.value)} />
        </div>
        {pernas.map((p, i) => (
          <div className="linha-form" key={i}>
            <SeletorEntidade entidades={entidades} value={p.deId}
              onChange={(v) => setPerna(i, 'deId', v)} placeholder="Quem entrega" />
            <span>→</span>
            <SeletorEntidade entidades={entidades} value={p.paraId}
              onChange={(v) => setPerna(i, 'paraId', v)} placeholder="Quem recebe" />
            <SeletorEntidade entidades={entidades} value={p.objetoId}
              onChange={(v) => setPerna(i, 'objetoId', v)} placeholder="O quê" />
            <input type="number" min="0.0001" step="any" value={p.quantidade}
              onChange={(e) => setPerna(i, 'quantidade', e.target.value)} />
            {pernas.length > 1 && (
              <button type="button" className="leve"
                onClick={() => setPernas((ps) => ps.filter((_, j) => j !== i))}>×</button>
            )}
          </div>
        ))}
        <div className="linha-form">
          <button type="button" className="leve"
            onClick={() => setPernas((ps) => [...ps, { ...pernaVazia }])}>
            + Perna (contrapartida)
          </button>
          <button type="submit">Registrar troca</button>
        </div>
      </form>

      <table>
        <thead><tr><th>Data</th><th>Tipo</th><th>De</th><th>Para</th><th>Objeto</th><th>Qtd</th><th>Descrição</th></tr></thead>
        <tbody>
          {trocas.map((t) => (
            <tr key={t.id}>
              <td>{new Date(t.data).toLocaleString('pt-BR')}</td>
              <td><span className="tag">{t.tipo}</span></td>
              <td>{t.de.nome}</td>
              <td>{t.para.nome}</td>
              <td>{t.objeto.nome}</td>
              <td>{t.quantidade}</td>
              <td>{t.descricao}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

export default function App() {
  const [aba, setAba] = useState('entidades');
  const [entidades, recarregar] = useEntidades();
  const [erro, setErro] = useState('');

  const mostrarErro = useCallback((msg) => {
    setErro(msg);
    setTimeout(() => setErro(''), 5000);
  }, []);

  return (
    <main>
      <header>
        <h1>Sistema de Troca</h1>
        <p className="subtitulo">Tudo é Entidade, Estrutura ou Troca.</p>
      </header>

      <nav>
        {[['entidades', 'Entidades'], ['estruturas', 'Estruturas'], ['trocas', 'Trocas']].map(([k, label]) => (
          <button key={k} className={aba === k ? 'ativo' : ''} onClick={() => setAba(k)}>{label}</button>
        ))}
      </nav>

      {erro && <div className="erro">{erro}</div>}

      {aba === 'entidades' && <AbaEntidades entidades={entidades} recarregar={recarregar} mostrarErro={mostrarErro} />}
      {aba === 'estruturas' && <AbaEstruturas entidades={entidades} mostrarErro={mostrarErro} />}
      {aba === 'trocas' && <AbaTrocas entidades={entidades} mostrarErro={mostrarErro} />}
    </main>
  );
}
