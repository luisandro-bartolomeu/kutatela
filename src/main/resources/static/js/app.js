// Global Application State
let currentBabies = [];
let ussdSessionId = "sim_session_" + Date.now();
let ussdCurrentPhone = "+244923111222";
let ussdPath = "";

document.addEventListener("DOMContentLoaded", () => {
    loadDashboardData();
    // Auto-refresh stats every 30 seconds
    setInterval(loadDashboardData, 30000);
});

async function loadDashboardData() {
    try {
        const ageFilter = document.getElementById("selectAgeFilter").value;
        const statusFilter = document.getElementById("selectStatusFilter").value;
        const provinceFilter = document.getElementById("selectProvinceFilter").value;

        // Fetch Summary Stats
        const summaryRes = await fetch("/api/v1/dashboard/summary");
        if (summaryRes.ok) {
            const summary = await summaryRes.json();
            renderSummaryMetrics(summary);
        }

        // Fetch Babies List from Database
        let babiesUrl = "/api/v1/dashboard/babies?";
        if (ageFilter !== "") babiesUrl += `ageMonths=${ageFilter}&`;
        if (statusFilter) babiesUrl += `status=${statusFilter}&`;
        if (provinceFilter) babiesUrl += `province=${encodeURIComponent(provinceFilter)}&`;

        const babiesRes = await fetch(babiesUrl);
        if (babiesRes.ok) {
            currentBabies = await babiesRes.json();
            renderBabiesTable(currentBabies);
        }
    } catch (err) {
        console.error("Erro ao carregar dados do painel:", err);
    }
}

function renderSummaryMetrics(summary) {
    document.getElementById("valTotalBabies").innerText = summary.totalBabies || 0;
    document.getElementById("valTotalMothers").innerText = `${summary.totalMothers || 0} Mães Cadastradas`;
    document.getElementById("valWellVaccinated").innerText = summary.wellVaccinatedBabies || 0;
    document.getElementById("valPendingVaccinated").innerText = summary.pendingVaccinatedBabies || 0;
    
    const rate = summary.vaccinationRate || 0;
    document.getElementById("valVaccinationRate").innerText = `${rate}%`;
    document.getElementById("progressBarRate").style.width = `${rate}%`;
}

function renderBabiesTable(babies) {
    const tbody = document.getElementById("tableBabiesBody");
    const countLbl = document.getElementById("lblRecordsCount");
    
    const searchTerm = document.getElementById("inputSearch").value.toLowerCase().trim();
    
    const filtered = babies.filter(b => {
        if (!searchTerm) return true;
        const matchName = (b.fullName || "").toLowerCase().includes(searchTerm);
        const matchMother = (b.motherName || "").toLowerCase().includes(searchTerm);
        const matchPhone = (b.motherPhone || "").toLowerCase().includes(searchTerm);
        const matchProv = (b.province || "").toLowerCase().includes(searchTerm);
        return matchName || matchMother || matchPhone || matchProv;
    });

    countLbl.innerText = `${filtered.length} Bebé${filtered.length === 1 ? '' : 's'}`;

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-4 text-muted">
                    <i class="fa-solid fa-folder-open fa-2x"></i>
                    <p class="mt-2">Nenhum bebé encontrado na base de dados com os filtros aplicados.</p>
                </td>
            </tr>
        `;
        return;
    }

    let html = "";
    filtered.forEach(b => {
        const isWell = b.wellVaccinated;
        const statusBadge = isWell
            ? `<span class="badge badge-success"><i class="fa-solid fa-circle-check"></i> Bem Vacinado (${b.ageInMonths}M)</span>`
            : `<span class="badge badge-danger"><i class="fa-solid fa-circle-exclamation"></i> ${b.pendingVaccinesCount} Vacina(s) em Falta</span>`;

        let vaccinesListHtml = "";
        if (!isWell && b.pendingVaccines && b.pendingVaccines.length > 0) {
            const listStr = b.pendingVaccines.map(pv => `<strong class="text-danger">• [${pv.recommendedAgeMonths}M] ${pv.vaccineName}</strong>`).join("<br>");
            vaccinesListHtml = `<div style="font-size: 0.85rem;">${listStr}</div>`;
        } else {
            vaccinesListHtml = `<span class="text-success" style="font-size: 0.85rem;"><i class="fa-solid fa-check"></i> ${b.completedVaccinesCount} vacinas concluídas</span>`;
        }

        html += `
            <tr>
                <td>
                    <strong>${escapeHtml(b.fullName)}</strong>
                    <div style="font-size: 0.82rem; color: #64748b;">
                        <span><i class="fa-solid fa-cake-candles"></i> ${b.ageInMonths} Meses</span>
                        <span class="ms-2">(${b.gender === 'F' ? 'Feminino' : 'Masculino'})</span>
                    </div>
                </td>
                <td>
                    <div><strong>${escapeHtml(b.motherName || 'Mãe')}</strong></div>
                    <div style="font-size: 0.82rem; color: #0284c7;">
                        <i class="fa-solid fa-phone"></i> ${escapeHtml(b.motherPhone || '-')}
                    </div>
                </td>
                <td>
                    <div>${escapeHtml(b.province || 'Luanda')}</div>
                    <div style="font-size: 0.8rem; color: #64748b;">${escapeHtml(b.municipality || '')}</div>
                </td>
                <td>${statusBadge}</td>
                <td>${vaccinesListHtml}</td>
                <td>
                    <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
                        <button class="btn btn-primary btn-action" onclick="openSendAlertModal(${b.id})">
                            <i class="fa-brands fa-whatsapp"></i> Alerta GOWA
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

function applyFilters() {
    renderBabiesTable(currentBabies);
}

function setAgeTab(btn, ageValue) {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById("selectAgeFilter").value = ageValue;
    loadDashboardData();
}

/* Modals Management */
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove("active");
}

