/**
 * Script to manage configurations within triggers tab.
 */
require([
    'aui',
    'jquery',
    'bitbucket/util/events',
    'com/trimble/tekla/teamcity/shared-storage',
    'jed/uuid'
], function(AJS, $, events, storage, uuid) {

    var triggersTableConrtoller = {
        _$triggersTable : undefined,
        _$triggersTableBody : undefined,
        _$referenceRegexp : undefined,
        _$triggerTarget : undefined,
        _$triggerType : undefined,
        _$triggerOnEmptyBranches : undefined,
        _$triggerOnPullRequest : undefined,
        _$cancelRunningBuilds : undefined,
        _$downStreamTriggerType : undefined,
        _$downStreamUrl : undefined,
        _triggers : undefined,
        _errors : undefined,

        /**
         * Initializes repository triggers table controller
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

            this._$triggersTable = $('#repository-triggers-table');
            this._$triggersTableBody = $('#repository-triggers-table > tbody');
            this._$referenceRegexp = $('#referenceRegexp');
            this._$triggerTarget = $('#triggerTarget');
            this._$triggerType = $('#triggerType');
            this._$triggerOnEmptyBranches = $('#triggerOnEmptyBranches');
            this._$triggerOnPullRequest = $('#triggerOnPullRequest');
            this._$cancelRunningBuilds = $('#cancelRunningBuilds');
            this._$downStreamTriggerType = $('#downStreamTriggerType');
            this._$downStreamUrl = $('#downStreamUrl');

            $('#addTriggerButton').off().on('click', $.proxy(this._addTriggerHandler, this));

            this._drawTableContents();

            if (!$.isEmptyObject(this._triggers)) {
                this._$triggersTable.show();
            }
        },

        /**
         * Redraws entire repository triggers table
         */
        _drawTableContents : function() {
            this._$triggersTableBody.empty();
            $.each(this._triggers, $.proxy(this._drawTableRow, this));
        },

        /**
         * Draws a single row in a table with trigger configuration
         */
        _drawTableRow : function(triggerUUID, trigger) {
            var $tableRow = $('<tr/>', {
                html : [$('<td/>', {
                    html : $.proxy(function() {
                        if (this._errors[triggerUUID]) {
                            var $errorIcon = $(aui.icons.icon({
                                tagName : 'span',
                                icon : 'error',
                                size : 'small',
                                accessibilityText : 'error',
                                useIconFont : true,
                                extraAttributes : {
                                    'title' : this._errors[triggerUUID]
                                }
                            }));
                            $errorIcon.tooltip({
                                gravity : 's',
                                className : 'aui-form-notification-tooltip aui-form-notification-tooltip-error'
                            });
                            return $errorIcon;
                        } else {
                            return '';
                        }
                    }, this)
                }), $('<td/>', {
                    text : trigger.regexp
                }), $('<td/>', {
                    text : trigger.target
                }), $('<td/>', {
                    text : trigger.type
                }), $('<td/>', {
                    html : [$('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.cancel.builds')
                        }), $('<span/>', {
                            text : trigger.cancelRunningBuilds
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.pull.request')
                        }), $('<span/>', {
                            text : trigger.triggerOnPullRequest
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.empty.branches')
                        }), $('<span/>', {
                            text : trigger.triggerOnEmptyBranches
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.target')
                        }), $('<span/>', {
                            text : trigger.downStreamUrl
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.type')
                        }), $('<span/>', {
                            text : trigger.downStreamTriggerType
                        })]
                    })]
                }), $('<td/>', {
                    html : aui.icons.icon({
                        tagName : 'a',
                        icon : 'remove',
                        size : 'small',
                        accessibilityText : 'delete',
                        useIconFont : true,
                        extraAttributes : {
                            'data-aui-trigger' : '',
                            'aria-controls' : 'builds-info-dialog'
                        }
                    })
                })]
            });
            $tableRow.on('click', '.aui-iconfont-remove', triggerUUID, $.proxy(this._deleteTriggerHandler, this));
            $tableRow.appendTo(this._$triggersTableBody);
        },

        /**
         * Event handler for button click to delete trigger data from the table
         */
        _deleteTriggerHandler : function(event) {
            event.preventDefault();
            var $delegateTarget = $(event.delegateTarget);
            $delegateTarget.hide('fast', function() {
                $delegateTarget.remove();
            });
            delete this._triggers[event.data];
            if ($.isEmptyObject(this._triggers)) {
                this._$triggersTable.hide();
            }
        },

        /**
         * Event handler for button click to add trigger data to the table
         */
        _addTriggerHandler : function(event) {
            event.preventDefault();

            var trigger = {
                regexp : this._$referenceRegexp.val(),
                target : this._$triggerTarget.val(),
                type : this._$triggerType.val(),
                triggerOnEmptyBranches : this._$triggerOnEmptyBranches[0].checked,
                triggerOnPullRequest : this._$triggerOnPullRequest[0].checked,
                cancelRunningBuilds : this._$cancelRunningBuilds[0].checked,
                downStreamTriggerType : this._$downStreamTriggerType.val(),
                downStreamUrl : this._$downStreamUrl.val(),
            };

            this._$referenceRegexp.val('');
            this._$triggerTarget.val('');
            this._$downStreamUrl.val('');

            this._$triggerOnEmptyBranches[0].checked = true;
            this._$triggerOnPullRequest[0].checked = false;
            this._$cancelRunningBuilds[0].checked = false;

            var triggerUUID = uuid();
            this._triggers[triggerUUID] = trigger;
            this._drawTableRow(triggerUUID, trigger);

            this._$triggersTable.show();
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
        var $repositoryTriggersJson = $('#repositoryTriggersJson');
        Object.defineProperty($repositoryTriggersJson[0], 'value', {
            get : function() {
                return triggersTableConrtoller.getTriggers();
            },
            enumerable : true,
            configurable : true
        });

        triggersTableConrtoller.init(storage.config.repositoryTriggersJson, storage.errors);
    });
});
