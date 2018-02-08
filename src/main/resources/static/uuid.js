/**
 * A module for generating UUID's. Code snippet adopted from https://gist.github.com/jed/982883
 *
 * This is a quick hack to get development forward and get UUID generation support fast.
 *
 * Suggesting to use one of the https://www.npmjs.com/search?q=uuid modules for production implementation.
 *
 * @returns {function} UUID 4 generating function
 */

define('jed/uuid', function() {
    var uuid = function(a){return a?(a^Math.random()*16>>a/4).toString(16):([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g,uuid)};
    return uuid;
});