/* Send Individual Alert Modal Logic */
function openSendAlertModal(babyId) {
    const baby = currentBabies.find(b => b.id === babyId);
    if (!baby) return;

    document.getElementById("alertBabyId").value = baby.id;
    document.getElementById("alertMotherInfo").value = `${baby.motherName} (${baby.motherPhone})`;

    let pendingList = "";
    if (baby.pendingVaccines && baby.pendingVaccines.length > 0) {
        pendingList = baby.pendingVaccines.map(v => v.vaccineName).join(", ");
    }

    const defaultMsg = `Kutatela Mama 🌿: Olá ${baby.motherName}, lembramos que o(a) bebé ${baby.fullName} (${baby.ageInMonths} meses) possui vacinas pendentes para o seu mês: ${pendingList || 'Vacinas do mês'}. Por favor dirija-se ao posto de saúde da sua localidade (${baby.province || 'Luanda'}).`;
    
    document.getElementById("alertMessageText").value = defaultMsg;
    document.getElementById("modalAlert").classList.add("active");
}

async function handleSendAlertSubmit(event) {
    event.preventDefault();
    const btn = document.getElementById("btnSubmitAlert");
    btn.disabled = true;
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> A Enviar via GOWA...`;

    try {
        const babyId = parseInt(document.getElementById("alertBabyId").value);
        const customMessage = document.getElementById("alertMessageText").value;
        const channel = document.querySelector('input[name="alertChannel"]:checked').value;

        const res = await fetch("/api/v1/dashboard/send-alert", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ babyId, customMessage, channel })
        });

        if (res.ok) {
            const data = await res.json();
            alert(`✅ Alerta enviado com sucesso via GOWA WhatsApp (${channel}) para ${data.recipientPhone}!`);
            closeModal("modalAlert");
        } else {
            alert("❌ Falha ao enviar alerta. Verifique a ligação com o GOWA.");
        }
    } catch (err) {
        console.error("Erro ao enviar alerta:", err);
        alert("❌ Ocorreu um erro na ligação com o servidor.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="fa-solid fa-paper-plane"></i> Enviar Alerta GOWA`;
    }
}

/* Global Alert Modal Logic for All Overdue Mothers */
function openGlobalAlertModal() {
    document.getElementById("modalGlobalAlert").classList.add("active");
}

async function handleSendGlobalAlertSubmit(event) {
    event.preventDefault();
    const btn = document.getElementById("btnSubmitGlobalAlert");
    btn.disabled = true;
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> A Disparar Alerta Global...`;

    try {
        const customMessage = document.getElementById("globalAlertMessageText").value;

        const res = await fetch("/api/v1/dashboard/send-global-alert", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ channel: "WHATSAPP", message: customMessage })
        });

        if (res.ok) {
            const data = await res.json();
            alert(`📢 ALERTA GLOBAL DISPARADO!\n\n${data.message}\n• Mães Notificadas: ${data.mothersNotified}\n• Bebés com Vacinas em Falta: ${data.totalBabiesWithPending}`);
            closeModal("modalGlobalAlert");
        } else {
            alert("❌ Falha ao disparar alerta global.");
        }
    } catch (err) {
        console.error("Erro ao disparar alerta global:", err);
        alert("❌ Ocorreu um erro de ligação.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="fa-solid fa-bullhorn"></i> Disparar Alerta Global Agora`;
    }
}

/* USSD Interactive Simulator */
function openUssdSimulatorModal() {
    ussdPath = "";
    document.getElementById("modalUssd").classList.add("active");
    callUssdApi("");
}

function quickUssd(text) {
    ussdPath = text;
    document.getElementById("ussdInputText").value = "";
    callUssdApi(ussdPath);
}

function sendUssdCommand() {
    const input = document.getElementById("ussdInputText");
    const val = input.value.trim();
    if (!val) return;

    if (ussdPath) {
        ussdPath += "*" + val;
    } else {
        ussdPath = val;
    }
    input.value = "";
    callUssdApi(ussdPath);
}

async function callUssdApi(text) {
    const display = document.getElementById("ussdDisplay");
    display.innerText = "A carregar de *123#...";

    try {
        const bodyData = new URLSearchParams();
        bodyData.append("sessionId", ussdSessionId);
        bodyData.append("serviceCode", "*123#");
        bodyData.append("phoneNumber", ussdCurrentPhone);
        bodyData.append("text", text);

        const res = await fetch("/ussd", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: bodyData.toString()
        });

        if (res.ok) {
            const respText = await res.text();
            display.innerText = respText;
            if (respText.startsWith("END")) {
                ussdPath = ""; // Reset session on END
            }
        } else {
            display.innerText = "END Erro ao comunicar com o gateway USSD.";
        }
    } catch (err) {
        display.innerText = "END Erro de rede ao simular USSD.";
    }
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}
