<?php
include("../inc/header_auth.php");
$jwt = $_SESSION['jwt'];
$profileId = $_SESSION['profile']['documentId'];

?>
<div class="content content-plain top-margin">
  <h2>Lieblingsspiele importieren</h2>
  <p>Wenn dir deine Bewertungen als CSV-Datei vorliegen, kannst du sie hier hochladen und automatisch in deine Lieblingsspiele übernehmen lassen.</p>

  <form id="csvUploadForm" enctype="multipart/form-data" style="margin-top: 1.5rem;">
    <div style="margin-bottom: 1rem;">
      <label for="csvFile">CSV-Datei auswählen:</label><br>
      <input type="file" id="csvFile" name="csvFile" accept=".csv" required>
    </div>

    <div style="margin-bottom: 1rem; max-width: 600px;">
      <p style="font-size: 0.9rem; line-height: 1.4;">
        Die Datei sollte Spieltitel und deine Bewertung enthalten. Nach dem Hochladen kannst du die Spalten den passenden Feldern zuordnen.
        <br><br>
        Bitte lade nur Daten hoch, die du selbst erstellt hast oder zu deren Weitergabe du berechtigt bist – z.&nbsp;B. aus deinem privaten Account oder aus öffentlich zugänglichen Quellen. Mit dem Hochladen bestätigst du, dass du zur Nutzung und Weitergabe dieser Daten berechtigt bist.
      </p>
    </div>

    <div style="margin-bottom: 1rem;">
      <label>
        <input type="checkbox" required>
        Ich bestätige, dass ich zur Nutzung dieser Datei berechtigt bin und dass deren Inhalte (Titel, BGG-ID, Bewertung) zur Erweiterung der Spielesammlung auf MeepleMates verwendet werden dürfen.
      </label>
    </div>

    <button type="submit" class="btn-primary">Hochladen & Weiter</button>
  </form>

  <div id="uploadStatus" style="margin-top: 1rem;">
    <div id="loadingSpinner" style="display: none; margin-bottom: 1rem;">
      🔄 Daten werden geprüft, bitte warten ...
    </div>
  </div>
</div>

<script>
// Globale Variable für Delimiter
let currentDelimiter = ",";

document.getElementById("csvUploadForm").addEventListener("submit", function (e) {
  e.preventDefault();
  const formData = new FormData();
  const file = document.getElementById("csvFile").files[0];
  formData.append("csvFile", file);

  fetch("../inc/api/upload-handler.php", {
    method: "POST",
    body: formData
  })
  .then(res => res.json())
  .then(data => {
    if (data.success) {
      currentDelimiter = data.delimiter || ",";
      renderMappingUI(data.preview, data.headers, data.filePath, currentDelimiter);
    } else {
      console.log(data);
      document.getElementById("uploadStatus").innerText = "Fehler beim Hochladen: " + data.error;
    }
  });
});

/**
 * Analysiert eine Spalte und gibt Validierungs-Feedback
 */
