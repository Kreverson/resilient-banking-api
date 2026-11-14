#!/usr/bin/env bash
# Gera as tres configuracoes do gateway para o experimento de isolamento por cliente:
#   random.conf  - sem isolamento: qualquer parceiro cai em qualquer um dos 8 nos (e o retry espalha o estrago)
#   cells.conf   - bulkhead em celulas: 4 grupos fixos de 2 nos, cada parceiro preso a um grupo
#   shuffle.conf - shuffle sharding: cada parceiro ganha um par de nos sorteado entre as 28 combinacoes possiveis
# O parceiro sai do caminho /v1/partners/{id}/...; o split_clients faz o sorteio por hash, sempre igual para o mesmo id.
set -euo pipefail
cd "$(dirname "$0")"
NODES=8

server() { echo "        server node$1:8080 max_fails=1 fail_timeout=30s;"; }

header() {
  cat <<'EOF'
# Arquivo gerado por generate-nginx.sh
worker_processes auto;
events { worker_connections 8192; }

http {
    access_log off;

    map $uri $partner {
        ~^/v1/partners/(?<id>[^/]+)/ $id;
        default "";
    }
EOF
}

footer() {
  local group=$1 tries=$2
  cat <<EOF

    server {
        listen 80;

        location /v1/partners/ {
            proxy_pass http://${group};
            # Sem isso o Host vira o nome do upstream (all_nodes, shard_1_2) e o Tomcat recusa o underscore com 400.
            proxy_set_header Host \$host;
            # POST tambem e repetido no proximo no quando a conexao falha (o no caiu antes de responder).
            proxy_next_upstream error timeout http_502 http_503 non_idempotent;
            proxy_next_upstream_tries ${tries};
            proxy_connect_timeout 1s;
            proxy_read_timeout 3s;
            add_header X-Upstream \$upstream_addr always;
        }

        location /gateway/health {
            return 200 "ok";
        }
    }
}
EOF
}

# --- sem isolamento
{
  header
  echo
  echo "    upstream all_nodes {"
  echo "        random;"
  for n in $(seq 1 $NODES); do server "$n"; done
  echo "    }"
  footer all_nodes 3
} > random.conf

# --- celulas fixas de 2 nos
{
  header
  echo
  echo '    split_clients "${partner}" $cell {'
  echo "        25% cell1;"
  echo "        25% cell2;"
  echo "        25% cell3;"
  echo "        *   cell4;"
  echo "    }"
  for c in 1 2 3 4; do
    echo
    echo "    upstream cell$c {"
    server $((2 * c - 1))
    server $((2 * c))
    echo "    }"
  done
  footer '$cell' 2
} > cells.conf

# --- shuffle sharding: todos os pares de 8 nos (28)
pairs=()
for a in $(seq 1 $NODES); do
  for b in $(seq $((a + 1)) $NODES); do
    pairs+=("$a-$b")
  done
done
{
  header
  echo
  echo '    split_clients "${partner}" $shard {'
  last=$((${#pairs[@]} - 1))
  for i in "${!pairs[@]}"; do
    if [ "$i" -eq "$last" ]; then
      echo "        *     shard_${pairs[$i]/-/_};"
    else
      echo "        3.57% shard_${pairs[$i]/-/_};"
    fi
  done
  echo "    }"
  for p in "${pairs[@]}"; do
    echo
    echo "    upstream shard_${p/-/_} {"
    server "${p%-*}"
    server "${p#*-}"
    echo "    }"
  done
  footer '$shard' 2
} > shuffle.conf

echo "gerados: random.conf cells.conf shuffle.conf (${#pairs[@]} pares)"
