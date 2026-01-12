let postImages = [];

/**
 * Komprimiert ein Bild auf eine Zielgröße
 * @param {File} file - Das Original-Bild
 * @param {number} maxSizeMB - Maximale Größe in MB
 * @returns {Promise<File>} - Komprimiertes Bild
 */
async function compressImage(file, maxSizeMB) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = (e) => {
      const img = new Image();

      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // Maximale Dimension: 1920px (Full HD)
        const maxDimension = 1920;
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = (height / width) * maxDimension;
            width = maxDimension;
          } else {
            width = (width / height) * maxDimension;
            height = maxDimension;
          }
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        // Komprimierung mit abnehmender Qualität bis Zielgröße erreicht
        let quality = 0.9;
        const tryCompress = () => {
          canvas.toBlob((blob) => {
            if (!blob) {
              reject(new Error('Komprimierung fehlgeschlagen'));
              return;
            }

            const sizeMB = blob.size / 1024 / 1024;

            if (sizeMB <= maxSizeMB || quality <= 0.1) {
              // Zielgröße erreicht oder minimale Qualität
              const compressedFile = new File([blob], file.name, {
                type: 'image/jpeg',
                lastModified: Date.now()
              });

              if (sizeMB > maxSizeMB) {
                reject(new Error(`Bild zu groß: ${sizeMB.toFixed(2)} MB`));
              } else {
                resolve(compressedFile);
              }
            } else {
              // Weiter komprimieren
              quality -= 0.1;
              tryCompress();
            }
          }, 'image/jpeg', quality);
        };

        tryCompress();
      };

      img.onerror = () => reject(new Error('Bild konnte nicht geladen werden'));
      img.src = e.target.result;
    };

    reader.onerror = () => reject(new Error('Datei konnte nicht gelesen werden'));
    reader.readAsDataURL(file);
  });
}

document.addEventListener('DOMContentLoaded', () => {
  document.body.addEventListener('click', async (e) => {
    const btn = e.target.closest('.postModal');
    if (!btn) return;

    e.preventDefault();

    const parentDocumentId = btn.dataset.documentid || null;

    // Modal HTML laden
    const response = await fetch('/inc/post/post-modal.php');
    const html = await response.text();

    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);

    // Init mit documentId
    initPostModal(parentDocumentId);
  });
});


function initPostModal(parentDocumentId = null) {
  const modal = document.getElementById('postModalOverlay');
  const closeBtn = document.getElementById('closeModal');
  const imageInput = document.getElementById('postImage');
  const cameraInput = document.getElementById('postCamera');

  modal.style.display = 'flex';

  postImages = [];

  closeBtn.addEventListener('click', () => {
    modal.remove();
  });

  // ✨ Hier setzen wir den Parent-Wert, falls vorhanden
  if (parentDocumentId) {
    const parentInput = modal.querySelector('input[name="parent-id"]');
    if (parentInput) {
      parentInput.value = parentDocumentId;
    }
  }

  // Bildvorschau + weitere Listener (unverändert)
  const MAX_FILE_SIZE_MB = 2;

  // Gemeinsame Handler-Funktion für beide Inputs (Galerie + Kamera)
  const handleImageSelection = async (input) => {
    const newFiles = Array.from(input.files);
    const total = postImages.length + newFiles.length;

    if (total > 4) {
      notify("⚠️ Maximal 4 Bilder erlaubt", 'error');
      return;
    }

    for (const file of newFiles) {
      // Prüfen ob Bild zu groß ist
      let processedFile = file;

      if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
        console.log(`📸 Bild zu groß (${(file.size / 1024 / 1024).toFixed(2)} MB), komprimiere...`);

        try {
          processedFile = await compressImage(file, MAX_FILE_SIZE_MB);
          const savedMB = ((file.size - processedFile.size) / 1024 / 1024).toFixed(2);
          console.log(`✅ Komprimierung erfolgreich! ${savedMB} MB gespart`);
        } catch (error) {
          notify(`❌ ${file.name} ist zu groß und konnte nicht komprimiert werden (max. ${MAX_FILE_SIZE_MB} MB)`, 'error');
          continue; // Überspringe dieses Bild
        }
      }

      // Bild zur Vorschau hinzufügen
      const reader = new FileReader();
      reader.onload = e => {
        postImages.push({
          file: processedFile,
          previewUrl: e.target.result,
          alt: ''
        });
        renderThumbnails();
      };
      reader.readAsDataURL(processedFile);
    }

    input.value = '';
  };

  // Event-Listener für beide Inputs registrieren
  imageInput.addEventListener('change', () => handleImageSelection(imageInput));
  cameraInput.addEventListener('change', () => handleImageSelection(cameraInput));

  // KEIN manueller Click-Handler mehr! Das for-Attribut im HTML reicht.
  // Manuelle Click-Handler können in Chrome mit dem for-Attribut kollidieren
  // und verhindern, dass das Foto nach der Aufnahme zurückgegeben wird.

  document.getElementById('saveAltBtn').addEventListener('click', saveAltText);
  document.getElementById('submitPostBtn').addEventListener('click', submitPost);
}