function validateColumn(preview, columnIndex, fieldType) {
  if (columnIndex === "") return null;
  
  const values = preview.map(row => row[columnIndex]).filter(v => v != null && v.trim() !== "");
  if (values.length === 0) return null;
  
  const totalValues = values.length;
  let numericCount = 0;
  let textCount = 0;
  let emptyCount = 0;
  const numericValues = [];
  
  values.forEach(val => {
    const trimmed = val.trim();
    if (trimmed === "") {
      emptyCount++;
    } else if (/^\d+\.?\d*$/.test(trimmed)) { // Erlaubt auch Dezimalzahlen
      numericCount++;
      numericValues.push(parseFloat(trimmed));
    } else {
      textCount++;
    }
  });
  
  const numericPercent = (numericCount / totalValues) * 100;
  const textPercent = (textCount / totalValues) * 100;
  
  // Validierung basierend auf Feldtyp
  if (fieldType === "title") {
    if (numericPercent > 80) {
      return {
        type: "error",
        message: "⚠️ <strong>Warnung:</strong> Die gewählte Spalte besteht zu " + Math.round(numericPercent) + "% aus reinen Zahlen. Das sieht nicht nach Spieltiteln aus!"
      };
    } else if (numericPercent > 30) {
      return {
        type: "warning",
        message: "⚠️ <strong>Hinweis:</strong> Die gewählte Spalte enthält viele Zahlen (" + Math.round(numericPercent) + "%). Bitte überprüfe die Zuordnung."
      };
    }
    
    // NEU: Prüfung auf identische/wenig verschiedene Titel
    const uniqueTitles = new Set(values.map(v => v.trim().toLowerCase()));
    
    // Alle Titel identisch
    if (uniqueTitles.size === 1) {
      return {
        type: "error",
        message: "⚠️ <strong>Warnung:</strong> Alle Einträge sind identisch (\"" + values[0].substring(0, 30) + (values[0].length > 30 ? "..." : "") + "\"). Das kann nicht stimmen!"
      };
    }
    
    // Zu wenig verschiedene Titel (verdächtig wenn z.B. 100 Zeilen aber nur 3 verschiedene Titel)
    if (uniqueTitles.size < 5 && totalValues > 10) {
      return {
        type: "warning",
        message: "⚠️ <strong>Hinweis:</strong> Nur " + uniqueTitles.size + " verschiedene Titel bei " + totalValues + " Einträgen gefunden. Ist das die richtige Spalte?"
      };
    }
    
    // Duplikate warnen (wenn >30% Duplikate vorhanden sind)
    const duplicatePercent = ((totalValues - uniqueTitles.size) / totalValues) * 100;
    if (duplicatePercent > 30) {
      return {
        type: "warning",
        message: "⚠️ <strong>Hinweis:</strong> " + Math.round(duplicatePercent) + "% Duplikate gefunden. Das könnte bedeuten, dass du manche Spiele mehrfach importierst."
      };
    }
    
    return { 
      type: "success", 
      message: "✅ Sieht nach Spieltiteln aus (" + uniqueTitles.size + " verschiedene Titel)" 
    };
  }
  
  if (fieldType === "bgg_id") {
    if (textPercent > 20) {
      return {
        type: "error",
        message: "⚠️ <strong>Warnung:</strong> Die gewählte Spalte enthält Text-Einträge (" + Math.round(textPercent) + "%). BGG-IDs müssen reine Zahlen sein!"
      };
    }
    
    if (numericValues.length > 0) {
      const minVal = Math.min(...numericValues);
      const maxVal = Math.max(...numericValues);
      const avgVal = numericValues.reduce((a, b) => a + b, 0) / numericValues.length;
      const uniqueValues = new Set(numericValues);
      
      // Prüfung: Zu kleine Werte (0, 1, 2 sind sehr unwahrscheinlich)
      if (minVal < 10 && numericValues.filter(v => v < 10).length > numericValues.length * 0.5) {
        return {
          type: "error",
          message: "⚠️ <strong>Warnung:</strong> Viele Werte sind sehr klein (< 10). BGG-IDs sind normalerweise 4-6 stellig. Hast du vielleicht eine andere Spalte gewählt?"
        };
      }
      
      // Prüfung: Alle Werte gleich
      if (uniqueValues.size === 1) {
        return {
          type: "error",
          message: "⚠️ <strong>Warnung:</strong> Alle Werte sind identisch (" + minVal + "). Das kann nicht stimmen!"
        };
      }
      
      // Prüfung: Zu wenig Variation (verdächtig wenn nur 2-3 verschiedene Werte)
      if (uniqueValues.size < 5 && numericValues.length > 10) {
        return {
          type: "warning",
          message: "⚠️ <strong>Hinweis:</strong> Nur " + uniqueValues.size + " verschiedene Werte gefunden. Ist das wirklich die BGG-ID Spalte?"
        };
      }
      
      // Prüfung: Unrealistisch hohe Werte (>500.000)
      if (maxVal > 500000) {
        return {
          type: "warning",
          message: "⚠️ <strong>Hinweis:</strong> Einige Werte sind sehr hoch (max: " + Math.round(maxVal) + "). BGG-IDs sind aktuell meist < 500.000."
        };
      }
      
      // Alles OK
      if (emptyCount > 0) {
        return { 
          type: "success", 
          message: "✅ BGG-IDs sehen plausibel aus (Bereich: " + Math.round(minVal) + " - " + Math.round(maxVal) + ", " + emptyCount + " leere Einträge)"
        };
      } else {
        return { 
          type: "success", 
          message: "✅ BGG-IDs sehen plausibel aus (Bereich: " + Math.round(minVal) + " - " + Math.round(maxVal) + ")"
        };
      }
    }
  }
  
  if (fieldType === "rating") {
    if (textPercent > 20) {
      return {
        type: "error",
        message: "⚠️ <strong>Warnung:</strong> Die gewählte Spalte enthält Text-Einträge (" + Math.round(textPercent) + "%). Bewertungen müssen Zahlen sein!"
      };
    }
    
    if (numericValues.length > 0) {
      const minVal = Math.min(...numericValues);
      const maxVal = Math.max(...numericValues);
      const avgVal = numericValues.reduce((a, b) => a + b, 0) / numericValues.length;
      const uniqueValues = new Set(numericValues);
      
      // Prüfung: Negative Werte
      if (minVal < 0) {
        return {
          type: "error",
          message: "⚠️ <strong>Warnung:</strong> Negative Bewertungen gefunden (min: " + minVal + "). Das kann nicht stimmen!"
        };
      }
      
      // Prüfung: Zu hohe Werte (> 10 ist ungewöhnlich)
      if (maxVal > 10) {
        return {
          type: "warning",
          message: "⚠️ <strong>Hinweis:</strong> Einige Bewertungen sind sehr hoch (max: " + maxVal + "). Übliche Skalen sind 1-5 oder 1-10. Eventuell falsche Spalte?"
        };
      }
      
      // Prüfung: Sehr hohe Werte (> 100 deutet auf Prozentwerte oder Fehler hin)
      if (maxVal > 100) {
        return {
          type: "error",
          message: "⚠️ <strong>Warnung:</strong> Bewertungen über 100 gefunden (max: " + maxVal + "). Das sieht nicht nach Bewertungen aus!"
        };
      }
      
      // Prüfung: Alle Werte gleich
      if (uniqueValues.size === 1) {
        return {
          type: "warning",
          message: "⚠️ <strong>Hinweis:</strong> Alle Bewertungen sind identisch (" + minVal + "). Ist das gewollt?"
        };
      }
      
      // Prüfung: Zu wenig Variation
      if (uniqueValues.size < 3 && numericValues.length > 10) {
        return {
          type: "warning",
          message: "⚠️ <strong>Hinweis:</strong> Sehr wenig Variation in den Bewertungen (nur " + uniqueValues.size + " verschiedene Werte)."
        };
      }
      
      // Skala erkennen und Info geben
      let scaleInfo = "";
      if (maxVal <= 5) {
        scaleInfo = " (Skala scheint 0-5 oder 1-5 zu sein)";
      } else if (maxVal <= 10) {
        scaleInfo = " (Skala scheint 0-10 oder 1-10 zu sein)";
      }
      
      // Alles OK
      if (emptyCount > 0) {
        return { 
          type: "success", 
          message: "✅ Bewertungen sehen plausibel aus" + scaleInfo + " (Bereich: " + minVal + " - " + maxVal + ", " + emptyCount + " ohne Bewertung)"
        };
      } else {
        return { 
          type: "success", 
          message: "✅ Bewertungen sehen plausibel aus" + scaleInfo + " (Bereich: " + minVal + " - " + maxVal + ")"
        };
      }
    }
    
    // Wenn nur leere Werte
    if (emptyCount > 0) {
      return { 
        type: "info", 
        message: "ℹ️ " + emptyCount + " Einträge ohne Bewertung (werden ignoriert)" 
      };
    }
  }
  
  return null;
}


