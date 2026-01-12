<?php
// Prüfen, ob das Formular abgeschickt wurde

// $errorMessage sollte vorab existieren oder hier initialisiert werden:
$errorMessage = $errorMessage ?? null;

if ($_SERVER['REQUEST_METHOD'] === 'POST' || isset($_POST['log']) || isset($_POST['pwd'])) {

    // 0) Precheck zuerst (IP wird in der Funktion automatisch ermittelt)
    
 $pre = checkRateLimit('login', 20, 300, null);

// Besser: Prüft auf den 'success'-Schlüssel.
// Das fängt technische Fehler (Config, cURL) UND API-Fehler (4xx, 5xx) ab.
if (!$pre['success']) {
    
    // Standard-Fehler
    $errorMessage = "❌ Technischer Fehler beim Sicherheitscheck. Bitte später erneut versuchen.";
    
    // Spezialfall: API hat einen Rate-Limit-Fehler (429) gemeldet
    // und uns Details in einem Array mitgeschickt.
    if ($pre['code'] === 429 && is_array($pre['response'])) {
        $mins = isset($pre['response']['until']) ? max(1, ceil(($pre['response']['until'] - time()) / 60)) : null;
        $hint = $mins ? " Bitte in ca. {$mins} Minute(n) erneut versuchen." : "";
        $errorMessage = "❌ Zu viele Anmeldeversuche." . $hint;
    }

} else {
    // Der Aufruf war erfolgreich (HTTP 200)
    // Jetzt den *Inhalt* der Antwort prüfen (z.B. Geo-Blocking)
    $preData = $pre['response'];

    if (($preData['action'] ?? null) === 'block') {
        if (($preData['reason'] ?? null) === 'geo') {
            $errorMessage = "❌ Anmeldung aus deiner Region momentan nicht erlaubt.";
        } else {
            // Anderer Block-Grund bei HTTP 200
            $errorMessage = "❌ Anmeldung nicht möglich (Grund: " . ($preData['reason'] ?? 'unbekannt') . ").";
        }
    }
    
    // Wenn $preData['action'] == 'allow', wird $errorMessage nie gesetzt,
    // was das korrekte Verhalten ist.
}

    // 1) Nur wenn Precheck *nicht* gestoppt hat, mit Login fortfahren
    if (empty($errorMessage)) {
        $identifier = $_POST['log'] ?? '';
        $password   = $_POST['pwd'] ?? '';

        if ($identifier === '' || $password === '') {
            $errorMessage = "❌ Bitte Benutzername/E-Mail und Passwort eingeben.";
        } else {
            $result = loginUser($identifier, $password);

            if (!$result) {
                $errorMessage = "❌ Login fehlgeschlagen. Bitte überprüfe deine Zugangsdaten.";
            } else {
                // Login erfolgreich: Session speichern
                $_SESSION['user'] = $result['user'];
                $_SESSION['jwt']  = $result['jwt'];

                // Weiterleitung zur Startseite
                header('Location:' . $baseurl . '/dashboard/?notification="Erfolgreich Angemeldet"');
                exit;
            }
        }
    }
}


?>


<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Meply - Meet & Play</title>
<meta name='robots' content='max-image-preview:large' />
	<style>img:is([sizes="auto" i], [sizes^="auto," i]) { contain-intrinsic-size: 3000px 1500px }</style>
	<link rel='dns-prefetch' href='//unpkg.com' />
