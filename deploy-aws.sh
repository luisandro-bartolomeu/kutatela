#!/bin/bash
# ==============================================================================
# Script de Implantação Automatizada - Kutatela Mama na AWS EC2 / Lightsail
# ==============================================================================

set -e

echo "🌿 [Kutatela Mama] A iniciar implantação na AWS..."

# 1. Verificar instalação do Docker
if ! command -v docker &> /dev/null; then
    echo "📦 Docker não encontrado. A instalar Docker..."
    sudo apt-get update -y || sudo yum update -y
    sudo apt-get install -y docker.io docker-compose-plugin || sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
fi

# 2. Criar ficheiro .env se não existir
if [ ! -f .env ]; then
    echo "⚙️ A criar ficheiro .env a partir de .env.example..."
    cp .env.example .env
    echo "⚠️ Por favor edite o ficheiro .env com as suas chaves reais da Africa's Talking e Gemini se necessário."
fi

# 3. Compilar e subir os contentores Docker
echo "🚀 A compilar imagens Docker e iniciar serviços..."
docker compose down --remove-orphans || true
docker compose up -d --build

# 4. Verificar estado da aplicação
echo "⏳ A aguardar inicialização do Spring Boot e PostgreSQL..."
sleep 10

docker compose ps

echo "=============================================================================="
echo "✅ Implantação concluída com sucesso!"
echo "📍 API REST disponível em: http://$(curl -s ifconfig.me):8080/api/v1/mothers"
echo "📍 OpenAPI Swagger em:     http://$(curl -s ifconfig.me):8080/swagger-ui.html"
echo "📍 Endpoint Callback USSD: http://$(curl -s ifconfig.me):8080/ussd"
echo "=============================================================================="
