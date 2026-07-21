# ☁️ Guia de Implantação na AWS (Amazon Web Services) — Kutatela Mama

Este guia detalha o processo passo a passo para fazer o deploy da aplicação **Kutatela Mama** (API REST + Gateway USSD + PostgreSQL) numa instância **AWS EC2** ou **AWS Lightsail** utilizando Docker e Docker Compose.

---

## 📋 Pré-requisitos na AWS

1. Uma conta ativa na **AWS**.
2. Uma instância **EC2** ou **Lightsail** criada:
   - **SO Recomendado**: Ubuntu 22.04 LTS ou Amazon Linux 2023.
   - **Tamanho Mínimo Recomendado**: `t3.small` ou `t2.small` (2 GB RAM) para acomodar a JVM do Spring Boot e o PostgreSQL.
3. **Grupo de Segurança (Security Group)** configurado com os seguintes acessos (Inbound Rules):
   - `HTTP` (Porta `80`) — Para tráfego web / reverse proxy.
   - `Custom TCP` (Porta `8080`) — Porta direta da aplicação Spring Boot.
   - `SSH` (Porta `22`) — Para acesso remoto.

---

## 🛠️ Passo 1: Conectar à Instância EC2 via SSH

No seu terminal local:
```bash
ssh -i /caminho/para/sua-chave.pem ubuntu@IP_PUBLICO_DA_SUA_INSTANCIA
```

---

## 🚀 Passo 2: Clonar ou Transferir o Código para a Instância

Pode enviar o projeto via Git ou SCP:
```bash
git clone https://github.com/seu-usuario/kutatela-mama.git
cd kutatela-mama
```

---

## ⚙️ Passo 3: Configurar Variáveis de Ambiente

Crie o ficheiro `.env` baseado no exemplo:
```bash
cp .env.example .env
nano .env
```

Ajuste as variáveis de produção conforme necessário:
```env
SPRING_PROFILES_ACTIVE=prod
POSTGRES_DB=kutatela_db
POSTGRES_USER=kutatela_user
POSTGRES_PASSWORD=sua_palavra_passe_segura_2026

AFRICASTALKING_USERNAME=seu_username
AFRICASTALKING_API_KEY=sua_api_key
AFRICASTALKING_ENABLED=true

GEMINI_API_KEY=sua_api_key_do_gemini
```

---

## 🐋 Passo 4: Implantação com Docker Compose

Execute o script de automação:
```bash
chmod +x deploy-aws.sh
./deploy-aws.sh
```

Ou execute diretamente via Docker Compose:
```bash
docker compose up -d --build
```

---

## 🔍 Passo 5: Verificar o Estado dos Contentores e Logs

Para verificar se os contentores estão em execução:
```bash
docker compose ps
```

Para consultar os logs em tempo real da aplicação:
```bash
docker compose logs -f app
```

---

## 🌐 Endpoints Públicos de Teste na AWS

Após a inicialização, a API estará acessível em:

- **Swagger UI (Documentação Interativa)**:  
  `http://<IP-PUBLICO-AWS>:8080/swagger-ui.html`

- **Endpoint Callback USSD (Africa's Talking)**:  
  `http://<IP-PUBLICO-AWS>:8080/ussd`

- **APIs REST de Mães & Bebés**:  
  `http://<IP-PUBLICO-AWS>:8080/api/v1/mothers`  
  `http://<IP-PUBLICO-AWS>:8080/api/v1/triages`  
  `http://<IP-PUBLICO-AWS>:8080/api/v1/vaccinations/national-calendar`

---

## 🔒 Opção Recomendada: Configurar Domínio e SSL Gratuito (HTTPS) com Nginx & Certbot

A plataforma Africa's Talking exige URLs `HTTPS` seguras em ambiente de produção. Pode facilmente configurar Nginx + Let's Encrypt:

1. Instalar Nginx e Certbot:
   ```bash
   sudo apt-get install -y nginx certbot python3-certbot-nginx
   ```
2. Redirecionar o tráfego do domínio na porta 80/443 para `localhost:8080`.
3. Executar o Certbot para obter SSL automático:
   ```bash
   sudo certbot --nginx -d ussd.kutatelamama.ao
   ```
