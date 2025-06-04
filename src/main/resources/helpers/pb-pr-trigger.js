var ModalFirstStart = false;

define("teamcity/manual-build-pullrequest", [
  "aui/dialog2",
  "jquery",
  "bitbucket/util/server",
  "lodash",
  "aui/flag",
  "@atlassian/clientside-extensions-registry",
], function (dialog2, $, server_util, _, flag, registry) {
  var urlRegex = /(.+?)\/projects\/.+?\/repos\/.+?\/.*/;
  var urlParts = window.location.href.match(urlRegex);
  function getResourceUrl(context, resourceType) {
    return (
      urlParts[1] +
      "/rest/teamcity/latest/projects/" +
      context.project.key +
      "/repos/" +
      context.repository.slug +
      "/" +
      resourceType
    );
  }

  function getData(resourceUrl) {
    return server_util.ajax({
      type: "GET",
      url: resourceUrl,
      dataType: "json",
    });
  }

  function buttonPluginFactory(pluginAPI, context) {
    var pullRequest = context.pullRequest;

    var hookUrl =
      getResourceUrl(context, "getHookEnabled") + "?prid=" + pullRequest.id;

    function displayManualModal() {
      // start frame
      var modalDialogParentDiv = document.createElement("div");
      var modalCss =
        "display: block;position: fixed; z-index: 1; padding-top: 100px; left: 0; top: 0; width: 100%;height: 100%;  overflow: auto; background-color: rgb(0,0,0);  background-color: rgba(0,0,0,0.4);";
      modalDialogParentDiv.setAttribute("class", "modal");
      modalDialogParentDiv.setAttribute("id", "myModal");
      modalDialogParentDiv.setAttribute("style", modalCss);

      var modelContentCss =
        "background-color: #fefefe;margin: auto;border: 2px solid #888;width: 80%;";
      var modalContent = document.createElement("div");
      modalContent.setAttribute("class", "modal-content");
      modalContent.setAttribute("style", modelContentCss);

      modalDialogParentDiv.appendChild(modalContent);

      var iframeElement = document.createElement("iframe");
      iframeElement.setAttribute("id", "mainFrameId");
      iframeElement.setAttribute("style", "width: 100%; height: 500px;");
      iframeElement.setAttribute("frameborder", "0");

      var project = context.project;
      var repository = context.repository;
  
      var repoName = repository.name;
      var projectId = project.key;
      var branch = pullRequest.fromRef.displayId;
      var commit = pullRequest.fromRef.latestCommit;
      var prDest = pullRequest.toRef.displayId;

      var paramsString = "prId=" + pullRequest.id + "&projectId=" + projectId + "&repoName=" + repoName + "&branch=" + branch + "&commit=" + commit + "&prDest=" + prDest;

      iframeElement.setAttribute(
        "src",
        getResourceUrl(context, "ui/loadhtml") + "?page=index.html&" + paramsString
      );
      modalContent.appendChild(iframeElement);

      // add contetnt to modal
      document.body.appendChild(modalDialogParentDiv);
      ModalFirstStart = true;

      window.onclick = function (event) {
        if (!ModalFirstStart) {
            modalDialogParentDiv.style.display = "none";
            modalDialogParentDiv.parentNode.removeChild(modalDialogParentDiv);
        }

        ModalFirstStart = false;
      };
    }

    $.when(getData(hookUrl)).then(function (hookEnabled) {
      // if the hook is enabled and the user can manually trigger jobs
      if (hookEnabled) {
        // show the button
        pluginAPI.updateAttributes({
          hidden: false,
        });
      }
    });

    return {
      type: "button",
      onAction: function onAction() {
        displayManualModal();
      },
      label: "build & test",
      hidden: true,
    };
  }

  registry.registerExtension(
    "com.trimble.tekla:pr-trigger-teamcity",
    buttonPluginFactory
  );
});

$(document).ready(function () {
  require("teamcity/manual-build-pullrequest");
});
