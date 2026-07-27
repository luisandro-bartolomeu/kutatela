# 📲 Guia Passo a Passo: Configuração do WhatsApp no Kutatela Mama 🌿

> **Número Oficial do Assistente:** `+244 936245610`  
> **Backend API:** Kutatela Mama (Spring Boot 3.4.2)  
> **Servidor WhatsApp:** Gowa / Whatsmeow (Go)  
> **URL do Webhook Público:** `https://kutatela-production.up.railway.app/api/v1/whatsapp/webhook`

---

## 📋 Visão Geral da Integração

O número **+244 936245610** atuará como o **atendente virtual e assistente de saúde oficial do Kutatela Mama**. 
Quando uma mãe angolana enviar uma mensagem para este número:
1. O servidor **Gowa/Whatsmeow** recebe o pacote do WhatsApp via WebSocket.
2. O Gowa encaminha o payload em JSON para a API do Kutatela Mama (`/api/v1/whatsapp/webhook`).
3. O Kutatela Mama processa a intenção (menu de vacinação, dicas semanais ou **triagem de sintomas com Inteligência Artificial Gemini**).
4. O Kutatela Mama envia a resposta de volta ao WhatsApp da mãe através do Gowa (`/send-message`).

---

## 🚀 Passo a Passo para Ativar o Número +244 936245610

### 1️⃣ Iniciar a API Kutatela Mama (Spring Boot)

Certifique-se de que a aplicação backend está a rodar com as variáveis de ambiente do Gowa:

* **Em ambiente local:**
  ```bash
  ./mvnw spring-boot:run
  ```
* **Em produção (AWS / Railway):**
  Defina as variáveis de ambiente no container/painel:
  ```env
  SPRING_PROFILES_ACTIVE=prod
  GOWA_API_URL=https://go-whatsapp-web-multidevice-production-4328.up.railway.app
  GOWA_ENABLED=true
  GEMINI_API_KEY=sua_chave_gemini_aqui
  ```

---

### 2️⃣ Iniciar o Servidor Gowa (Whatsmeow)

Inicie a sua instância do **Gowa/Whatsmeow** configurando a URL do webhook para apontar para a API do Kutatela Mama:

* **Em ambiente local (com ngrok ou localtunnel):**
  ```bash
  export WEBHOOK_URL="http://localhost:8080/api/v1/whatsapp/webhook"
  export PORT=3000
  ./gowa
  ```

* **Em produção no Railway (Docker Container `aldinokem/gowa:latest`):**
  1. No painel do **Railway**, adicione um novo serviço (Service ➔ Docker Image).
  2. Imagem Docker: `aldinokem/gowa:latest`
  3. Variáveis de ambiente no serviço do WhatsApp no Railway:
     ```env
     PORT=3000
     WHATSAPP_WEBHOOK_URL=https://kutatela-production.up.railway.app/api/v1/whatsapp/webhook
     ```
  4. Na aplicação Spring Boot (`kutatela-mama`), configure a variável `GOWA_API_URL` apontando para o serviço do WhatsApp.

---

### 3️⃣ Parear o Telemóvel +244 936245610 (Conexão do Aparelho)

1. Pegue no smartphone que possui o cartão SIM com o número **+244 936245610**.
2. Abra o **WhatsApp** nesse smartphone.
3. No canto superior direito, toque no **menu de 3 pontos** (ou **Definições** no iPhone).
4. Selecione **Aparelhos Conectados** (Linked Devices).
5. Toque em **Conectar um Aparelho**.
6. Aponte a câmara do telemóvel para o **QR Code** que é exibido no terminal do Gowa ou no painel web.
7. *Concluído!* O Gowa guardará as chaves de criptografia na base de dados e o número **+244 936245610** ficará ativo como o bot oficial.

---

### 4️⃣ Testar o Funcionamento em Tempo Real

Peça a uma mãe ou envie uma mensagem a partir de **qualquer outro número de WhatsApp** para o **+244 936245610**:

#### 🧪 Teste 1: Menu Principal
* **Mensagem enviada:** `MENU` ou `Olá`
* **Resposta esperada do Kutatela Mama:**
  ```text
  🌿 Olá! Bem-vinda ao Kutatela Mama 🌿
  Mãe: Maria
  ================================
  Escolha uma opção digitando o número:

  1️⃣ Calendário de Vacinação
  2️⃣ Triagem de Sintomas (IA)
  3️⃣ Dicas de Cuidados Semanais
  4️⃣ Registar / Atualizar Dados
  5️⃣ Sair
  ```

#### 🧪 Teste 2: Consultar Vacinas
* **Mensagem enviada:** `1`
* **Resposta esperada:** Retorna a lista de vacinas agendadas do bebé (BCG, Polio, Pentavalente, etc.).

#### 🧪 Teste 3: Triagem Inteligente com IA Gemini
* **Mensagem enviada em texto livre:** _"O meu bebé tem febre de 38.5º e diarreia"_
* **Resposta esperada:** A Inteligência Artificial analisa os sintomas em tempo real e devolve a resposta formatada:
  ```text
  🩺 Triagem de Saúde Kutatela Mama 🌿

  👤 Mãe: Ana Silva
  👶 Bebé: Bernardo (2 meses)
  --------------------------------
  🔍 Análise IA: Suspeita de infeção gastrointestinal ou reação febril...
  📋 Cuidados em Casa: Ofereça soro de reidratação oral, mantenha a amamentação...
  🏥 Recomendação: Procure o Posto de Saúde se a febre persistir por mais de 24h.
  ⚠️ Nível de Gravidade: WARNING (Atenção)
  ```

---

### 💡 Recomendações de Manutenção do Número

1. **Bateria e Internet:** Mantenha o telemóvel do número `+244 936245610` ligado à internet e com bateria (mesmo em modo multidispositivo).
2. **Reconexão:** Se a sessão cair por inatividade prolongada, basta abrir o Gowa e escanear o QR Code novamente. As informações de mães e bebés registradas **não serão perdidas**, pois ficam guardadas na base de dados do Kutatela Mama.
