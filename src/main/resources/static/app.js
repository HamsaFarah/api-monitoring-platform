const API = "";
const $ = (id) => document.getElementById(id);
const state = { endpoints: [], incidents: [], view: "overview", historyEndpointId: null, historyPage: 0, historyPages: 0, historySize: 15, editingId: null };
const esc = (value) => String(value ?? "").replace(/[&<>"']/g, c => ({ "&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;" }[c]));
const date = value => value ? new Date(value).toLocaleString() : "—";
const shortDate = value => value ? new Date(value).toLocaleDateString() : "—";
const endpointName = id => state.endpoints.find(e => e.id === id)?.name || `Endpoint #${id}`;
const pill = (status, label) => `<span class="pill ${esc(status.toLowerCase())}">${esc(label || status)}</span>`;
function toast(message, type="success") {
  const el = document.createElement("div"); el.className = `toast ${type}`; el.textContent = message;
  $("toast-region").append(el); setTimeout(() => el.remove(), 4200);
}
async function api(path, options={}) {
  let response;
  try {
    response = await fetch(`${API}${path}`, { ...options, headers: { ...(options.body ? {"Content-Type":"application/json"} : {}), ...(options.headers || {}) } });
  } catch (_) { $("api-state").textContent = "Offline"; throw new Error("Cannot reach the API. Check that the Spring Boot server is running."); }
  if (response.status === 204) return null;
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch (_) { data = raw; }
  if (!response.ok) throw new Error(data?.message || data?.detail || `Request failed (${response.status})`);
  return data;
}
function setView(view) {
  state.view = view;
  document.querySelectorAll(".view").forEach(v => v.classList.add("hidden"));
  $(`view-${view}`).classList.remove("hidden");
  document.querySelectorAll(".nav-item").forEach(b => b.classList.toggle("active", b.dataset.view === view));
  $("page-title").textContent = ({overview:"Overview",endpoints:"Monitored endpoints",incidents:"Incident center"})[view];
  $("add-endpoint").classList.toggle("hidden", view === "incidents");
  if (view === "endpoints") renderEndpoints();
  if (view === "incidents") loadIncidents();
}
function renderOverview() {
  const endpoints = state.endpoints;
  $("overview-endpoints").innerHTML = endpoints.length ? endpoints.slice(0,6).map(e => {
    const last = e.latestCheck;
    return `<tr><td><div class="endpoint-name">${esc(e.name)}</div><div class="endpoint-url">${esc(e.url)}</div></td><td><span class="method">${esc(e.method)}</span></td><td>${pill(e.enabled ? (last ? (last.success?"up":"down") : "disabled") : "disabled", !e.enabled?"Paused":!last?"No checks":last.success?"Operational":"Failing")}</td><td>${last?esc(date(last.checkedAt)):"—"}</td><td>${last?.latencyMs != null ? `${last.latencyMs} ms`:"—"}</td><td><button class="action-btn" data-action="check" data-id="${e.id}">Check now</button></td></tr>`;
  }).join("") : `<tr><td colspan="6" class="empty">No endpoints yet. Add your first monitor to get started.</td></tr>`;
}
function renderEndpoints() {
  const term = ($("endpoint-search").value || "").toLowerCase();
  const filtered = state.endpoints.filter(e => `${e.name} ${e.url}`.toLowerCase().includes(term));
  $("endpoint-count").textContent = `${filtered.length} of ${state.endpoints.length} endpoints`;
  $("endpoint-rows").innerHTML = filtered.length ? filtered.map(e => `<tr>
    <td><div class="endpoint-name">${esc(e.name)}</div><div class="endpoint-url">${esc(e.url)}</div></td>
    <td><span class="method">${esc(e.method)}</span></td><td>${esc(e.expectedStatus)}</td>
    <td>${pill(e.enabled?"up":"disabled",e.enabled?"Enabled":"Paused")}</td><td>${esc(shortDate(e.createdAt))}</td>
    <td><div class="actions"><button class="action-btn" data-action="check" data-id="${e.id}">Run check</button><button class="action-btn" data-action="history" data-id="${e.id}">History</button><button class="action-btn" data-action="edit" data-id="${e.id}">Edit</button><button class="action-btn" data-action="toggle" data-id="${e.id}">${e.enabled?"Pause":"Enable"}</button><button class="action-btn" data-action="delete" data-id="${e.id}">Delete</button></div></td>
  </tr>`).join("") : `<tr><td colspan="6" class="empty">No matching endpoints.</td></tr>`;
}
function renderIncidents(target="incident-rows", limit=0) {
  const incidents = limit ? state.incidents.slice(0,limit) : state.incidents;
  const colspan = target === "incident-rows" ? 7 : 6;
  $(target).innerHTML = incidents.length ? incidents.map(i => `<tr data-action="incident" data-id="${i.id}" style="cursor:pointer">
    <td class="endpoint-name">#${i.id}</td><td>${esc(endpointName(i.endpointId))}</td><td>${esc(i.reason || "—")}</td><td>${esc(i.failureCount)}</td><td>${esc(date(i.startedAt))}</td>
    ${target==="incident-rows"?`<td>${esc(date(i.resolvedAt))}</td>`:""}<td>${pill(i.status)}</td>
  </tr>`).join("") : `<tr><td colspan="${colspan}" class="empty">No incidents found.</td></tr>`;
}
async function loadData() {
  try {
    const [endpoints, incidents, stats] = await Promise.all([
      api("/api/endpoints"), api("/api/incidents"), api("/api/endpoints/stats")
    ]);
    const latest = await Promise.all(endpoints.map(async e => {
      try { const page = await api(`/api/endpoints/${e.id}/checks?page=0&size=1`); return {...e, latestCheck:page.content?.[0] || null}; }
      catch (_) { return {...e, latestCheck:null}; }
    }));
    state.endpoints = latest; state.incidents = incidents;
    $("api-state").textContent = "Connected";
    $("stat-endpoints").textContent = stats.totalEndpoints;
    $("stat-incidents").textContent = stats.openIncidents;
    $("stat-checks").textContent = stats.totalChecks;
    $("stat-success").textContent = `${stats.successRate}%`;
    $("nav-open-count").textContent = stats.openIncidents;
    renderOverview(); renderEndpoints(); renderIncidents("overview-incidents",5);
    $("last-updated").textContent = `Updated ${new Date().toLocaleTimeString()}`;
  } catch (error) {
    $("api-state").textContent = "Offline";
    $("overview-endpoints").innerHTML = `<tr><td colspan="6" class="empty">${esc(error.message)}</td></tr>`;
    toast(error.message,"error");
  }
}
async function loadIncidents() {
  try {
    const status = $("incident-filter").value;
    state.incidents = await api(`/api/incidents${status?`?status=${status}`:""}`);
    renderIncidents();
  } catch (e) { toast(e.message,"error"); }
}
function openModal(endpoint=null) {
  state.editingId = endpoint?.id ?? null;
  $("endpoint-form").reset();
  $("endpoint-id").value = endpoint?.id ?? "";
  $("modal-title").textContent = endpoint ? "Edit endpoint" : "Add endpoint";
  $("save-endpoint").textContent = endpoint ? "Save changes" : "Save endpoint";
  $("field-name").value = endpoint?.name ?? "";
  $("field-url").value = endpoint?.url ?? "";
  $("field-method").value = endpoint?.method ?? "GET";
  $("field-status").value = endpoint?.expectedStatus ?? 200;
  $("field-interval").value = endpoint?.intervalSeconds ?? 60;
  $("field-timeout").value = endpoint?.timeoutMs ?? 5000;
  $("field-latency").value = endpoint?.latencyThresholdMs ?? "";
  $("field-enabled").checked = endpoint?.enabled ?? true;
  $("form-error").classList.add("hidden");
  $("endpoint-modal").classList.remove("hidden");
}
function closeModal() { $("endpoint-modal").classList.add("hidden"); }
async function saveEndpoint(event) {
  event.preventDefault();
  const payload = {
    name:$("field-name").value.trim(), url:$("field-url").value.trim(), method:$("field-method").value,
    expectedStatus:Number($("field-status").value), intervalSeconds:Number($("field-interval").value),
    timeoutMs:Number($("field-timeout").value), latencyThresholdMs:$("field-latency").value?Number($("field-latency").value):null,
    enabled:$("field-enabled").checked
  };
  const button = $("save-endpoint"); button.disabled = true; button.textContent = "Saving…";
  try {
    await api(state.editingId ? `/api/endpoints/${state.editingId}` : "/api/endpoints", {
      method:state.editingId?"PUT":"POST", body:JSON.stringify(payload)
    });
    closeModal(); toast(state.editingId?"Endpoint updated":"Endpoint created"); await loadData();
  } catch (e) { $("form-error").textContent=e.message; $("form-error").classList.remove("hidden"); }
  finally { button.disabled=false; button.textContent=state.editingId?"Save changes":"Save endpoint"; }
}
async function runCheck(id) {
  const button = document.querySelector(`[data-action="check"][data-id="${id}"]`);
  if (button) { button.disabled=true; button.textContent="Checking…"; }
  try {
    const result = await api(`/api/endpoints/${id}/check`,{method:"POST"});
    toast(result.success ? `Check passed · ${result.latencyMs} ms` : `Check failed · ${result.errorType || "Unknown error"}`, result.success?"success":"error");
    await loadData();
  } catch(e) { toast(e.message,"error"); }
  finally { if(button){button.disabled=false;button.textContent=button.dataset.action==="check"?"Run check":"Check now";} }
}
async function endpointAction(action,id) {
  const endpoint = state.endpoints.find(e=>e.id===Number(id));
  if (action==="check") return runCheck(id);
  if (action==="history") return openHistory(id);
  if (action==="edit") return openModal(endpoint);
  if (action==="toggle") {
    try { await api(`/api/endpoints/${id}/enabled`,{method:"PATCH",body:JSON.stringify({enabled:!endpoint.enabled})}); toast(endpoint.enabled?"Monitor paused":"Monitor enabled"); await loadData(); }
    catch(e){toast(e.message,"error");}
  }
  if (action==="delete" && confirm(`Delete "${endpoint.name}" and all its saved health checks and incidents?`)) {
    try { await api(`/api/endpoints/${id}`,{method:"DELETE"}); toast("Endpoint deleted"); await loadData(); }
    catch(e){toast(e.message,"error");}
  }
}
async function openHistory(id) {
  state.historyEndpointId=id; state.historyPage=0;
  $("drawer-kicker").textContent="HEALTH CHECK HISTORY";
  $("drawer-title").textContent=endpointName(id);
  $("drawer-content").innerHTML='<div class="empty">Loading history…</div>';
  $("detail-backdrop").classList.remove("hidden");
  await loadHistory();
}
async function loadHistory() {
  const id=state.historyEndpointId; if(!id)return;
  try {
    const data=await api(`/api/endpoints/${id}/checks?page=${state.historyPage}&size=${state.historySize}`);
    state.historyPages=data.totalPages || 0;
    $("drawer-content").innerHTML=`<div class="detail-grid"><div class="detail-box"><small>Total checks</small><strong>${data.totalElements}</strong></div><div class="detail-box"><small>Current page</small><strong>${data.totalPages?data.number+1:0} / ${data.totalPages}</strong></div></div>`+
      (data.content?.length?data.content.map(c=>`<div class="history-item"><div><strong>${c.success?"Successful check":"Failed check"}</strong><div class="history-meta">${esc(date(c.checkedAt))} · ${c.statusCode??"No response"} · ${esc(c.errorType||"No error")}</div></div><div>${pill(c.success?"up":"down",c.success?"Passed":"Failed")}<div class="history-meta" style="text-align:right">${c.latencyMs??"—"} ms</div></div></div>`).join(""):'<div class="empty">No checks recorded yet. Run a check to begin history.</div>');
    $("history-page").textContent=`Page ${data.totalPages?data.number+1:0} of ${data.totalPages}`;
    $("history-prev").disabled=state.historyPage<=0;
    $("history-next").disabled=state.historyPage+1>=state.historyPages;
  } catch(e){$("drawer-content").innerHTML=`<div class="alert">${esc(e.message)}</div>`;}
}
async function showIncident(id) {
  try {
    const incident=await api(`/api/incidents/${id}`);
    $("drawer-kicker").textContent="INCIDENT DETAILS";
    $("drawer-title").textContent=`Incident #${incident.id}`;
    $("drawer-content").innerHTML=`<div class="detail-grid"><div class="detail-box"><small>Endpoint</small><strong>${esc(endpointName(incident.endpointId))}</strong></div><div class="detail-box"><small>Status</small><strong>${esc(incident.status)}</strong></div><div class="detail-box"><small>Failure count</small><strong>${incident.failureCount}</strong></div><div class="detail-box"><small>Reason</small><strong>${esc(incident.reason||"—")}</strong></div><div class="detail-box"><small>Started</small><strong>${esc(date(incident.startedAt))}</strong></div><div class="detail-box"><small>Resolved</small><strong>${esc(date(incident.resolvedAt))}</strong></div></div>`;
    $("history-prev").classList.add("hidden"); $("history-next").classList.add("hidden"); $("history-page").textContent="";
    $("detail-backdrop").classList.remove("hidden");
  } catch(e){toast(e.message,"error");}
}
function closeDrawer(){ $("detail-backdrop").classList.add("hidden"); $("history-prev").classList.remove("hidden"); $("history-next").classList.remove("hidden"); }
document.querySelectorAll(".nav-item").forEach(b=>b.addEventListener("click",()=>setView(b.dataset.view)));
document.querySelectorAll("[data-go]").forEach(b=>b.addEventListener("click",()=>setView(b.dataset.go)));
$("add-endpoint").addEventListener("click",()=>openModal());
$("add-endpoint-2").addEventListener("click",()=>openModal());
$("close-modal").addEventListener("click",closeModal); $("cancel-modal").addEventListener("click",closeModal);
$("endpoint-form").addEventListener("submit",saveEndpoint);
$("refresh-all").addEventListener("click",loadData);
$("endpoint-search").addEventListener("input",renderEndpoints);
$("incident-filter").addEventListener("change",loadIncidents);
$("close-drawer").addEventListener("click",closeDrawer);
$("detail-backdrop").addEventListener("click",e=>{if(e.target===$("detail-backdrop"))closeDrawer();});
$("history-prev").addEventListener("click",()=>{if(state.historyPage>0){state.historyPage--;loadHistory();}});
$("history-next").addEventListener("click",()=>{if(state.historyPage+1<state.historyPages){state.historyPage++;loadHistory();}});
document.addEventListener("click",e=>{
  const action=e.target.closest("[data-action]");
  if(!action)return;
  const {action:kind,id}=action.dataset;
  if(kind==="incident")showIncident(id); else endpointAction(kind,id);
});
document.addEventListener("keydown",e=>{if(e.key==="Escape"){closeModal();closeDrawer();}});
loadData();
setInterval(()=>{if(document.visibilityState==="visible")loadData();},30000);
