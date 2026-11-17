#!/usr/bin/env bash
# CT56 (RNF05): mede o tempo de resposta de GET /api/ordens (meta: até 2 s).
# Uso: EMAIL=ana.demo@oficina.teste SENHA=demo12345 ./medir-listagem.sh [http://localhost:8080] [repeticoes]
set -euo pipefail
API="${1:-http://localhost:8080}"
REPETICOES="${2:-10}"
: "${EMAIL:?defina EMAIL}" "${SENHA:?defina SENHA}"

TOKEN=$(curl -sf -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"senha\":\"$SENHA\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')

medir() {
  local nome="$1" url="$2" tempos=()
  curl -sf -o /dev/null -H "Authorization: Bearer $TOKEN" "$API$url"   # aquecimento
  for _ in $(seq "$REPETICOES"); do
    tempos+=("$(curl -sf -o /dev/null -w '%{time_total}' -H "Authorization: Bearer $TOKEN" "$API$url")")
  done
  printf '%s\n' "${tempos[@]}" | sort -n | awk -v nome="$nome" '
    { t[NR] = $1; soma += $1 }
    END { printf "%-45s média %.3f s | mediana %.3f s | máx %.3f s (%d chamadas)\n", nome, soma / NR, t[int((NR + 1) / 2)], t[NR], NR }'
}

echo "Total de OS na listagem: $(curl -sf -H "Authorization: Bearer $TOKEN" "$API/api/ordens?size=1" | sed -E 's/.*"totalElements":([0-9]+).*/\1/')"
medir "GET /api/ordens (padrão, 50 por página)" "/api/ordens"
medir "Quadro: 6 status, 100 por página" "/api/ordens?status=ABERTA&status=AGUARDANDO_APROVACAO&status=APROVADA&status=EM_EXECUCAO&status=AGUARDANDO_PECA&status=CONCLUIDA&size=100&sort=dataAbertura,asc"
medir "Busca por placa/cliente (busca=fic)" "/api/ordens?busca=fic"
medir "Página 150 (OFFSET alto)" "/api/ordens?page=150&size=50"