function renderMappingUI(preview, headers, filePath, initialDelimiter = ",") {
  const container = document.getElementById("uploadStatus");
  currentDelimiter = initialDelimiter;

  const fieldsToMap = [
    { key: "title", label: "Spielname", required: true },
    { key: "bgg_id", label: "BoardGameGeek ID", required: false },
    { key: "rating", label: "Bewertung", required: false }
  ];

  let html = `<h4>Liste Konfigurieren:</h4>`;

  // Delimiter Auswahl
  html += `
    <div style="margin-bottom: 1rem;">
      <label for="delimiterSelect"><strong>Trennzeichen:</strong></label>
      <p>Bitte wähle das verwendete Trennzeichen. Wenn du das Trennzeichen nicht kennst, öffne die Datei mit einem Texteditor und überprüfe dies.</p>
      <select id="delimiterSelect">
        <option value="," ${currentDelimiter === "," ? "selected" : ""}>Komma (,)</option>
        <option value=";" ${currentDelimiter === ";" ? "selected" : ""}>Semikolon (;)</option>
        <option value="|" ${currentDelimiter === "|" ? "selected" : ""}>Pipe (|)</option>
      </select>
    </div>
  `;

  html += `<h4>Felder zuordnen:</h4>`;
  html += `<p>Ordne deine Spalten den Feldern zu. <strong>Titel wird benötigt</strong>, BGG-ID und Bewertung sind optional.</p>`;

  // Vorschau-Steuerung
  html += `
    <div style="margin-bottom: 1rem;">
      <strong>Vorschauzeile:</strong>
      <span id="preview-counter">1 / ${preview.length}</span>
      <button type="button" id="preview-prev" style="cursor: pointer; margin-left: 0.5rem; background-color:white"><i class="fa-solid fa-angles-left"></i></button>
      <button type="button" id="preview-next" style="cursor: pointer; margin-left: 0.5rem; background-color:white"><i class="fa-solid fa-angles-right"></i></button>
    </div>
  `;

  html += "<table><tbody>";
  fieldsToMap.forEach(field => {
    const requiredMark = field.required ? '<span style="color: red;">*</span>' : '';
    html += `<tr>
      <td style="vertical-align: top; padding-top: 8px;"><strong>${field.label}${requiredMark}:</strong></td>
      <td style="vertical-align: top;">
        <select class="mapping-select" data-key="${field.key}" ${field.required ? 'required' : ''}>
          <option value="">(nicht zuordnen)</option>
          ${headers.map((header, i) => `
            <option value="${i}">${header}</option>
          `).join("")}
        </select>
        <div id="validation-${field.key}" style="margin-top: 0.5rem; font-size: 0.85rem;"></div>
      </td>
      <td class="preview-cell" id="preview-${field.key}" style="color: #aaa; font-style: italic; padding-left:25px; vertical-align: top; padding-top: 8px;"></td>
    </tr>`;
  });
  html += "</tbody></table>";

  // Gesamtwarnung
  html += `<div id="overall-validation" style="margin-top: 1rem; color:black"></div>`;

  // Import Button
  html += `<button type="button" id="startImportBtn" class="btn-primary" style="margin-top: 1rem;">Import starten</button>`;

  container.innerHTML = html;

  let previewIndex = 0;

  function updatePreview() {
    document.querySelectorAll(".mapping-select").forEach(select => {
      const index = select.value;
      const key = select.dataset.key;
      const cell = document.getElementById("preview-" + key);
      if (index !== "" && preview[previewIndex]) {
        const value = preview[previewIndex][index];
        cell.innerText = value?.trim() !== "" ? value : "[kein Inhalt]";
      } else {
        cell.innerText = "";
      }
    });

    document.getElementById("preview-counter").innerText = `${previewIndex + 1} / ${preview.length}`;
  }
  
  function validateAllFields() {
    let hasErrors = false;
    let hasWarnings = false;
    
    document.querySelectorAll(".mapping-select").forEach(select => {
      const key = select.dataset.key;
      const index = select.value;
      const validationDiv = document.getElementById("validation-" + key);
      
      if (index === "") {
        validationDiv.innerHTML = "";
        return;
      }
      
      const result = validateColumn(preview, parseInt(index), key);
      
      if (result) {
        let color = "#666";
        if (result.type === "error") {
          color = "#d32f2f";
          hasErrors = true;
        } else if (result.type === "warning") {
          color = "#f57c00";
          hasWarnings = true;
        } else if (result.type === "success") {
          color = "#388e3c";
        } else if (result.type === "info") {
          color = "#1976d2";
        }
        
        validationDiv.innerHTML = `<div style="color: ${color};">${result.message}</div>`;
      } else {
        validationDiv.innerHTML = "";
      }
    });
    
    // Gesamtwarnung anzeigen
    const overallDiv = document.getElementById("overall-validation");
    if (hasErrors) {
      overallDiv.innerHTML = `
        <div style="background-color: #ffebee; border-left: 4px solid #d32f2f; padding: 1rem; margin-bottom: 1rem;">
          <strong>⚠️ Achtung:</strong> Es wurden Probleme bei der Feldzuordnung erkannt. 
          Bitte überprüfe deine Auswahl, bevor du den Import startest.
        </div>
      `;
    } else if (hasWarnings) {
      overallDiv.innerHTML = `
        <div style="background-color: #fff3e0; border-left: 4px solid #f57c00; padding: 1rem; margin-bottom: 1rem;">
          <strong>ℹ️ Hinweis:</strong> Bitte überprüfe die Hinweise zu deinen Feldzuordnungen.
        </div>
      `;
    } else {
      overallDiv.innerHTML = "";
    }
  }

  document.querySelectorAll(".mapping-select").forEach(select => {
    select.addEventListener("change", () => {
      updatePreview();
      validateAllFields();
    });
  });

  document.getElementById("preview-prev").addEventListener("click", () => {
    if (previewIndex > 0) {
      previewIndex--;
      updatePreview();
    }
  });

  document.getElementById("preview-next").addEventListener("click", () => {
    if (previewIndex < preview.length - 1) {
      previewIndex++;
      updatePreview();
    }
  });

  updatePreview();
  validateAllFields(); // Initial validation

  // Event Listener for delimiter change
  document.getElementById("delimiterSelect").addEventListener("change", () => {
    currentDelimiter = document.getElementById("delimiterSelect").value;
    console.log('Change Delimiter: ' + currentDelimiter);
    const formData = new FormData();
    formData.append("filePath", filePath);
    formData.append("delimiter", currentDelimiter);

    fetch("../inc/api/parse-csv-preview.php", {
      method: "POST",
      body: formData
    })
    .then(res => res.json())
    .then(data => {
      if (data.success) {
        renderMappingUI(data.preview, data.headers, data.filePath, currentDelimiter);
      } else {
        container.innerHTML = `<p style="color: red;">Fehler beim Neuladen: ${data.error || "Unbekannter Fehler"}</p>`;
      }
    });
  });

  // Import Button Click Handler
  document.getElementById("startImportBtn").addEventListener("click", () => {
    submitMappedData(filePath, currentDelimiter);
  });
}

