<?php
// CSRF Protection (load first - other functions may need it)
require_once __DIR__ . '/functions/csrf.php';

require_once __DIR__ . '/functions/makeAdminStrapiApiCall.php';
require_once __DIR__ . '/functions/makePublicApiCall.php';
require_once __DIR__ . '/functions/makeStrapiApiCall.php';
require_once __DIR__ . '/functions/get_nearby_events_data.php';
require_once __DIR__ . '/functions/render_nearby_events_html.php';
require_once __DIR__ . '/functions/analyzeCsvFile.php';
require_once __DIR__ . '/functions/archiveNotificationsByType.php';
require_once __DIR__ . '/functions/assignGameToUser.php';
require_once __DIR__ . '/functions/getProfileData.php';
require_once __DIR__ . '/functions/checkFollowStatus.php';
require_once __DIR__ . '/functions/checkRateLimit.php';
require_once __DIR__ . '/functions/checkRegistrationCode.php';
require_once __DIR__ . '/functions/confirmProfile.php';
require_once __DIR__ . '/functions/confirmWaitlistToken.php';
require_once __DIR__ . '/functions/normalizeDate.php';
require_once __DIR__ . '/functions/createBoardGame.php';
require_once __DIR__ . '/functions/createProfile.php';
require_once __DIR__ . '/functions/createUserSession.php';
require_once __DIR__ . '/functions/deleteMediaFromStrapi.php';
require_once __DIR__ . '/functions/determinePostStatus.php';
require_once __DIR__ . '/functions/displayPostOptions.php';
require_once __DIR__ . '/functions/esc.php';
require_once __DIR__ . '/functions/formatDate.php';
require_once __DIR__ . '/functions/formatTime.php';
require_once __DIR__ . '/functions/formatRelativeTime.php';
require_once __DIR__ . '/functions/generateConfirmationToken.php';
require_once __DIR__ . '/functions/generateRandomString.php';
require_once __DIR__ . '/functions/generateUniqueSlug.php';
require_once __DIR__ . '/functions/get_meetings.php';
require_once __DIR__ . '/functions/getCoordinates.php';
require_once __DIR__ . '/functions/getCurrentPrivacyVersion.php';
require_once __DIR__ . '/functions/getEventsByLocation.php';
require_once __DIR__ . '/functions/getMatchScore.php';
require_once __DIR__ . '/functions/getMeetingsByZip.php';
require_once __DIR__ . '/functions/renderMeetingCard.php';
require_once __DIR__ . '/functions/getProfileByToken.php';
require_once __DIR__ . '/functions/getProfileDocumentId.php';
require_once __DIR__ . '/functions/getUserEvents.php';
require_once __DIR__ . '/functions/getUserLocations.php';
require_once __DIR__ . '/functions/getUserSales.php';
require_once __DIR__ . '/functions/haversineGreatCircleDistance.php';
require_once __DIR__ . '/functions/is_logged_in_explizit.php';
require_once __DIR__ . '/functions/is_logged_in_soft.php';
require_once __DIR__ . '/functions/isDocumentId.php';
require_once __DIR__ . '/functions/setScheduledDeletion.php';
require_once __DIR__ . '/functions/loginUser.php';
require_once __DIR__ . '/functions/logoutUser.php';
require_once __DIR__ . '/functions/mapConsent.php';
require_once __DIR__ . '/functions/markCodeAsUsed.php';
require_once __DIR__ . '/functions/registerUser.php';
require_once __DIR__ . '/functions/renderCreateMeetingButton.php';
require_once __DIR__ . '/functions/renderLikeLink.php';
require_once __DIR__ . '/functions/renderPosts.php';
require_once __DIR__ . '/functions/requireLogin.php';
require_once __DIR__ . '/functions/sendConfirmationMail.php';
require_once __DIR__ . '/functions/getMyInviteCodes.php';
require_once __DIR__ . '/functions/getSecureUploadPath.php';
require_once __DIR__ . '/functions/createOrFindBoardGame.php';

/*
$functionsDir = __DIR__ . '/functions';
require_once __DIR__ . '/config.php';
foreach (glob($functionsDir . '/*.php') as $file) {
    require_once $file;
}

*/