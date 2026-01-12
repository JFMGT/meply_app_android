document.addEventListener('DOMContentLoaded', () => {
  let modalLoaded = false;

  // Modal entfernen & zurücksetzen
  function removeModal() {
    const modal = document.getElementById('createMeetingModal');
    if (modal) {
      modal.closest('div').remove();
      modalLoaded = false;
    }
  }

async function loadConfig() {
  const res = await fetch('../../inc/configJS.php');
  const config = await res.json();
  return config;
}

async function loadModalHtml(eventId = false) {
  if (modalLoaded) return;

  let apiUrl = '../../inc/meeting/meeting-modal.php';
  if (eventId) {
    apiUrl += '?eventID=' + encodeURIComponent(eventId); // ✅ FIX: encodeURIComponent für Sicherheit
  }

  try {
    const res = await fetch(apiUrl);
    if (!res.ok) throw new Error('Modal konnte nicht geladen werden');
    const html = await res.text();

    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);

    modalLoaded = true;

    // ✅ FIX: Initialisierung der Datumstyp-Auswahl
    initializeDateTypeSelector();

  } catch (err) {
    notify('Fehler beim Laden des Modals: ' + err.message);
  }
}

// ✅ NEU: Separate Funktion für die Datumstyp-Initialisierung
function initializeDateTypeSelector() {
  const dateTypeSelect = document.getElementById('dateType');
  if (!dateTypeSelect) return; // Kein Dropdown vorhanden (z.B. bei Event-Meetings)

  dateTypeSelect.addEventListener('change', () => {
    const selected = dateTypeSelect.value;
    
    // Alle date-option Divs verstecken
    document.querySelectorAll('.date-option').forEach(div => {
      div.classList.add('hidden');
    });

    // Nur das ausgewählte anzeigen
    if (selected === 'fixed') {
      document.getElementById('dateFixed')?.classList.remove('hidden');
    } else if (selected === 'range') {
      document.getElementById('dateRange')?.classList.remove('hidden');
    } else if (selected === 'recurring') {
      document.getElementById('dateRecurring')?.classList.remove('hidden');
    }
  });

  // Initial das richtige Feld anzeigen
  dateTypeSelect.dispatchEvent(new Event('change'));
}

async function loadMeetingsIfNeeded() {
  // Suche das Element mit Klasse hold-meetings und entweder data-location oder data-event
  const container = document.querySelector('.hold-meetings[data-location], .hold-meetings[data-event]');
  if (!container) {
    // Kein solches Element gefunden, Funktion beendet
    return;
  }

  // Prüfe, ob data-location oder data-event existiert
  const documentId = container.getAttribute('data-location') || container.getAttribute('data-event');
  if (!documentId) {
    return;
  }

  // Bestimme den type-Parameter
  const type = container.hasAttribute('data-location') ? 'location' : 'event';

  try {
    // PHP API aufrufen mit dem passenden type & documentId
    const response = await fetch(`../../inc/api/getMeetings.php?type=${type}&documentId=${encodeURIComponent(documentId)}`);
    
    if (!response.ok) {
      console.error('Fehler beim Laden der Meetings:', response.statusText);
      const text = await response.text();
      console.error('Fehlerdetails:', response.status, response.statusText, text);
      return;
    }
    
    const data = await response.json();

    if (data.success && data.html !== undefined) {
      // Inhalt ersetzen
      container.innerHTML = data.html;
    } else {
      console.error('Unerwartete Antwort von API:', data);
    }
  } catch (error) {
    console.error('Fehler beim Abrufen der Meetings:', error);
  }
}


  // Klick-Handler für .create-meeting Buttons
  document.querySelectorAll('.create-meeting').forEach(link => {
    link.addEventListener('click', async (e) => {
      e.preventDefault();

      const eventId = link.dataset.event;
      const eventTitle = link.dataset.eventTitle;

      await loadModalHtml(eventId);

      const modal = document.getElementById('createMeetingModal');
      const form = document.getElementById('meetingForm');
      const modalTitle = document.getElementById('modalTitle');
      const closeBtn = modal.querySelector('.close');

      form.reset();

      // Titel anpassen basierend auf Event-Titel
      if (eventId && eventTitle) {
        // XSS-Schutz: Event-Titel als textContent setzen
        const strong = document.createElement('strong');
        strong.style.color = '#fec50d';
        strong.textContent = eventTitle;

        modalTitle.textContent = 'Mitspielersuche für ';
        modalTitle.appendChild(strong);
        modalTitle.appendChild(document.createTextNode(' erstellen'));
      } else {
        modalTitle.textContent = 'Meeting erstellen';
      }

      form.meetingId.value = '';
      form.locationId.value = '';
      form.eventId.value = '';

      const meetingId = link.dataset.meeting;
      const locationId = link.dataset.location;

      if (meetingId) {
        try {
          const config = await loadConfig();
          const res = await fetch(`${config.apiBaseUrl}/meetings/${meetingId}`);
          if (!res.ok) throw new Error('Meeting nicht gefunden');
          const result = await res.json();
          const data = result.data.attributes;

          modalTitle.textContent = 'Meeting bearbeiten';
          form.meetingId.value = meetingId;
          form.title.value = data.title || '';
          form.description.value = data.description || '';
          
          // ✅ FIX: Datum richtig laden
          if (data.date) {
            form.date.value = data.date.slice(0, 16);
          }
        } catch (err) {
          notify('Fehler beim Laden des Meetings');
          return;
        }
      } else {
        if (locationId) form.locationId.value = locationId;
        if (eventId) form.eventId.value = eventId;
      }

      modal.classList.remove('hidden');

      // Nur einmalige Initialisierung
      if (!modal.dataset.initialized) {
        closeBtn.addEventListener('click', removeModal);

        window.addEventListener('click', (e) => {
          if (e.target === modal) removeModal();
        });

        form.addEventListener('submit', async (e) => {
          e.preventDefault();

          const payload = {
            data: {
              title: form.title.value,
              description: form.description.value
            }
          };

          if (form.locationId.value) payload.data.location = form.locationId.value;
          if (form.eventId.value) payload.data.event = form.eventId.value;
          if (form.meetingId.value) payload.id = form.meetingId.value;

          // ❌ SICHERHEIT: author-Feld wird NICHT mehr vom Client gesendet
          // Das Backend setzt den author automatisch auf den eingeloggten User

          // 🧠 Zeitdaten: Events vs. freie Eingabe
          if (form.eventId.value) {
            // ➕ Event-Gesuch – hol dir ausgewählte Tage
            const selectedDays = Array.from(form.querySelectorAll('input[name="eventDays[]"]:checked')).map(input => input.value);
            payload.data.eventDays = selectedDays;
          } else {
            // ➕ Freie Eingabe
            const dateType = form.dateType?.value;
            
            if (dateType) {
              payload.data.dateType = dateType;
              
              if (dateType === 'fixed') {
                payload.data.date = form.date.value;
              } else if (dateType === 'range') {
                payload.data.dateFrom = form.dateFrom.value;
                payload.data.dateTo = form.dateTo.value;
              } else if (dateType === 'recurring') {
                const recurringDays = Array.from(form.querySelectorAll('input[name="recurringDays[]"]:checked')).map(input => input.value);
                payload.data.recurringDays = recurringDays;
                payload.data.recurringFrequency = form.recurringFrequency.value;
              }
            }
          }

          try {
            const res = await fetch('../../inc/api/save-meeting.php', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(payload)
            });

            if (!res.ok) {
              const errorData = await res.json();
              throw new Error(errorData?.error || 'Speichern fehlgeschlagen');
            }

            notify('Meeting gespeichert!');
            loadMeetingsIfNeeded();
            
            removeModal();
          } catch (err) {
            notify('Fehler: ' + err.message);
          }
        });

        modal.dataset.initialized = "true";
      }
    });
  });
});