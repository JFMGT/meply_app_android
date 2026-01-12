<?php

require_once('../inc/config.php');
include("../inc/header_auth.php");
$curUser = $_SESSION['user']['documentId'] ?? null;
$jwt = $_SESSION['jwt'] ?? null;

if (!$curUser || !$jwt) {
    echo "<p>❌ Nicht eingeloggt.</p>";
    include("../inc/footer.php");
    exit;
}


?>

<!-- ===== Bilder-Übersicht ===== -->
<div class="card content content-plain top-margin">
<h3>Deiner hochgeladenen Bilder</h3>
<p>
Hier kannst du deine Bilder verwalten und diejenigen entfernen, die nicht mehr sichtbar sein sollen.<br>
👉 Hinweis: Die dazugehörigen Beiträge bleiben bestehen und werden nicht gelöscht!
</p>
<!-- Pagination (oben) -->
<div id="img_pagination_top" class="pagination-controls"></div>

<!-- Grid -->
<div id="userImageGallery" class="img-gallery"></div>

<!-- Pagination (unten) -->
<div id="img_pagination_bottom" class="pagination-controls"></div>

<!-- Lightbox / Preview -->
<div id="lightbox" class="lightbox" aria-hidden="true">
  <div class="lightbox-inner">
    <button class="close" onclick="closeLightbox()">✕</button>
    <img id="lightboxImg" src="" alt="">
    <div id="lightboxMeta" class="meta"></div>
  </div>
</div>
</div>
<script>
var baseImageUrl = "<?php echo STRAPI_IMAGE_BASE; ?>";

// --- Helper zum Ergänzen der Domain ---
function toImageUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path; // schon absolut
  return baseImageUrl.replace(/\/$/, '') + path; // Domain vorne anhängen
}

(function injectBasicStyles(){
  const css = `
    .img-gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:12px;margin-top:1rem}
    .img-card{background:#1c242d;border:1px solid #2a3542;border-radius:8px;overflow:hidden;color:#eaeff5;display:flex;flex-direction:column}
    .img-thumb{width:100%;aspect-ratio:1/1;object-fit:cover;cursor:pointer;display:block}
    .img-meta{padding:8px;font-size:12px;line-height:1.35}
    .img-meta .name{font-weight:600;word-break:break-word}
    .img-actions{display:flex;gap:8px;padding:8px;border-top:1px solid #2a3542}
    .btn{background:#2b3643;border:1px solid #394658;color:#dfe7f1;padding:6px 8px;border-radius:6px;font-size:12px;cursor:pointer}
    .btn[disabled]{opacity:.5;cursor:not-allowed}
    .pagination-controls{margin:8px 0}
    .pagination-controls button{margin:0 4px 8px 0;padding:6px 10px;border-radius:6px;border:1px solid #394658;background:#2b3643;color:#dfe7f1;cursor:pointer}
    .pagination-controls button.active{background:#fec50d;color:#1c1c1c;border-color:#fec50d}
    .lightbox{display:none;position:fixed;inset:0;background:rgba(0,0,0,.8);z-index:9999;align-items:center;justify-content:center;padding:24px}
    .lightbox.open{display:flex}
    .lightbox-inner{background:#111722;border:1px solid #2a3542;border-radius:10px;max-width:90vw;max-height:90vh;padding:14px;color:#eaeff5}
    .lightbox img{max-width:100%;max-height:70vh;display:block;margin:0 auto 10px}
    .lightbox .close{float:right;cursor:pointer;font-size:18px;background:none;border:none;color:#dfe7f1}
    .lightbox .meta{font-size:12px;opacity:.9}
    .delete-image{padding:5px;background-color:#2a3542;color:#fff;cursor:pointer;border-radius:6px;display:inline-block}
  `;
  const el = document.createElement('style'); el.textContent = css; document.head.appendChild(el);
})();

// --- Pagination-State ---
let currentImgPage = 1;
const IMG_PAGE_SIZE = 24;

// --- API-Aufruf an dein PHP ---
async function loadUserImages(page = 1) {
  currentImgPage = page;
  const url = `/inc/api/list_user_images.php?page=${page}&pageSize=${IMG_PAGE_SIZE}`;

  let payload;
  try {
    const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
    const txt = await res.text();           // robustes Parsing (Debug bei HTML-Fehlerseiten)
    payload = JSON.parse(txt);
  } catch (e) {
    console.error('❌ JSON-Parsing fehlgeschlagen:', e);
    return;
  }

  if (!payload?.success) {
    console.error('❌ API-Fehler:', payload?.error || payload);
    renderUserImages([]);                   // leeren Zustand zeigen
    renderImagePagination({ page: 1, pageCount: 1 });
    return;
  }

  const items = payload.results || [];
  const pagination = payload.pagination || { page, pageSize: IMG_PAGE_SIZE, pageCount: 1, total: 0 };

  renderUserImages(items);
  renderImagePagination(pagination);
}