function renderThumbnails() {
  const previewContainer = document.getElementById('previewContainer');
  previewContainer.innerHTML = '';

  postImages.forEach((img, index) => {
    const wrapper = document.createElement('div');
    wrapper.classList.add('thumb');

    const image = document.createElement('img');
    image.src = img.previewUrl;

    // 🗑️ Delete Button (CSS Icon)
    const deleteBtn = document.createElement('button');
    deleteBtn.classList.add('delete-thumb', 'icon-delete');
    deleteBtn.onclick = () => {
      postImages.splice(index, 1);
      renderThumbnails();
    };

    // 📝 Alt-Text Button (CSS Icon)
    const altBtn = document.createElement('button');
    altBtn.classList.add('alt-thumb', 'icon-alt');
    altBtn.onclick = () => openAltEditor(index);

    wrapper.appendChild(image);
    wrapper.appendChild(deleteBtn);
    wrapper.appendChild(altBtn);
    previewContainer.appendChild(wrapper);
  });
}



let currentAltIndex = null;

function openAltEditor(index) {
  currentAltIndex = index;
  document.getElementById('altImage').src = postImages[index].previewUrl;
  document.getElementById('altTextInput').value = postImages[index].alt || '';
  document.getElementById('altTextModal').style.display = 'flex';
}

function closeAltEditor() {
  document.getElementById('altTextModal').style.display = 'none';
  currentAltIndex = null;
}

function saveAltText() {
  const newAlt = document.getElementById('altTextInput').value;
  if (currentAltIndex !== null) {
    postImages[currentAltIndex].alt = newAlt;
  }
  closeAltEditor();
  renderThumbnails();
}

async function submitPost() {
  const content = document.getElementById('postText').value.trim();

  if (!content) {
    notify("Bitte gib einen Text ein.", "error");
    return;
  }

  const formData = new FormData();
  formData.append('text', content);


  const visibilityToggle = document.querySelector('.who-can-answer');
  const visibility = visibilityToggle?.dataset.answer === 'follower' ? 'follower' : 'members';
  formData.append('visibility', visibility);
  console.log(visibilityToggle?.dataset.answer);
  // 📎 Parent-ID (wenn vorhanden)
  const parentField = document.querySelector('input[name="parent-id"]');
  const parentId = parentField?.value?.trim();
  if (parentId) {
    formData.append('parent', parentId);
  }

  // 📸 Bilder & Alt-Texte
  postImages.forEach((img, i) => {
    formData.append('images[]', img.file);
    formData.append(`alts[${i}]`, img.alt || '');
  });

  try {
    const response = await fetch('/inc/post/post-submit.php', {
      method: 'POST',
      body: formData
    });

    const data = await response.json();
    console.log('🧪 Rückgabe:', data);

    if (data.status === 200 || data.status === 201) {
      notify("✅ Beitrag erfolgreich gespeichert!");
      document.getElementById('postModalOverlay')?.remove();

      // 🔄 Nach kurzer Pause passende Ansicht neu laden
      setTimeout(() => {
        const feedEl = document.querySelector('.feed');
        const threadEl = document.querySelector('.thread-view');

        const feedVisible = feedEl && getComputedStyle(feedEl).display !== 'none';
        const threadVisible = threadEl && getComputedStyle(threadEl).display !== 'none';

        if (feedVisible && typeof window.loadFeed === 'function') {
  window.seenPostIds = [];
  window.allLoaded = false;
  document.querySelector('.feed').innerHTML = '';
  window.loadFeed(true);
  // Re-initialisiere Post Collapse nach Feed-Reload
  setTimeout(() => {
    if (typeof window.initPostCollapse === 'function') {
      window.initPostCollapse();
    }
  }, 600);
} else if (threadVisible && typeof window.showThread === 'function') {
          const currentHash = location.hash;
          const threadId = currentHash.startsWith('#thread-') ? currentHash.replace('#thread-', '') : null;

          if (threadId) {
            window.showThread(threadId);
          }
        }
      }, 500); // ⏳ z. B. 1,5 Sekunden warten

    } else {
      notify("❌ Fehler: " + JSON.stringify(data), "error");
    }

  } catch (err) {
    console.error("❌ Fehler beim Senden:", err);
    notify("❌ Serverfehler: " + err.message, "error");
  }
}




  document.addEventListener("click", function (event) {
    const el = event.target;

    // Prüfen, ob das geklickte Element die Klasse hat
    if (el.classList.contains("who-can-answer")) {
      const currentState = el.getAttribute("data-answer");

      if (currentState === "all") {
        el.setAttribute("data-answer", "follower");
        el.textContent = "Nur deine Follower können diesen Beitrag sehen.";
      } else {
        el.setAttribute("data-answer", "all");
        el.textContent = "Jeder kann diesen Beitrag sehen.";
      }
    }
  });