function submitMappedData(filePath, delimiter) {
  const selects = document.querySelectorAll(".mapping-select");
  const mapping = {};

  selects.forEach(select => {
    const field = select.dataset.key;
    const index = select.value;
    if (index !== "") {
      mapping[field] = parseInt(index);
    }
  });

  if (mapping.title === undefined) {
    alert("Bitte ordne mindestens die Spalte 'Spielname' zu.");
    return;
  }

  // UI vorbereiten
  const uploadStatus = document.getElementById("uploadStatus");
  uploadStatus.innerHTML = `
    <div id="import-progress">
      <div style="margin-bottom: 1.5rem;">
        <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
          <strong id="progress-text">Starte Import...</strong>
          <span id="progress-percent">0%</span>
        </div>
        <div style="background-color: #e0e0e0; height: 24px; border-radius: 4px; overflow: hidden;">
          <div id="progress-bar" style="background-color: #4caf50; height: 100%; width: 0%; transition: width 0.3s;"></div>
        </div>
        <div style="margin-top: 0.5rem; font-size: 0.9rem; color: #666;">
          <span id="time-info">Berechne Dauer...</span>
        </div>
      </div>
      
      <div id="stats-summary" style="color:black;margin-bottom: 1rem; padding: 1rem; background-color: #f5f5f5; border-radius: 4px; display: none;">
        <strong>Aktueller Stand:</strong>
        <div style="margin-top: 0.5rem;">
          ✔️ <span id="stat-existing">0</span> bereits vorhanden &nbsp;•&nbsp; 
          ➕ <span id="stat-new">0</span> neu angelegt &nbsp;•&nbsp; 
          ⚠️ <span id="stat-errors">0</span> Fehler
        </div>
      </div>
      
      <div id="games-list" style="max-height: 400px; overflow-y: auto; border: 1px solid #e0e0e0; border-radius: 4px; padding: 0.5rem;">
        <!-- Wird dynamisch gefüllt -->
      </div>
    </div>
  `;

  // Import-State
  const state = {
    offset: 0,
    limit: 5,
    totalRows: 0,
    totalExisting: 0,
    totalNew: 0,
    totalErrors: 0,
    allResults: [],
    startTime: Date.now()
  };

  // Batch-Import starten
  processBatch(filePath, mapping, delimiter, state);
}

