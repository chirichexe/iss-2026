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

function firstFreeSlot() {
  return Object.entries(state.slots).find(([slot, value]) => slot !== "slot5" && value === "free")?.[0] ?? null;
}

function setDisplay(message) {
  displayMessage.textContent = message;
}

function render() {
  serviceIndicator.className = `status-pill ${state.service === "working" ? "working" : "out"}`;
  serviceIndicator.textContent = state.service === "working" ? "Service working" : "Out of service";

  ioportState.className = `status-pill ${state.ioportBusy ? "busy" : "free"}`;
  ioportState.textContent = state.ioportBusy ? "IOPort occupato" : "IOPort libero";

  loadButton.disabled = state.service !== "working" || state.ioportBusy;

  slotNodes.forEach((node) => {
    const slot = node.dataset.slot;
    const value = state.slots[slot];
    node.className = `slot ${value}`;
    node.querySelector("strong").textContent = value === "reserved" ? "riservato" : value === "occupied" ? "occupato" : value === "marker" ? "marker" : "libero";
  });
}

async function sendLoadRequest() {
  // Placeholder per l'integrazione Javalin: sostituire questa simulazione con fetch("/api/load").
  const slot = firstFreeSlot();

  if (state.service !== "working" || state.ioportBusy) {
    setDisplay("retrylater: il sistema non puo accettare ora una nuova richiesta.");
    return;
  }

  if (!slot) {
    setDisplay("refused: hold piena.");
    return;
  }

  state.reservedSlot = slot;
  state.slots[slot] = "reserved";
  state.ioportBusy = true;
  setDisplay(`accepted: container assegnato a ${slot}. Posizionare il container sul sensore.`);
  render();

  window.setTimeout(() => {
    state.slots[slot] = "occupied";
    state.ioportBusy = false;
    setDisplay(`Service working. Carico completato su ${slot}.`);
    render();
  }, 3000);
}

loadButton.addEventListener("click", sendLoadRequest);
render();
