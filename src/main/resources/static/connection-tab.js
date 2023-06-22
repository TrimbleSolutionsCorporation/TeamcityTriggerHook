/**
 * Script to manage events within connection tab.
 */
require([
    'jquery',
    'bitbucket/util/events',
    'bitbucket/util/navbuilder',
    'bitbucket/util/state'
], function ($, events, navBuilder, pageState) {       
    /**
     * Trigger on dialog reload event "com.trimble.tekla.teamcity.hook.init"
     */
    events.on('com.trimble.tekla.teamcity.hook.init', function() {
        var $teamCityUrlField = $('#teamCityUrl');
        var $teamCityUserNameField = $('#teamCityUserName');
        var $teamCityPasswordField = $('#teamCityPassword');
        var $testTeamCityConnectionButton = $('#testTeamCityConnection');
        var $isDebugEnabled = $('#isDebugEnabled');
        var $testStatusDiv = $testTeamCityConnectionButton.next();
        
        var selectedProjectKey = pageState.getRepository()["project"]["key"];
        var selectedRepositorySlug = pageState.getRepository()["slug"];
                    
        var testUrlNavBuilder = navBuilder.rest('teamcity')
                .addPathComponents("projects")
                .addPathComponents(selectedProjectKey)
                .addPathComponents("repos")
                .addPathComponents(selectedRepositorySlug)
                .addPathComponents('testconnection');
        
        /**
         * Connection validation button handler
         */
        $testTeamCityConnectionButton.click(function() {
            $testStatusDiv.removeClass('error');
            $testStatusDiv.html('Testing ...');
            var testUrl = testUrlNavBuilder.withParams({
                url : $teamCityUrlField.val(),
                username : $teamCityUserNameField.val(),
                password : $teamCityPasswordField.val(),
                debugon : $isDebugEnabled[0].checked
            }).build();
        
            $.ajax({
                url : testUrl
            }).done(function(data) {
                $teamCityPasswordField.val(data);
                $testStatusDiv.html('OK');
            }).fail(function(xjr) {
                $testStatusDiv.addClass('error');
                if (xjr.responseXML || xjr.responseJSON) {
                    $testStatusDiv.html('Internal error. REST service is not running...');
                } else {
                    $testStatusDiv.html(xjr.responseText);
                }
            });
        });
    });
});
