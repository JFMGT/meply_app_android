<?php
include("../inc/header.php");
?>
<div class="content top-margin content-plain">
    <h1>Anmelden</h1>
    <div class="card">
        <p>Melde dich mit deinen Benutzerdaten an oder registriere dich mit deiner E-Mail-Adresse. <a href="/community/registrieren/">Hier geht es zur Registrierung.</a></p>
    </div>

    <?php if (!empty($errorMessage)) : ?>
        <div class="card" style="border: 1px solid red; color: red;">
            <?= $errorMessage ?>
        </div>
    <?php endif; ?>

    <div class="card">
        <form name="loginform" id="loginform" action="login.php" method="post">
            <p class="login-username">
                <label for="user_login">Benutzername oder E-Mail-Adresse</label>
                <input type="text" name="log" id="user_login" autocomplete="username" class="input" value="<?= htmlspecialchars($_POST['log'] ?? '') ?>" size="20" />
            </p>
            <p class="login-password">
                <label for="user_pass">Passwort</label>
                <input type="password" name="pwd" id="user_pass" autocomplete="current-password" spellcheck="false" class="input" value="" size="20" />
            </p>
            <p class="login-remember">
                <label><input name="rememberme" type="checkbox" id="rememberme" value="forever" /> Angemeldet bleiben</label>
            </p>
            <p class="login-submit">
                <input type="submit" name="wp-submit" id="wp-submit" class="button button-primary" value="Anmelden" />
                <input type="hidden" name="redirect_to" value="/" />
            </p>
        </form>
    </div>

    <div class="card">
        Passwort vergessen? <a href="/community/reset-password/">Setze hier ein neues!</a>
    </div>
</div>

<?php include("../inc/footer.php"); ?>
