/**
 * Script to manage configurations within triggers tab.
 */
require([
    'jquery',
    'bitbucket/util/events',
    'com/trimble/tekla/teamcity/shared-storage',
    'jed/uuid'
], function($, events, storage, uuid) {

    var triggersTableConrtoller = {
        _$triggersTable : undefined,
        _$triggersTableBody : undefined,
        _$referenceRegex : undefined,
        _$triggerTarget : undefined,
        _$triggerType : undefined,
        _$triggerInclusion : undefined,
        _$triggerExclusion : undefined,
        _$triggerOnEmptyBranches : undefined,
        _$triggerOnPullRequest : undefined,
        _$hideOnPullRequest : undefined,
        _$triggerWhenNoReviewers : undefined,
        _$cancelRunningBuilds : undefined,
        _$cancelDependencies : undefined,
        _$downStreamTriggerDescription : undefined,
        _$downStreamTriggerType : undefined,
        _$downStreamTriggerTarget : undefined,
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
            this._$referenceRegex = $('#referenceRegex');
            this._$triggerTarget = $('#triggerTarget');
            this._$triggerType = $('#triggerType');

            this._$triggerInclusion = $('#triggerInclusion');
            this._$triggerExclusion = $('#triggerExclusion');

            this._$triggerOnEmptyBranches = $('#triggerOnEmptyBranches');
            this._$triggerWhenNoReviewers = $('#triggerWhenNoReviewers');
            this._$triggerOnPullRequest = $('#triggerOnPullRequest');
            this._$hideOnPullRequest = $('#hideOnPullRequest');
            this._$cancelRunningBuilds = $('#cancelRunningBuilds');
            this._$cancelDependencies = $('#cancelDependencies');
            this._$downStreamTriggerType = $('#downStreamTriggerType');
            this._$downStreamTriggerDescription = $('#downStreamTriggerDescription');
            this._$downStreamTriggerTarget = $('#downStreamTriggerTarget');

            $('#addTriggerButton').off().on('click', $.proxy(this._addTriggerHandler, this));
            $('#exportConfigButton').off().on('click', $.proxy(this._exportTriggerHandler, this));
            $('#importConfigButton').off().on('click', $.proxy(this._importTriggerHandler, this));

            var inputElement = document.getElementById('importDialogId');
            if(inputElement === null) {
                inputElement = document.createElement("input");
                inputElement.type = "file";
                inputElement.id = "importDialogId";
                document.body.appendChild(inputElement);
                $('#importDialogId').off().on('change', $.proxy(this._loadFileHandler, this));
            }

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
            if(typeof trigger.triggerWhenNoReviewers === 'undefined'){
                trigger.triggerWhenNoReviewers = true;
            }
            if(typeof trigger.triggerOnEmptyBranches === 'undefined'){
                trigger.triggerOnEmptyBranches = true;
            }            
            if(typeof trigger.cancelDependencies === 'undefined'){
                trigger.cancelDependencies = false;
            }            
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
                    text : trigger.regex
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
                            text : AJS.I18n.getText('triggers.column.cancel.dependencies')
                        }), $('<span/>', {
                            text : trigger.cancelDependencies
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.pull.request')
                        }), $('<span/>', {
                            text : trigger.triggerOnPullRequest
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.hide')
                        }), $('<span/>', {
                            text : trigger.hideOnPullRequest
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.empty.branches')
                        }), $('<span/>', {
                            text : trigger.triggerOnEmptyBranches
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.column.no.reviewers')
                        }), $('<span/>', {
                            text : trigger.triggerWhenNoReviewers
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.inclusion')
                        }), $('<span/>', {
                            text : trigger.triggerInclusion
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.exclusion')
                        }), $('<span/>', {
                            text : trigger.triggerExclusion
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.description')
                        }), $('<span/>', {
                            text : trigger.downStreamTriggerDescription
                        })]
                    }), $('<span/>', {
                        html : [$('<span/>', {
                            text : AJS.I18n.getText('triggers.target')
                        }), $('<span/>', {
                            text : trigger.downStreamTriggerTarget
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
                regex : this._$referenceRegex.val(),
                target : this._$triggerTarget.val(),
                type : this._$triggerType.val(),
                triggerOnEmptyBranches : this._$triggerOnEmptyBranches[0].checked,
                triggerWhenNoReviewers : this._$triggerWhenNoReviewers[0].checked,
                triggerOnPullRequest : this._$triggerOnPullRequest[0].checked,
                hideOnPullRequest : this._$hideOnPullRequest[0].checked,
                cancelRunningBuilds : this._$cancelRunningBuilds[0].checked,
                cancelDependencies : this._$cancelDependencies[0].checked,
                triggerInclusion : this._$triggerInclusion.val(),
                triggerExclusion : this._$triggerExclusion.val(),
                downStreamTriggerType : this._$downStreamTriggerType.val(),
                downStreamTriggerDescription : this._$downStreamTriggerDescription.val(),
                downStreamTriggerTarget : this._$downStreamTriggerTarget.val()
            };

            this._$referenceRegex.val('');
            this._$triggerTarget.val('');
            this._$downStreamTriggerTarget.val('');

            this._$triggerInclusion.val('');
            this._$triggerExclusion.val('');

            this._$triggerOnEmptyBranches[0].checked = true;
            this._$triggerWhenNoReviewers[0].checked = true;
            this._$triggerOnPullRequest[0].checked = false;
            this._$hideOnPullRequest[0].checked = false;
            this._$cancelRunningBuilds[0].checked = false;
            this._$cancelDependencies[0].checked = false;

            var triggerUUID = uuid();
            this._triggers[triggerUUID] = trigger;
            this._drawTableRow(triggerUUID, trigger);

            this._$triggersTable.show();
        },

        /**
         * Event handler for button to import configuration
         */
        _importTriggerHandler : function(event) {

            event.preventDefault();
            var inputElement = document.getElementById('importDialogId')
            inputElement.click();
        },

        /**
         * Event handler for button click to add trigger data to the table
         */
        _loadFileHandler : function(evt) {
            evt.preventDefault();
            var files = evt.target.files;
            var file = files[0];
            var hookContext = this;
            this.reader = new FileReader();
            this.reader.onload = function(event) {
                event.preventDefault();
                var data = "";
                try {
                    data = JSON.parse(event.target.result);
                }
                catch(err) {
                    alert("Cannot parse File, try again");
                    return;
                }

                hookContext._triggers = data;
                hookContext._$triggersTableBody.empty();
                $.each(hookContext._triggers, $.proxy(hookContext._drawTableRow, hookContext));
                hookContext._$triggersTable.show();       
            }
            this.reader.readAsText(file);
        },

        /**
         * Event handler for button to export configuration
         */
        _exportTriggerHandler : function(event) {
            event.preventDefault();
            var configuration = JSON.stringify(this._triggers, null, 4);
            var element = document.createElement('a');
            element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(configuration));
            element.setAttribute('download', "hook_configuration.json");

            element.style.display = 'none';
            document.body.appendChild(element);

            element.click();

            document.body.removeChild(element);
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
