<?php
include('../../inc/header.php');
// CSRF-Token wird jetzt im header.php bzw. über generateCsrfToken() generiert
generateCsrfToken(); // Ensure token exists
?>
<div class="content top-margin content-plain">
    <h1>Werde Teil der Meply-Community!</h1>
    <div class="card">
        <p>Die Registrierung ist kostenlos und in nur wenigen Schritten erledigt. Worauf wartest du noch? Werde Teil unserer wachsenden Brettspiel-Community und finde deine nächste Partie!</p>

        <div id="form-message" class="form-message"></div>

        <form id="register-form">
            <!-- SECURITY: CSRF-Token -->
            <input type="hidden" name="csrf_token" id="csrf_token" value="<?php echo htmlspecialchars($_SESSION['csrf_token'], ENT_QUOTES, 'UTF-8'); ?>">
            
            <p>
                <label for="username">Benutzername</label>
                <input
                    type="text"
                    name="username"
                    id="username"
                    required
                    pattern="[a-zA-Z0-9_\-]{3,20}"
                    title="3-20 Zeichen, nur Buchstaben, Zahlen, Unterstrich und Bindestrich"
                    maxlength="20"
                    autocomplete="username"
                >
                <small>3-20 Zeichen, nur Buchstaben, Zahlen, _ und -</small>
            </p>
            
            <p>
                <label for="email">E-Mail-Adresse</label>
                <input 
                    type="email" 
                    name="email" 
                    id="email" 
                    required
                    maxlength="100"
                    autocomplete="email"
                >
            </p>
            
            <p>
                <label for="password">Passwort</label>
                <input 
                    type="password" 
                    name="password" 
                    id="password" 
                    required
                    minlength="8"
                    maxlength="128"
                    autocomplete="new-password"
                >
                <small>Mindestens 8 Zeichen</small>
                <!-- Optional: Passwort-Stärke-Anzeige hier einfügen -->
                <div id="password-strength" class="password-strength"></div>
            </p>
            
            <p>
                <label for="password_confirm">Passwort bestätigen</label>
                <input 
                    type="password" 
                    name="password_confirm" 
                    id="password_confirm" 
                    required
                    minlength="8"
                    maxlength="128"
                    autocomplete="new-password"
                >
            </p>
            
            <p>
                <label for="registration_key">Registrierungsschlüssel</label>
                <input 
                    type="text" 
                    name="registration_key" 
                    id="registration_key" 
                    required
                    maxlength="50"
                >
            </p>
            <p class="small-text">Um eine Überlastung des Servers zu vermeiden, ist ein Registrierungsschlüssel erforderlich.</p>

            <p class="checkbox-group">
                <label>
                    <input type="checkbox" name="privacy_confirm" id="privacy_confirm" required>
                    Ich habe die <a href="/pages/datenschutzerklaerung" target="_blank">Datenschutzerklärung</a> gelesen und akzeptiere sie.
                </label>
            </p>

            <input type="submit" name="submit" value="Registrieren" id="submit-button">
        </form>
    </div>
</div>

<style>
/* Bestehende Styles */
.form-message { margin-top: 1rem; font-weight: bold; padding: 10px; border-radius: 4px; }
.form-message.error { color: #d32f2f; background-color: #ffebee; border: 1px solid #ef5350; }
.form-message.success { color: #388e3c; background-color: #e8f5e9; border: 1px solid #66bb6a; }
.small-text { font-size: 12px; color: #666; margin-top: 5px; }
.checkbox-group { margin-top: 15px; font-size: 14px; }
.checkbox-group a { color: #fec50d; text-decoration: underline; }

/* Neue Styles */
small { 
    display: block; 
    font-size: 12px; 
    color: #666; 
    margin-top: 5px; 
}

input:invalid { 
    border-color: #d32f2f; 
}

input:valid { 
    border-color: #388e3c; 
}

/* Passwort-Stärke-Anzeige */
.password-strength {
    height: 4px;
    margin-top: 5px;
    border-radius: 2px;
    transition: all 0.3s;
}

.password-strength.weak { background-color: #d32f2f; width: 33%; }
.password-strength.medium { background-color: #ff9800; width: 66%; }
.password-strength.strong { background-color: #388e3c; width: 100%; }

/* Loading-State */
#submit-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
</style>

<script>
// ========================================
// SECURITY: Client-seitige Validierung
// ========================================

document.addEventListener('DOMContentLoaded', function() {

// Passwort-Stärke-Check (optional)
const passwordField = document.getElementById('password');
if (passwordField) {
    passwordField.addEventListener('input', function(e) {
    const password = e.target.value;
    const strengthBar = document.getElementById('password-strength');
    
    if (password.length === 0) {
        strengthBar.className = 'password-strength';
        return;
    }
    
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.length >= 12) strength++;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++;
    if (/\d/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;
    
    if (strength <= 2) {
        strengthBar.className = 'password-strength weak';
    } else if (strength <= 3) {
        strengthBar.className = 'password-strength medium';
    } else {
        strengthBar.className = 'password-strength strong';
    }
    });
}

// ========================================
// Form Submit Handler
// ========================================

const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const form = e.target;
        const formData = new FormData(form);
        const messageBox = document.getElementById('form-message');
        const submitButton = document.getElementById('submit-button');
    
    // Reset
    messageBox.className = 'form-message';
    messageBox.textContent = '';
    
    // ========================================
    // SECURITY: Client-seitige Validierung
    // ========================================
    
    // Passwort-Match prüfen
    if (formData.get('password') !== formData.get('password_confirm')) {
        messageBox.textContent = 'Die Passwörter stimmen nicht überein.';
        messageBox.classList.add('error');
        return;
    }
    
    // Username-Format prüfen
    const username = formData.get('username');
    if (!/^[a-zA-Z0-9_\-]{3,20}$/.test(username)) {
        messageBox.textContent = 'Ungültiger Benutzername (3-20 Zeichen, nur Buchstaben, Zahlen, _ und -)';
        messageBox.classList.add('error');
        return;
    }
    
    // Passwort-Länge prüfen
    const password = formData.get('password');
    if (password.length < 8) {
        messageBox.textContent = 'Passwort muss mindestens 8 Zeichen lang sein.';
        messageBox.classList.add('error');
        return;
    }
    
    // Datenschutz-Checkbox prüfen
    if (!formData.get('privacy_confirm')) {
        messageBox.textContent = 'Bitte bestätige die Datenschutzerklärung.';
        messageBox.classList.add('error');
        return;
    }
    
    // ========================================
    // Submit
    // ========================================
    
    // UI: Loading-State
    submitButton.disabled = true;
    messageBox.textContent = '⏳ Registrierung wird verarbeitet...';
    messageBox.className = 'form-message';

    try {
        const response = await fetch('register.php', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (data.success) {
            messageBox.textContent = data.message;
            messageBox.classList.add('success');
            
            // Formular zurücksetzen
            form.reset();
            
            // Weiterleitung nach 3 Sekunden
            setTimeout(() => {
                window.location.href = '/?notification=' + encodeURIComponent(data.message);
            }, 3000);
            
        } else {
            messageBox.textContent = data.message || 'Ein unbekannter Fehler ist aufgetreten.';
            messageBox.classList.add('error');
            submitButton.disabled = false;
        }

    } catch (err) {
        messageBox.textContent = 'Ein Netzwerkfehler ist aufgetreten. Bitte versuche es später erneut.';
        messageBox.classList.add('error');
        submitButton.disabled = false;
        console.error('Register Error:', err);
    }
    });
}

}); // End DOMContentLoaded
</script>

<?php include("../../inc/footer.php"); ?>