// --- Rendering des Grids ---
function renderUserImages(items) {
  const wrap = document.getElementById('userImageGallery');
  wrap.innerHTML = '';

  if (!Array.isArray(items) || items.length === 0) {
    wrap.innerHTML = '<div style="opacity:.7">Du hast noch keine Bilder hochgeladen.</div>';
    return;
  }

  items.forEach((item) => {
    const f = item.file;

    const card = document.createElement('div');
    card.className = 'img-card';

    const img = document.createElement('img');
    img.className = 'img-thumb';
    img.src = toImageUrl(f?.thumbnailUrl || f?.url || '');
    img.alt = f?.name || 'Bild';
    img.loading = 'lazy';
    img.onclick = () => openLightbox({
      src: toImageUrl(f?.previewUrl || f?.url || ''),
      name: f?.name,
      info: {
        id: f?.id, mime: f?.mime, size: f?.size,
        width: f?.width, height: f?.height,
        reason: item?.reason, createdAt: item?.createdAt,
      }
    });

    const meta = document.createElement('div');
    meta.className = 'img-meta';
    meta.innerHTML = `
      <div class="name">${escapeHtml(f?.name || '—')}</div>
      <div>${f?.mime || '—'} • ${formatKB(f?.size)} • ${f?.width || '–'}×${f?.height || '–'}</div>
      <div>Grund: ${escapeHtml(item?.reason || '—')}</div>
      <div><small>${formatDate(item?.createdAt)}</small></div><br>
      <div class="delete-image" data-img-id="${item?.id}">Bild löschen</div>
    `;

    const actions = document.createElement('div');
    actions.className = 'img-actions';

    card.appendChild(img);
    card.appendChild(meta);
    card.appendChild(actions);
    document.getElementById('userImageGallery').appendChild(card);
  });
}

// --- Pagination-Buttons (oben & unten) ---
function renderImagePagination(pagination) {
  const top = document.getElementById('img_pagination_top');
  const bottom = document.getElementById('img_pagination_bottom');
  [top, bottom].forEach(el => { if (el) el.innerHTML = ''; });

  const { page = 1, pageCount = 1 } = pagination || {};
  const containers = [top, bottom].filter(Boolean);

  containers.forEach(el => {
    for (let i = 1; i <= pageCount; i++) {
      const btn = document.createElement('button');
      btn.textContent = i;
      btn.className = (i === page) ? 'active' : '';
      btn.addEventListener('click', () => loadUserImages(i));
      el.appendChild(btn);
    }
  });
}

// --- Lightbox / Preview ---
function openLightbox({ src, name, info }) {
  const lb = document.getElementById('lightbox');
  const img = document.getElementById('lightboxImg');
  const meta = document.getElementById('lightboxMeta');

  img.src = toImageUrl(src);
  img.alt = name || 'Bild';
  meta.innerHTML = `
    <div><strong>${escapeHtml(name || '')}</strong></div>
    <div>ID: ${info?.id ?? '—'} | ${info?.mime ?? '—'} | ${formatKB(info?.size)}</div>
    <div>Maße: ${info?.width ?? '–'}×${info?.height ?? '–'}</div>
    <div>Grund: ${escapeHtml(info?.reason || '—')}</div>
    <div>Hochgeladen: ${formatDate(info?.createdAt)}</div>
  `;

  lb.classList.add('open');
  lb.setAttribute('aria-hidden', 'false');
}
function closeLightbox() {
  const lb = document.getElementById('lightbox');
  const img = document.getElementById('lightboxImg');
  const meta = document.getElementById('lightboxMeta');
  img.src = '';
  meta.innerHTML = '';
  lb.classList.remove('open');
  lb.setAttribute('aria-hidden', 'true');
}
document.getElementById('lightbox')?.addEventListener('click', (e) => {
  if (e.target.id === 'lightbox') closeLightbox();
});

// --- Delete: Event Delegation + API-Call ---
document.addEventListener('click', async (e) => {
  const btn = e.target.closest('.delete-image');
  if (!btn) return;

  const id = btn.getAttribute('data-img-id');
  if (!id) return;

  if (!confirm('Willst du dieses Bild wirklich löschen?')) return;

  const card = btn.closest('.img-card');
  const prevText = btn.textContent;
  btn.textContent = 'Lösche …';
  btn.classList.add('busy');
  btn.style.opacity = '0.7';
  btn.style.pointerEvents = 'none';

  try {
    const res = await fetch(`/inc/api/delete_user_image.php?id=${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { 'Accept': 'application/json' },
    });

    const text = await res.text();
    let payload;
    try { payload = JSON.parse(text); } catch { payload = null; }

    if (!res.ok || !payload || payload.success !== true) {
      alert(payload?.error || `Fehler beim Löschen (HTTP ${res.status})`);
      btn.textContent = prevText;
      btn.style.opacity = '';
      btn.style.pointerEvents = '';
      btn.classList.remove('busy');
      return;
    }

    // Erfolgreich – Karte entfernen
    if (card) card.remove();

    // Falls die Seite leer ist, zurückblättern oder neu laden
    const gallery = document.getElementById('userImageGallery');
    if (gallery && gallery.children.length === 0) {
      const prevPage = Math.max(1, currentImgPage - 1);
      if (currentImgPage > 1) {
        loadUserImages(prevPage);
      } else {
        loadUserImages(1);
      }
    }
  } catch (err) {
    console.error('Netzwerk-/Serverfehler beim Löschen:', err);
    alert('Netzwerkfehler beim Löschen.');
    btn.textContent = prevText;
    btn.style.opacity = '';
    btn.style.pointerEvents = '';
    btn.classList.remove('busy');
  }
});

// --- kleine Helfer ---
function formatKB(n){ if (!n && n !== 0) return '– KB'; return `${Math.round(n)} KB`; }
function formatDate(iso){ if (!iso) return '—'; const d=new Date(iso); return d.toLocaleString(); }
function escapeHtml(s){
  return String(s ?? '')
    .replace(/&/g,'&amp;').replace(/</g,'&lt;')
    .replace(/>/g,'&gt;').replace(/"/g,'&quot;')
    .replace(/'/g,'&#039;');
}

// Initial laden (Seite 1)
document.addEventListener('DOMContentLoaded', () => loadUserImages());
</script>


<?php include("../inc/footer.php"); ?>