<style id='classic-theme-styles-inline-css' type='text/css'>
/*! This file is auto-generated */
.wp-block-button__link{color:#fff;background-color:#32373c;border-radius:9999px;box-shadow:none;text-decoration:none;padding:calc(.667em + 2px) calc(1.333em + 2px);font-size:1.125em}.wp-block-file__button{background:#32373c;color:#fff;text-decoration:none}
</style>

<link rel='stylesheet' id='main-styles-css' href='<?= $baseurl ?>/etc/styles/main.css?ver=1.8.3' type='text/css' media='all' />
<link rel='stylesheet' id='main-styles-css' href='<?= $baseurl ?>/etc/styles/shrink.css?ver=1.2.2' type='text/css' media='all' />
<link rel='stylesheet' id='main-styles-css' href='<?= $baseurl ?>/etc/styles/modal.css?ver=1' type='text/css' media='all' />
<link rel='stylesheet' id='font-styles-css' href='<?= $baseurl; ?>/etc/fonts/poppins/stylesheet.css?ver=6.7.2' type='text/css' media='all' />
<link rel='stylesheet' id='awesome-styles-css' href='<?= $baseurl; ?>/etc/fonts/font-awesome/css/all.min.css?ver=6.7.2' type='text/css' media='all' />
<link rel='stylesheet' id='leafleet-styles-css' href='<?= WEBSITE_BASE ?>etc/styles/leaflet.css?ver=6.7.2' type='text/css' media='all' />
<link rel='stylesheet' id='leaflet-cluster-css-css' href='<?= WEBSITE_BASE ?>etc/styles/leaflet-markercluster.css?ver=6.7.2' type='text/css' media='all' />
<!--
<script type="text/javascript" id="jquery-core-js-extra">
/* <![CDATA[ */
var myAjax = {"ajaxurl":"https:\/\/meply.de\/wp-admin\/admin-ajax.php"};
var ajaxurl = "https:\/\/meply.de\/wp-admin\/admin-ajax.php";
/* ]]> */
</script>
-->

<link rel="canonical" href="https://meply.de/" />
<link rel='shortlink' href='https://meply.de/' />
<link rel="alternate" title="oEmbed (JSON)" type="application/json+oembed" href="https://meply.de/wp-json/oembed/1.0/embed?url=https%3A%2F%2Fmeply.de%2F" />
<link rel="alternate" title="oEmbed (XML)" type="text/xml+oembed" href="https://meply.de/wp-json/oembed/1.0/embed?url=https%3A%2F%2Fmeply.de%2F&#038;format=xml" />
<link rel="icon" href="https://meply.de/etc/images/favicon.png" type="image/png">

<!-- CSRF Protection Meta Tag -->
<?php echo csrfTokenMeta(); ?>
</head>

<?php
function activeClass(string $path): string {
    $current = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
    $path = rtrim(strtolower($path), '/');
    $current = rtrim(strtolower($current), '/');
    
    return $current === $path ? ' current-menu-item ' : '';
}
?>


<body class="poppins-regular">
 <div class="main-background">
     <div class="main-container">
    <?php

    if($_SESSION['profile']['deleteDate']){
        ?>

        <style>
            .deletation-error {
    position: fixed;
    bottom: 100px; /* Abstand von oben */
    left: 50%; /* Mittig auf der Seite */
    transform: translateX(-50%); /* Korrektur für die exakte Mitte */
    background-color: #f44336; /* Roter Hintergrund für Warnung */
    color: white; /* Weißer Text */
    padding: 15px 30px; /* Etwas Abstand zum Rand */
    border-radius: 5px; /* Abgerundete Ecken */
    font-size: 16px; /* Schriftgröße */
    z-index: 1000; /* Sicherstellen, dass die Warnung oben ist */
}


        </style>

        <div class="deletation-error">
            Dein Profil ist für die Löschung vorgesehen und wird ab dem <?= $_SESSION['profile']['deleteDate'] ?> gelöscht. <br>
            Du kannst die Löschung in deinem Profil stoppen. 
        </div>
        <?php
    }

    ?>


    <header>
        <div class="container">
            <!-- Logo -->
            <div class="logo">
                <div class="mobile-sidebar-toggle">
                 <i class="fas fa-bars"></i>
            </div>
                <a style="margin-top:15px" href="<?= $baseurl ?>"><img style="width:35px; height:auto; margin-right:35px" src="<?php echo WEBSITE_BASE; ?>/etc/images/logo_meeplemates_white_bigboy.svg"></a>

                <a href="<?= $baseurl ?>" style="position: relative; top: 4px;">MEPLY<span style="font-size:15px;display: block;position: relative;top: -10px;">Meet &amp; Play</span></a>


            </div>
            <div class="nav-cta">
            <!-- Navigation Menu -->
            <nav>
                   <ul id="menu-hauptmenue" class="main-menu">
                    


               
            <!-- CTA Button -->
            
                

<?php
    if (isset($_SESSION['user'])) {
        ?>
        <li class="notification-wrapper">
  <a href="#" class="notification-icon" id="notificationToggle">
    <i class="fa-solid fa-bell"></i>
    <span class="notification-badge hidden" id="notificationCount">!</span>
  </a>
  <div class="notification-dropdown hidden" id="notificationDropdown">
    <ul id="notificationList">
      <li><i class="fa-solid fa-spinner fa-spin"></i> Lade Benachrichtigungen...</li>
    </ul>
  </div>
</li>
        <?php
        echo'<li class="user-setting-menu">';
    // Benutzer ist eingeloggt

    echo '<a href="" class="user-menu">Hallo, '.$_SESSION["user"]["username"].'<br><span>Einstellungen</span> <i class="fa-solid fa-caret-down"></i></a>';
    echo"</li>";
} else {
    echo'<li class="cta">';
    // Benutzer ist NICHT eingeloggt
    echo '<a href="' . $baseurl . '/community/login/" class="open-post-modal cta-button">Login</a>';
    echo"</li>";
}
?>


            </li>
        <?php    if (isset($_SESSION['user'])) { ?>
    <li class="cta">
                        <a id="" class="postModal cta-button" href="">+ <span>Etwas erzählen</span></a>
                    </li><?php } ?>

                    </ul>          
<div class="user-dropdown hidden">
  <ul>
    <li><a href="<?= $baseurl; ?>/dashboard/"><i class="fa-solid fa-gauge"></i>Dashboard</a></li>
    <li><a href="<?= $baseurl; ?>/community/edit_profile.php"><i class="fa-solid fa-user"></i>Profil</a></li>
    <li><a href="<?= $baseurl; ?>/community/follower.php"><i class="fa-solid fa-people-arrows"></i>Follower</a></li>
    <li><a href="<?= $baseurl; ?>/community/boardgames.php"><i class="fa-solid fa-dice"></i>Sammlung</a></li>
    <li><a href="<?= $baseurl; ?>/community/messages/"><i class="fa-solid fa-comments"></i>Nachrichten</a></li>
    <li><a href="<?= $baseurl; ?>/community/gesuche/"><i class="fa-solid fa-magnifying-glass"></i>Deine Gesuche</a></li>
    <li><a href="<?= $baseurl; ?>/community/locations/"><i class="fa-solid fa-map-marker-alt"></i>Deine Locations</a></li>
    <li><a href="<?= $baseurl; ?>/community/events/"><i class="fa-solid fa-calendar"></i>Deine Events</a></li>
    <li><a href="<?= $baseurl; ?>/community/file-list.php"><i class="fa-solid fa-images"></i>Deine Bilder</a></li>
    <li><a href="<?= $baseurl; ?>/community/logout.php"><i class="fa-solid fa-person-running"></i></i>Logoout</a></li>
  
  </ul>
</div>

                      </nav>
            </div>
            <!-- Mobile Menu Toggle -->
            <div class="mobile-menu-toggle">
            </div>
        </div>
    </header>
        <div class="sidebar">
            <ul id="menu-sidebar" class="sidebar-menu">
<li id="menu-item-29" class="<?php echo activeClass("/"); ?>fa-solid fa-dice-six menu-item menu-item-type-post_type menu-item-object-page menu-item-home menu-item-29"><a href="<?= $baseurl;  ?>/">Start</a></li>
<?php if (isset($_SESSION['user'])) { ?>
<li id="menu-item-29" class="<?php echo activeClass("/feed"); ?>fa-solid fa-bullhorn menu-item menu-item-type-post_type menu-item-object-page menu-item-home menu-item-29"><a href="<?= $baseurl;  ?>/community/feed/">Feed</a></li>
<?php } ?>

<li id="menu-item-21" class="<?php echo activeClass("/events"); ?>fas fa-calendar-alt menu-item menu-item-type-post_type menu-item-object-page menu-item-21"><a href="<?= $baseurl;  ?>events/">Events</a></li>
<li id="menu-item-545" class="<?php echo activeClass("/map/club"); ?> fa-solid fa-people-roof menu-item menu-item-type-post_type menu-item-object-page menu-item-545"><a href="<?= $baseurl;  ?>map/club">Clubs</a></li>
<li id="menu-item-23" class="<?php echo activeClass("/map/cafe"); ?> fas fa-coffee menu-item menu-item-type-post_type menu-item-object-page page_item page-item-6 current_page_item menu-item-23"><a href="<?= $baseurl;  ?>map/cafe" aria-current="page">Cafés</a></li>
<li id="menu-item-22" class="<?php echo activeClass("/map/geschaeft"); ?> fas fa-store menu-item menu-item-type-post_type menu-item-object-page menu-item-22"><a href="<?= $baseurl;  ?>map/geschaeft">Geschäfte</a></li>

<?php if (isset($_SESSION['user'])) { ?>
<li id="menu-item-523" class="<?php echo activeClass("/markt/"); ?> fa-solid fa-tags menu-item menu-item-type-post_type menu-item-object-page menu-item-523"><a href="<?= $baseurl;  ?>markt/">Trödelmarkt</a></li>
<li id="menu-item-20" class="<?php echo activeClass("/mitspieler"); ?> fas fa-users menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-20"><a href="<?= $baseurl;  ?>mitspieler/">Mitspieler</a>
<li id="menu-item-523" class="<?php echo activeClass("/community/mitspieler"); ?> fa-solid fa-magnifying-glass menu-item menu-item-type-post_type menu-item-object-page menu-item-523"><a href="<?= $baseurl;  ?>community/mitspieler/">Gesuche</a></li>

</li>


<?php } ?>  


<li class="fa-solid fa-right-to-bracket menu-item-postfach menu-item menu-item-type-post_type menu-item-object-page menu-item-165 mobile-extra-item"><a href="/profil/">Login</a></li></ul>

         
        </div>
<style type="text/css">
/* Container für Glocke und Dropdown */
.notification-wrapper {
  position: relative;
  margin-right: 20px;
}

/* Glocken-Icon */
.notification-icon {
  position: relative;
  color: white;
  font-size: 20px;
  text-decoration: none;
}

/* Rotes Badge oben rechts */
.notification-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  background: red;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 50%;
  display: inline-block;
}

