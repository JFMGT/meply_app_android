<?php
    require_once __DIR__ . '/inc/config.php';
    require_once(INCLUDE_DIR . 'header.php'); ?>

<div class="content hero" style="background-image: url('<?php echo WEBSITE_BASE; ?>etc/images/meeplemates_header.jpg');">
    <div class="color-layer">
        <section>
            <p class="overline">Die Brettspiel-Community für deine Region</p>
            <h1>Entdecke Events und Spielorte in deiner Nähe</h1>
            <p class="hero-subtitle">Werde Teil einer wachsenden Community. Entdecke spannende Events, finde Spielcafés und Locations auf unserer interaktiven Karte und vernetze dich mit anderen Brettspiel-Fans.</p>
            <?php if(!$_SESSION['user']){ ?>
            <div class="cta-buttons">
                <a href="<?= $baseurl; ?>/community/registrieren/" class="cta-button primary">Jetzt Teil werden</a>
                <a href="#features" class="cta-button secondary">Mehr erfahren</a>
            </div>
            <?php } else { ?>
            <div class="cta-buttons">
                <a href="<?= $baseurl; ?>/community/feed/" class="cta-button primary">Zum Feed</a>
                <a href="<?= $baseurl; ?>/events/" class="cta-button secondary">Events entdecken</a>
            </div>
            <?php } ?>
        </section>
    </div>
</div>

<!-- Features Section -->
<div class="content features-section" id="features">
    <h2 class="section-title">Was dich bei Meply erwartet</h2>

    <div class="features-grid">
        <div class="feature-card">
            <div class="feature-icon">
                <i class="fa-solid fa-calendar-days"></i>
            </div>
            <h3>Events entdecken</h3>
            <p>Finde Spielabende, Turniere und Community-Events in deiner Umgebung. Erstelle eigene Events und lade andere Spieler ein.</p>
        </div>

        <div class="feature-card">
            <div class="feature-icon">
                <i class="fa-solid fa-location-dot"></i>
            </div>
            <h3>Spielorte finden</h3>
            <p>Entdecke Spielcafés, Locations und Treffpunkte auf unserer interaktiven Karte. Bewerte Orte und teile deine Erfahrungen.</p>
        </div>

        <div class="feature-card">
            <div class="feature-icon">
                <i class="fa-solid fa-users"></i>
            </div>
            <h3>Community finden</h3>
            <p>Tausche dich mit anderen Spielern aus, teile deine Spielrunden und baue dein Netzwerk in der Brettspiel-Szene auf.</p>
        </div>

        <div class="feature-card">
            <div class="feature-icon">
                <i class="fa-solid fa-dice"></i>
            </div>
            <h3>Spielesammlung</h3>
            <p>Zeige anderen, welche Spiele du besitzt und welche du gerne spielen möchtest. Finde Gleichgesinnte mit ähnlichen Interessen.</p>
        </div>
    </div>
</div>


<!-- Events Vorschau -->
<div class="content">
    <h2 class="section-title">Aktuelle Events</h2>
    <div class="events events-frontpage"></div>
    <div style="text-align: center; margin-top: 30px;">
        <a href="<?= $baseurl; ?>/events/" class="cta-button secondary">Alle Events anzeigen</a>
    </div>
</div>

<!-- Final CTA -->
<?php if(!$_SESSION['user']){ ?>
<div class="content final-cta">
    <div class="final-cta-content">
        <h2>Bereit, Teil der Community zu werden?</h2>
        <p>Sei von Anfang an dabei und baue dein Brettspiel-Netzwerk auf.</p>
        <a href="<?= $baseurl; ?>/community/registrieren/" class="cta-button primary large">Jetzt registrieren</a>
    </div>
</div>
<?php } ?>

<script>
document.addEventListener('DOMContentLoaded', function () {
    const eventsContainer = document.querySelector('.events');

    function loadEvents(zip = 33334, radius = 1000000, limit = 2) {
        fetch('/inc/api/events.php', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `zip=${encodeURIComponent(zip)}&radius=${encodeURIComponent(radius)}&limit=${encodeURIComponent(limit)}`
        })
        .then(response => response.json())
        .then(data => {
            eventsContainer.innerHTML = data.html || '<p style="text-align:center; color:#999;">Keine Events gefunden.</p>';
        })
        .catch(error => {
            console.error('Fehler beim Laden der Events:', error);
            eventsContainer.innerHTML = '<p style="text-align:center; color:#999;">Fehler beim Laden der Events.</p>';
        });
    }

    // Events laden
    loadEvents();

    // Smooth Scroll für "Mehr erfahren" Link
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        });
    });
});
</script>

<?php
require_once(INCLUDE_DIR . 'footer.php');
