const state = {
  service: "working",
  ioportBusy: false,
  reservedSlot: null,
  slots: {
    slot1: "free",
    slot2: "free",
    slot3: "free",
    slot4: "free",
    slot5: "marker",
  },
};

const displayMessage = document.querySelector("#display-message");
const loadButton = document.querySelector("#load-button");
const serviceIndicator = document.querySelector("#service-indicator");
const ioportState = document.querySelector("#ioport-state");
const slotNodes = [...document.querySelectorAll(".slot")];

function setDisplay(message) {
  if (displayMessage) displayMessage.textContent = message;
}

function render() {
  if (serviceIndicator) {
    serviceIndicator.className = `status-pill ${state.service === "working" ? "working" : "out"}`;
    serviceIndicator.textContent = state.service === "working" ? "Service working" : "Out of service";
  }

  if (ioportState) {
    ioportState.className = `status-pill ${state.ioportBusy ? "busy" : "free"}`;
    ioportState.textContent = state.ioportBusy ? "IOPort occupato" : "IOPort libero";
  }

  if (loadButton) {
    loadButton.disabled = state.service !== "working" || state.ioportBusy;
  }

  slotNodes.forEach((node) => {
    const slot = node.dataset.slot;
    const value = state.slots[slot] || "free";
    node.className = `slot ${value}`;
    const strongTag = node.querySelector("strong");
    if (strongTag) {
      strongTag.textContent = value === "reserved" ? "riservato" : value === "occupied" ? "occupato" : value === "marker" ? "marker" : "libero";
    }
  });
}

const logBox = document.querySelector("#log-box");
const clearLogBtn = document.querySelector("#clear-log-btn");

function addLog(message, type = "info") {
  if (!logBox) return;
  const timeStr = new Date().toLocaleTimeString("it-IT", { hour12: false });
  const entry = document.createElement("div");
  entry.className = `log-entry ${type}`;
  entry.innerHTML = `<span class="log-time">[${timeStr}]</span> <span class="log-msg">${message}</span>`;
  logBox.appendChild(entry);
  logBox.scrollTop = logBox.scrollHeight;
}

if (clearLogBtn) {
  clearLogBtn.addEventListener("click", () => {
    if (logBox) logBox.innerHTML = "";
    addLog("Log pulito con successo.", "info");
  });
}

// Configurazione WebSocket per ricevere notifiche push dal server intermedio (facade CoAP->WS)
function setupWebSocket() {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = window.location.host || "localhost:8086";
  const wsUrl = `${protocol}//${host}/ws`;

  console.log("Connecting to WebSocket:", wsUrl);
  const ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log("WebSocket connected to IOPortServer");
    addLog("Connesso al server WebSocket (IOPortServer su :8086)", "success");
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log("State update from server:", data);

      let changesMsg = [];
      if (data.workingState && data.workingState !== (state.service === "working" ? "Service working" : "Out of service")) {
        changesMsg.push(`Servizio -> ${data.workingState}`);
        state.service = data.workingState === "Service working" ? "working" : "out";
      } else if (data.workingState) {
        state.service = data.workingState === "Service working" ? "working" : "out";
      }

      if (typeof data.ioPortOccupied === "boolean") {
        if (data.ioPortOccupied !== state.ioportBusy) {
          changesMsg.push(`IOPort -> ${data.ioPortOccupied ? "OCCUPATO" : "LIBERO"}`);
        }
        state.ioportBusy = data.ioPortOccupied;
      }

      if (data.reservedSlot !== undefined) {
        const newRes = data.reservedSlot > 0 ? `slot${data.reservedSlot}` : null;
        if (newRes !== state.reservedSlot) {
          changesMsg.push(`Slot riservato -> ${newRes || "Nessuno"}`);
        }
        state.reservedSlot = newRes;
      }

      if (data.slots && typeof data.slots === "object") {
        for (const [sName, sVal] of Object.entries(data.slots)) {
          if (state.slots[sName] !== sVal) {
            changesMsg.push(`${sName} -> ${sVal}`);
          }
        }
        state.slots = { ...state.slots, ...data.slots };
      }

      render();

      const summary = changesMsg.length > 0 ? `Variazione: ${changesMsg.join(" | ")}` : "Push ricevuto (stato inalterato)";
      addLog(`[CoAP->WS Push] ${summary}`, changesMsg.length > 0 ? "push" : "info");
    } catch (err) {
      console.error("Error parsing WebSocket message:", err, event.data);
      addLog(`Errore parse messaggio WebSocket: ${err.message}`, "error");
    }
  };

  ws.onclose = () => {
    console.warn("WebSocket closed. Reconnecting in 3 seconds...");
    addLog("Disconnesso dal server WebSocket. Riconnessione tra 3s...", "error");
    setTimeout(setupWebSocket, 3000);
  };

  ws.onerror = (err) => {
    console.error("WebSocket error:", err);
  };
}

async function sendLoadRequest() {
  if (state.service !== "working" || state.ioportBusy) {
    setDisplay("retrylater: il sistema non puo accettare ora una nuova richiesta.");
    addLog("Richiesta bloccata dal client: servizio offline o IOPort occupato.", "error");
    return;
  }

  setDisplay("Inviando richiesta load_request al cargoservice...");
  addLog("Invio HTTP POST /api/load al server intermedio...", "request");
  try {
    const response = await fetch("/api/load", {
      method: "POST",
      headers: { "Content-Type": "application/json" }
    });

    if (!response.ok) {
      setDisplay("retrylater: server temporaneamente non disponibile o errore http.");
      addLog(`Errore HTTP ${response.status} da /api/load`, "error");
      return;
    }

    const resJson = await response.json();
    console.log("POST /api/load result:", resJson);

    if (resJson.status === "accepted") {
      const assignedSlot = resJson.slot || "slot1";
      state.reservedSlot = assignedSlot;
      if (state.slots[assignedSlot]) state.slots[assignedSlot] = "reserved";
      setDisplay(`accepted: container assegnato a ${assignedSlot}. Posizionare il container sul sensore (IOPort).`);
      addLog(`[Risposta /api/load] ACCEPTED: Assegnato a <strong>${assignedSlot}</strong>. In attesa del sonar sul IOPort.`, "success");
    } else if (resJson.status === "retrylater") {
      setDisplay("retrylater: il sistema non puo accettare ora una nuova richiesta (occupato o fuori servizio).");
      addLog("[Risposta /api/load] RETRY LATER: Sistema occupato o fuori servizio.", "info");
    } else if (resJson.status === "refused") {
      setDisplay("refused: hold piena, nessuno slot libero disponibile.");
      addLog("[Risposta /api/load] REFUSED: Nessuno slot libero nella stiva.", "error");
    } else {
      setDisplay(`Errore o risposta sconosciuta: ${resJson.status}`);
      addLog(`[Risposta /api/load] Esito sconosciuto: ${resJson.status}`, "info");
    }
    render();
  } catch (err) {
    console.error("Errore durante la fetch /api/load:", err);
    setDisplay("Errore di connessione al server intermedio. Riprovare.");
    addLog(`Errore di rete su POST /api/load: ${err.message}`, "error");
  }
}

if (loadButton) {
  loadButton.addEventListener("click", sendLoadRequest);
}

render();
setupWebSocket();