function processBatch(filePath, mapping, delimiter, state) {
  fetch("/inc/api/process-import.php", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ 
      filePath, 
      mapping, 
      delimiter,
      offset: state.offset,
      limit: state.limit
    })
  })
  .then(res => res.json())
  .then(data => {
    console.log("Batch Response:", data);

    if (!data.success) {
      showError(data.error || "Fehler beim Import");
      return;
    }

    // Update State
    state.totalRows = data.batch.total_rows;
    state.totalExisting += data.stats.existing;
    state.totalNew += data.stats.new;
    
    // Fehler zählen
    const errors = data.results.filter(r => r.status === "error").length;
    state.totalErrors += errors;
    
    // Ergebnisse sammeln
    state.allResults = state.allResults.concat(data.results);

    // UI Update
    updateProgress(state, data.batch.processed);
    updateStats(state);
    updateGamesList(data.results);

    // Nächster Batch oder fertig?
    if (!data.batch.is_complete) {
      state.offset += data.batch.processed;
      // Kurze Pause für UI-Update
      setTimeout(() => {
        processBatch(filePath, mapping, delimiter, state);
      }, 100);
    } else {
      showComplete(state);
    }
  })
  .catch(err => {
    console.error("Fehler beim Import:", err);
    showError("Beim Import ist ein Fehler aufgetreten.");
  });
}

