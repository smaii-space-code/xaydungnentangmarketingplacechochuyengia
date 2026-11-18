document.querySelectorAll("[data-print-contract]").forEach(button => {
  button.addEventListener("click", () => window.print());
});

const signaturePad = document.querySelector("[data-signature-pad]");
if (signaturePad) {
  const form = signaturePad.closest("[data-signature-form]");
  const field = form.elements.signature;
  const clear = form.querySelector("[data-clear-signature]");
  const error = form.querySelector("[data-signature-error]");
  const context = signaturePad.getContext("2d");
  const maxPoints = 900;
  let strokes = [];
  let current = [];
  let activePointer = null;
  let points = 0;
  let ink = 0;

  context.strokeStyle = "#14264d";
  context.lineWidth = 2.5;
  context.lineCap = "round";
  context.lineJoin = "round";

  function position(event) {
    const bounds = signaturePad.getBoundingClientRect();
    return [
      Math.max(0, Math.min(signaturePad.width - 1, Math.round((event.clientX - bounds.left) * signaturePad.width / bounds.width))),
      Math.max(0, Math.min(signaturePad.height - 1, Math.round((event.clientY - bounds.top) * signaturePad.height / bounds.height)))
    ];
  }

  signaturePad.addEventListener("pointerdown", event => {
    if (activePointer !== null || event.button !== 0 || points >= maxPoints) return;
    activePointer = event.pointerId;
    signaturePad.setPointerCapture(activePointer);
    current = [position(event)];
    error.hidden = true;
  });

  function extend(event) {
    if (event.pointerId !== activePointer || points + current.length >= maxPoints) return;
    const [x, y] = position(event);
    const [previousX, previousY] = current.at(-1);
    const distance = Math.hypot(x - previousX, y - previousY);
    if (distance < 2) return;
    ink += distance;
    current.push([x, y]);
    context.beginPath();
    context.moveTo(previousX, previousY);
    context.lineTo(x, y);
    context.stroke();
  }

  function finish(event) {
    if (event.pointerId !== activePointer) return;
    if (current.length > 1) {
      strokes.push(current);
      points += current.length;
    }
    activePointer = null;
    current = [];
  }

  signaturePad.addEventListener("pointermove", extend);
  signaturePad.addEventListener("pointerup", event => { extend(event); finish(event); });
  signaturePad.addEventListener("pointercancel", finish);
  signaturePad.addEventListener("lostpointercapture", finish);
  clear.addEventListener("click", () => {
    if (activePointer !== null) return;
    context.clearRect(0, 0, signaturePad.width, signaturePad.height);
    strokes = [];
    points = 0;
    ink = 0;
    error.hidden = true;
  });
  form.addEventListener("submit", event => {
    if (activePointer !== null || ink < 12) {
      event.preventDefault();
      error.textContent = "Vui lòng ký tên trong ô chữ ký.";
      error.hidden = false;
      signaturePad.scrollIntoView({ block: "center" });
      return;
    }
    field.value = strokes.map(stroke => stroke.map(point => point.join(",")).join(" ")).join(";");
  });
}
