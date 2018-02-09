/**
 * Script to manage configurations within listeners tab.
 */
require([
    'jquery',
    'bitbucket/util/events',
    'com/trimble/tekla/teamcity/shared-storage',
    'jed/uuid'
], function ($, events, storage, uuid) {

    var listenersTableConrtoller = {
        _$listenersTableBody : undefined,
        _$referenceRegexp : undefined,
        _$listenerTargetId : undefined,
        _$listenerTarget : undefined,
        _$listenerDownStreamTriggerType : undefined,
        _$listenerDownStreamUrl : undefined,
        _listeners : undefined,
        _errors : undefined,
        
        /**
         * Initializes repository listeners table controller
         */
        init : function(listeners, errors) {
            console.log("init listener");
        
            if (listeners) {
                this._listeners = JSON.parse(listeners);
            } else {
                this._listeners = {};
            }

            if (errors) {
                this._errors = errors;
            } else {
                this._errors = {};
            }

            this._$listenersTableBody = $('#repository-listeners-table > tbody');
            this._$referenceRegexp = $('#referenceRegexp');
            this._$listenerTargetId = $('#listenerTargetId');
            this._$listenerTarget = $('#listenerTarget');
            this._$listenerDownStreamTriggerType = $('#downStreamTriggerType');
            this._$listenerDownStreamUrl = $('#downStreamUrl');


            $('#repository-listeners-table > thead button').off().on('click',$.proxy(this._addListenerHandler, this));

            this._drawTableContents();
        },

        /**
         * Redraws entire repository listeners table
         */
        _drawTableContents : function() {
            this._$listenersTableBody.empty();
            $.each(this._listeners, $.proxy(this._drawTableRow, this));
        },

        /**
         * Draws a single row in a table with listener configuration
         */
        _drawTableRow : function(listenerUUID, listener) {
            var $tableRow = $('<tr/>', {
                html : [$('<td/>', {
                    html : $.proxy(function() {
                        if (this._errors[listenerUUID]) {
                            var $errorIcon = $(aui.icons.icon({
                                tagName : 'span',
                                icon : 'error',
                                size : 'small',
                                accessibilityText : 'error',
                                useIconFont : true,
                                extraAttributes : {
                                    'title' : this._errors[listenerUUID]
                                }
                            }));
                            $errorIcon.tooltip({
                                gravity: 's',
                                className: 'aui-form-notification-tooltip aui-form-notification-tooltip-error'
                            });
                            return $errorIcon;
                        } else {
                            return '';
                        }
                    }, this)
                }), $('<td/>', {
                    html : listener.regexp
                }), $('<td/>', {
                    html : listener.targetId
                }), $('<td/>', {
                    html : listener.target
                }), $('<td/>', {
                    html : ''
                }), $('<td/>', {
                    html : listener.downStreamTriggerType
                }), $('<td/>', {
                    html : listener.downStreamUrl
                }), $('<td/>', {
                    html : aui.icons.icon({
                        tagName : 'a',
                        icon : 'remove',
                        size : 'small',
                        accessibilityText : 'delete',
                        useIconFont : true,
                        extraAttributes : {
                            'data-aui-trigger' : '',
                            'aria-controls' :'builds-info-dialog'
                        }
                    })
                })]
            });
            $tableRow.on('click', '.aui-iconfont-remove', listenerUUID, $.proxy(this._deleteListenerHandler, this));
            $tableRow.appendTo(this._$listenersTableBody);
        },

        /**
         * Event handler for button click to delete listener data from the table
         */
        _deleteListenerHandler : function(event) {
            event.preventDefault();
            var $delegateTarget = $(event.delegateTarget);
            $delegateTarget.hide('fast', function() {
                $delegateTarget.remove();
            });
            delete this._listeners[event.data];
        },

        /**
         * Event handler for button click to add listener data to the table
         */
        _addListenerHandler : function(event) {
            event.preventDefault();
            var listener = {
                regexp : this._$referenceRegexp.val(),
                targetId : this._$listenerTargetId.val(),
                target : this._$listenerTarget.val(),
                downStreamTriggerType : this._$listenerDownStreamTriggerType.val(),
                downStreamUrl : this._$listenerDownStreamUrl.val(),
            };

            this._$referenceRegexp.val('');
            this._$listenerTargetId.val('');
            this._$listenerDownStreamUrl.val('');

            var listenerUUID = uuid();
            this._listeners[listenerUUID] = listener;
            this._drawTableRow(listenerUUID, listener);
        },

        /**
         * Returns listeners as a string of JSON data
         *
         * @returns {string} JSON data
         */
        getListeners : function() {
            return JSON.stringify(this._listeners);
        }
    };

    /**
     * Trigger on dialog reload event "com.trimble.tekla.teamcity.hook.init"
     */
    events.on('com.trimble.tekla.teamcity.hook.init', function() {
        var $repositoryListenersJson = $('#repositoryListenersJson');
        Object.defineProperty($repositoryListenersJson[0], 'value', {
            get : function() {
                return listenersTableConrtoller.getListeners();
            },
            enumerable : true,
            configurable : true
        });

        listenersTableConrtoller.init(storage.config.repositoryListenersJson, storage.errors);
    });
});
