/**
 * Script to manage events within configuration hook dialog
 */
require([
    'bitbucket/util/events'
], function (events) {
    /**
     * Trigger on dialog reload event "com.trimble.tekla.teamcity.hook.init"
     */
    events.on('com.trimble.tekla.teamcity.hook.init', function() {
        AJS.tabs.setup();
    });
});
