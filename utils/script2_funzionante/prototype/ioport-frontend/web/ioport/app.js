// ─── Stato da inviare in JSON ──────────────────────────────────────────────────
const state = {
  working:     true,   // workingState: "Service working" | "Out of service"
  engaged:     false,  // serviceState: "engaged" | "disengaged"
  ioportBusy:  false,
  reservedSlot: null,
  slots: { slot1: "free", slot2: "free", slot3: "free", slot4: "free", slot5: "marker" },
};

// ─── DOM refs ─────────────────────────────────────────────────────────────────
const displayMessage    = document.querySelector("#display-message");
const reservationTimer  = document.querySelector("#reservation-timer");
const loadButton        = document.querySelector("#load-button");
const serviceIndicator  = document.querySelector("#service-indicator");
const ioportStateEl     = document.querySelector("#ioport-state");
const slotNodes         = [...document.querySelectorAll(".slot")];
const logBox            = document.querySelector("#log-box");
const clearLogBtn       = document.querySelector("#clear-log-btn");
let reservationInterval = null;

// ─── Display: SOLO i messaggi previsti dai requisiti ─────────────────────────
function setDisplay(message) {
  if (displayMessage) displayMessage.textContent = message;
}

function stopReservationTimer() {
  if (reservationInterval) {
    clearInterval(reservationInterval);
    reservationInterval = null;
  }
  if (reservationTimer) reservationTimer.hidden = true;
}

function startReservationTimer(slot) {
  stopReservationTimer();
  const slotLabel = String(slot).startsWith("slot") ? slot : `slot${slot}`;
  let secondsLeft = 30;

  const updateTimer = () => {
    if (!reservationTimer) return;
    reservationTimer.hidden = false;
    reservationTimer.textContent =
      `${secondsLeft}s - ${slotLabel} riservato. Posizionare il container nell'area IOPort prima della scadenza del timer`;
  };

  updateTimer();
  reservationInterval = setInterval(() => {
    secondsLeft -= 1;
    if (secondsLeft <= 0) {
      stopReservationTimer();
      return;
    }
    updateTimer();
  }, 1000);
}

// ─── Render: ricava l'UI dallo stato ─────────────────────────────────────────
function render() {
  // Status pill: "Service working" | "Out of service"  (req.)
  if (serviceIndicator) {
    serviceIndicator.className = `status-pill ${state.working ? "working" : "out"}`;
    serviceIndicator.textContent = state.working ? "Service working" : "Out of service";
  }

  // IOPort state pill
  if (ioportStateEl) {
    ioportStateEl.className = `status-pill ${state.ioportBusy ? "busy" : "free"}`;
    ioportStateEl.textContent = state.ioportBusy ? "IOPort occupato" : "IOPort libero";
  }

  // Pulsante: disabilitato se sistema engaged
  if (loadButton) {
    loadButton.disabled = state.engaged;
  }

  // Griglia hold: stato corrente degli slot (req.)
  slotNodes.forEach((node) => {
    const slot  = node.dataset.slot;
    const value = state.slots[slot] || "free";
    node.className = `slot ${value}`;
    const strongTag = node.querySelector("strong");
    if (strongTag) {
      strongTag.textContent =
        value === "reserved" ? "riservato" :
        value === "occupied"  ? "occupato"  :
        value === "marker"    ? "marker"    : "libero";
    }
  });
}

// ─── Log box (terminale di log) ───────────────────────────────────────────────
function addLog(message, type = "info") {
  if (!logBox) return;
  const timeStr = new Date().toLocaleTimeString("it-IT", { hour12: false });
  const entry = document.createElement("div");
  entry.className = `log-entry ${type}`;
  entry.innerHTML =
    `<span class="log-time">[${timeStr}]</span> <span class="log-msg">${message}</span>`;
  logBox.appendChild(entry);
  logBox.scrollTop = logBox.scrollHeight;
}

if (clearLogBtn) {
  clearLogBtn.addEventListener("click", () => {
    if (logBox) logBox.innerHTML = "";
    addLog("Log pulito.", "info");
  });
}

// ─── WebSocket: riceve push CoAP -> aggiorna stato e display ──────────────────
function setupWebSocket() {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const host = window.location.host || "localhost:8086";
  const ws = new WebSocket(`${protocol}//${host}/ws`);

  ws.onopen = () => {
    addLog("Connesso al server (WebSocket attivo).", "success");
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      const changes = [];

      // serviceState: "engaged" | "disengaged"
      if (data.serviceState !== undefined) {
        const newEngaged = data.serviceState === "engaged";
        if (newEngaged !== state.engaged) {
          changes.push(`stato servizio: ${data.serviceState}`);
          state.engaged = newEngaged;
        }
      }

      // workingState: "Service working" | "Out of service"  (req: display)
      if (data.workingState !== undefined) {
        const newWorking = data.workingState === "Service working";
        if (newWorking !== state.working) {
          setDisplay(data.workingState);       // messaggio previsto da requisiti
          changes.push(data.workingState);
          state.working = newWorking;
        }
      }

      // ioPortOccupied
      if (typeof data.ioPortOccupied === "boolean") {
        if (data.ioPortOccupied !== state.ioportBusy) {
          changes.push(`IOPort: ${data.ioPortOccupied ? "occupato" : "libero"}`);
          state.ioportBusy = data.ioPortOccupied;
        }
      }

      // reservedSlot
      if (data.reservedSlot !== undefined) {
        const newRes = data.reservedSlot > 0 ? `slot${data.reservedSlot}` : null;
        if (newRes !== state.reservedSlot) {
          changes.push(`slot riservato: ${newRes ?? "—"}`);
          state.reservedSlot = newRes;
        }
      }

      // slots
      if (data.slots && typeof data.slots === "object") {
        for (const [k, v] of Object.entries(data.slots)) {
          if (state.slots[k] !== v) changes.push(`${k}: ${v}`);
        }
        state.slots = { ...state.slots, ...data.slots };
      }

      render();

      if (changes.length > 0) {
        addLog(`[push] ${changes.join(" | ")}`, "push");
      }

    } catch (err) {
      addLog(`Errore parsing messaggio WebSocket: ${err.message}`, "error");
    }
  };

  ws.onclose = () => {
    addLog("WebSocket chiuso. Riconnessione tra 3s…", "error");
    setTimeout(setupWebSocket, 3000);
  };
}

// ─── Pulsante LOAD -> HTTP POST -> cargoservice  (req: pushbutton -> load_request)
async function sendLoadRequest() {
  try {
    const response = await fetch("/api/load", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });

    const resJson = await response.json();

    // Messaggi sul display: SOLO quelli previsti dai requisiti
    if (resJson.status === "accepted") {
      setDisplay("Service working");
      startReservationTimer(resJson.slot);
      addLog(`Accettato: ${resJson.slot}`, "success");
    } else if (resJson.status === "retrylater") {
      setDisplay("retrylater");
      addLog("Risposta: retrylater", "info");
    } else if (resJson.status === "refused") {
      setDisplay("refused");
      addLog("Risposta: refused (hold piena)", "error");
    }

    render();
  } catch (err) {
    addLog(`Errore di rete: ${err.message}`, "error");
  }
}

loadButton?.addEventListener("click", sendLoadRequest);

// ─── Init ─────────────────────────────────────────────────────────────────────
render();
setupWebSocket();
