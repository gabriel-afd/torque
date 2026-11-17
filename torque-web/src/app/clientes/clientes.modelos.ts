import { StatusOS } from '../ordens/ordens.modelos';

export interface Cliente {
  id: number;
  nome: string;
  documento: string;
  telefone: string;
  email: string | null;
  ativo: boolean;
}

export interface Veiculo {
  id: number;
  clienteId: number;
  placa: string;
  marca: string;
  modelo: string;
  ano: number | null;
  cor: string | null;
}

export interface ClienteDetalhe extends Cliente {
  veiculos: Veiculo[];
}

/** Resposta da busca por placa. Para MECANICO, documento/telefone/email não vêm. */
export interface VeiculoComDono {
  id: number;
  placa: string;
  marca: string;
  modelo: string;
  ano: number | null;
  cor: string | null;
  dono: { id: number; nome: string; documento?: string; telefone?: string; email?: string | null; ativo: boolean };
}

export interface ClienteRequest {
  nome: string;
  documento: string;
  telefone: string;
  email: string | null;
}

export interface VeiculoRequest {
  placa: string;
  marca: string;
  modelo: string;
  ano: number | null;
  cor: string | null;
}

/** 409 da API → campo do formulário. */
export const CONFLITOS_CLIENTE = { documento: /CPF|CNPJ/i };
export const CONFLITOS_VEICULO = { placa: /placa/i };

/** Uma OS no histórico do veículo (GET /api/veiculos/{id}/ordens). `servicos` e `total` são do orçamento vigente. */
export interface OsDoVeiculo {
  id: number;
  numero: number;
  status: StatusOS;
  dataAbertura: string;
  dataEntrega: string | null;
  quilometragem: number | null;
  problemaRelatado: string;
  servicos: string[];
  total: number | null;
}
