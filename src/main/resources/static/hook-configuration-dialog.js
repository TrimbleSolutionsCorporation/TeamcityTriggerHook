/**
 * Script to manage events within configuration hook dialog
 */
require([
    'aui',
    'bitbucket/util/events'
], function (AJS, events) {

    /**
     * Trigger on dialog reload event "com.trimble.tekla.teamcity.hook.init"
     */
    events.on('com.trimble.tekla.teamcity.hook.init', function() {
        AJS.tabs.setup();
    });
});