.notification-badge.hidden {
  display: none;
}

/* Dropdown-Feld */
.notification-dropdown {
  position: absolute;
  top: 30px;
  right: 0;
  background: #222;
  border: 1px solid #444;
  padding: 10px;
  width: 300px;
  z-index: 1000;
  box-shadow: 0 4px 8px rgba(0,0,0,0.3);
  border-radius: 8px;
}

.notification-dropdown ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notification-dropdown li {
  color: #eee;
  padding: 8px 5px;
  font-size: 14px;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
}

.notification-dropdown li:last-child {
  border-bottom: none;
}

.notification-dropdown i {
  margin-right: 10px;
  min-width: 20px;
}

.notification-dropdown.hidden {
  display: none;
}

    
    /* Grunddesign */
.user-menu {
  cursor: pointer;
  color: #ffffff;
  text-decoration: none;
  position: relative;
}

.user-dropdown {
  position: absolute;
  right: 0px;
    top: 79px;
  background: #222;
  border: 1px solid #444;
  border-bottom-left-radius:25px;
  padding: 10px;
  width: 428px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
  z-index: 1000;
  transition: all 0.2s ease;
}

.user-dropdown.hidden {
  display: none;
}

.user-dropdown a{
    margin:0;
  padding: 10px;
    width:100%;
    display: inline-block;
}

.user-dropdown i{
    margin-right:10px;
    min-width:25px;
}

.user-dropdown ul {
  list-style: none;
  margin: 0;
  padding: 0;
      margin-left: 23px;
}

.user-dropdown li {
  color: #eee;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.2s;
}

.user-dropdown li:hover {
  background: #333;
  cursor: pointer;
}

</style>

<script type="text/javascript">
    // Menü öffnen/schließen
document.querySelector('.user-menu').addEventListener('click', function(e) {
  e.preventDefault();
  document.querySelector('.user-dropdown').classList.toggle('hidden');
});

// Menü schließen, wenn man außerhalb klickt
document.addEventListener('click', function(event) {
  const menu = document.querySelector('.user-dropdown');
  const button = document.querySelector('.user-menu');
  
  if (!menu.contains(event.target) && !button.contains(event.target)) {
    menu.classList.add('hidden');
  }
});

</script>