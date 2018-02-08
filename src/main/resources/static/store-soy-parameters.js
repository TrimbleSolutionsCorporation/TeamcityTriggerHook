/**
 * Function meant to be called from the entry point closure template to store template parameters.
 *
 * Those parameters will be later reused in plugins RequireJS modules.
 */
window.com === window.com || {};
window.com.trimble === window.com.trimble || {};
window.com.trimble.tekla === window.com.trimble.tekla || {};
window.com.trimble.tekla.teamcity === window.com.trimble.tekla.teamcity || {};

window.com.trimble.tekla.teamcity.storeSoyParameters = function(opt_data) {
    require(['com/trimble/tekla/teamcity/shared-storage'], function(storage) {
        storage.config = opt_data.config;
        storage.errors = opt_data.errors;
    });
    return '';
};