function updateProgress(state, processed) {
  const currentTotal = state.offset + processed;
  const percent = Math.round((currentTotal / state.totalRows) * 100);
  
  document.getElementById("progress-bar").style.width = percent + "%";
  document.getElementById("progress-percent").textContent = percent + "%";
  document.getElementById("progress-text").textContent = 
    `Importiere Spiel ${currentTotal} von ${state.totalRows}`;

  // Zeit-Schätzung
  const elapsed = (Date.now() - state.startTime) / 1000; // Sekunden
  const avgTimePerGame = elapsed / currentTotal;
  const remaining = (state.totalRows - currentTotal) * avgTimePerGame;
  
  document.getElementById("time-info").textContent = 
    `Dauer: ${Math.round(elapsed)}s • Geschätzt noch: ~${Math.round(remaining)}s`;
}

function updateStats(state) {
  document.getElementById("stats-summary").style.display = "block";
  document.getElementById("stat-existing").textContent = state.totalExisting;
  document.getElementById("stat-new").textContent = state.totalNew;
  document.getElementById("stat-errors").textContent = state.totalErrors;
}

function updateGamesList(results) {
  const listDiv = document.getElementById("games-list");
  
  results.forEach(result => {
    const row = document.createElement("div");
    row.style.padding = "0.5rem";
    row.style.borderBottom = "1px solid #f0f0f0";
    
    let icon = "⏳";
    let status = "wird verarbeitet...";
    let color = "#666";
    
    if (result.status === "existing") {
      icon = "✅";
      status = "bereits vorhanden";
      color = "#388e3c";
    } else if (result.status === "new") {
      icon = "➕";
      status = "neu angelegt";
      color = "#1976d2";
    } else if (result.status === "error") {
      icon = "⚠️";
      status = "Fehler";
      color = "#d32f2f";
    }
    
    row.innerHTML = `
      <span style="color: ${color};">${icon}</span>
      <strong>${result.title}</strong>
      ${result.bgg_id ? '<span style="color: #999; font-size: 0.85rem;"> (BGG: ' + result.bgg_id + ')</span>' : ''}
      <span style="color: #999; margin-left: 0.5rem;">• ${status}</span>
    `;
    
    listDiv.appendChild(row);
  });
  
  // Auto-scroll nach unten
  listDiv.scrollTop = listDiv.scrollHeight;
}

function showComplete(state) {
  document.getElementById("progress-text").textContent = "Import abgeschlossen! ✅";
  document.getElementById("progress-bar").style.backgroundColor = "#4caf50";
  
  const elapsed = (Date.now() - state.startTime) / 1000;
  document.getElementById("time-info").textContent = 
    `Gesamtdauer: ${Math.round(elapsed)}s`;
}

function showError(message) {
  const uploadStatus = document.getElementById("uploadStatus");
  uploadStatus.innerHTML = `
    <div style="padding: 1rem; background-color: #ffebee; border-left: 4px solid #d32f2f; border-radius: 4px;">
      <strong>❌ Fehler:</strong> ${message}
    </div>
  `;
}
</script>
<?php include("../inc/footer.php"); ?>