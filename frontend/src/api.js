const BASE = 'http://localhost:8080/api';

async function req(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.erro || body.message || `Erro ${res.status}`);
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') return null;
  return res.json().catch(() => null);
}

export const api = {
  listarEntidades: (tipo) => req(`/entidades${tipo ? `?tipo=${encodeURIComponent(tipo)}` : ''}`),
  criarEntidade: (dados) => req('/entidades', { method: 'POST', body: JSON.stringify(dados) }),
  desativarEntidade: (id) => req(`/entidades/${id}`, { method: 'DELETE' }),
  posicao: (id) => req(`/entidades/${id}/posicao`),

  listarEstrutura: (paiId) => req(`/estruturas?paiId=${paiId}`),
  criarEstrutura: (dados) => req('/estruturas', { method: 'POST', body: JSON.stringify(dados) }),
  removerEstrutura: (id) => req(`/estruturas/${id}`, { method: 'DELETE' }),

  listarTrocas: (entidadeId) => req(`/trocas${entidadeId ? `?entidadeId=${entidadeId}` : ''}`),
  registrarTroca: (dados) => req('/trocas', { method: 'POST', body: JSON.stringify(dados) }),

  produzir: (dados) => req('/producoes', { method: 'POST', body: JSON.stringify(dados) }),
  planejar: (produtoId, quantidade, produtorId) =>
    req(`/planejamentos?produtoId=${produtoId}&quantidade=${quantidade}&produtorId=${produtorId}`),
  efetivarGrupo: (grupoId) => req(`/trocas/grupo/${grupoId}/efetivar`, { method: 'POST' }),
  cancelarGrupo: (grupoId) => req(`/trocas/grupo/${grupoId}`, { method: 'DELETE' }),
};
