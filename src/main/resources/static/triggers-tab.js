/**
 * Script to manage configurations within triggers tab.
 */
require([
    'jquery',
    'bitbucket/util/events',
    'com/trimble/tekla/teamcity/shared-storage',
    'jed/uuid'
], function ($, events, storage, uuid) {

    var triggersTableConrtoller = {
        _triggers : undefined,

        /**
         * Initializes triggers table controller
         */
        init : function(triggers, errors) {
            if (triggers) {
                this._triggers = JSON.parse(triggers);
            } else {
                this._triggers = {};
            }

            if (errors) {
                this._errors = errors;
            } else {
                this._errors = {};
            }
        },

        /**
         * Returns triggers as a string of JSON data
         *
         * @returns {string} JSON data
         */
        getTriggers : function() {
            return JSON.stringify(this._triggers);
        }
    };

    /**
     * Trigger on dialog reload event "com.trimble.tekla.teamcity.hook.init"
     */
    events.on('com.trimble.tekla.teamcity.hook.init', function() {
        var $triggersJson = $('#triggersJson');
        Object.defineProperty($triggersJson[0], 'value', {
            get : function() {
                return triggersTableConrtoller.getTriggers();
            },
            enumerable : true,
            configurable : true
        });

        triggersTableConrtoller.init(storage.config.triggersJson, storage.errors);
    });
});
