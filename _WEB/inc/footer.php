<footer>
    <div class="container">
        <p>&copy; 2025 Meply.de. Alle Rechte vorbehalten.</p>
           <ul id="menu-footer" class="main-menu"><li id="menu-item-184" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-184"><a href="<?= $baseurl ?>/pages/impressum/">Impressum</a></li>
<li id="menu-item-185" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-privacy-policy menu-item-185"><a rel="privacy-policy" href="<?= $baseurl ?>/pages/datenschutzerklaerung/">Datenschutz­erklärung</a></li>
<li id="menu-item-278" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-278"><a href="<?= $baseurl ?>/pages/cookie-richtlinie-eu/">Cookie-Richtlinie (EU)</a></li>

</ul>    </div>
</footer>

<!-- CSRF Protection - MUST be loaded first to override fetch() -->
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/csrf-protection.js" id="csrf-protection-js"></script>

<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/notify.js" id="notify-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/notificationState.js" id="notificationState-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/likeit.js" id="likeit-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/follow.js" id="follow-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/follow-manage.js" id="follow-manage-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/delete-post.js" id="delete-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/report-post.js" id="report-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/create-meeting.js" id="create-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/message.js" id="create-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/delete-meeting.js" id="create-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/delete-message.js" id="create-script-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/notifications.js" id="notifications-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/feed.js" id="feed-js"></script>



<script src="<?php echo $baseurl; ?>/inc/post/post.js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/post-collapse.js" id="post-collapse-js"></script>
<script type="text/javascript" src="<?= $baseurl; ?>/etc/scripts/lightbox.js" id="lightbox-js"></script>

<div id="lightbox" class="lightbox">
  <span class="close-btn" onclick="closeLightbox()">&times;</span>

  <!-- Navigation Controls -->
  <button class="lightbox-nav lightbox-prev" onclick="lightboxPrev()" aria-label="Vorheriges Bild">
    <i class="fa-solid fa-chevron-left"></i>
  </button>
  <button class="lightbox-nav lightbox-next" onclick="lightboxNext()" aria-label="Nächstes Bild">
    <i class="fa-solid fa-chevron-right"></i>
  </button>

  <!-- Image Counter -->
  <div class="lightbox-counter">
    <span id="lightboxCounter">1 / 1</span>
  </div>

  <!-- Zoom Controls -->
  <div class="lightbox-zoom-controls">
    <button onclick="zoomIn()" aria-label="Vergrößern">
      <i class="fa-solid fa-plus"></i>
    </button>
    <button onclick="resetZoom()" aria-label="Zoom zurücksetzen">
      <i class="fa-solid fa-expand"></i>
    </button>
    <button onclick="zoomOut()" aria-label="Verkleinern">
      <i class="fa-solid fa-minus"></i>
    </button>
  </div>

  <!-- Download Button -->
  <button class="lightbox-download" onclick="downloadImage()" aria-label="Bild herunterladen">
    <i class="fa-solid fa-download"></i>
  </button>

  <div class="holder">
    <img class="lightbox-image" src="" alt="">
    <div class="lightbox-caption"></div>
  </div>
</div>



<?php if(isset($_SESSION['profile']) && isset($_SESSION['profile']['postalCode']) && $_SESSION['profile']['postalCode'] <= 0){ ?>

<div class="missing-userinfo" id="missingUserinfo">
  <button class="close-btn" onclick="document.getElementById('missingUserinfo').style.display='none'">&times;</button>
  <p>
    Fast geschafft! Trag deine PLZ ein und wähle deine Lieblingsspiele, 
    um Mitspieler in deiner Nähe zu finden.
  </p>


  <a href="<?= $baseurl; ?>community/edit_profile.php">PLZ angeben</a>
  <a href="<?= $baseurl; ?>community/boardgames.php">Spiele auswählen</a>
</div>

<?php } ?>


<div id="notification" class="notification hidden"></div>
 <script>
        // Mobile Menu Toggle
        const toggleButton = document.querySelector('.mobile-menu-toggle');
        const menu = document.querySelector('.menu');
        toggleButton.addEventListener('click', () => {
            menu.classList.toggle('active');
        });


        const toggleSidebarButton = document.querySelector('.mobile-sidebar-toggle');
        const sidebar = document.querySelector('.sidebar');
        toggleSidebarButton.addEventListener('click', () => {
            sidebar.classList.toggle('active');
        });
    </script>


    </div>
 </div>
</body>
</html>