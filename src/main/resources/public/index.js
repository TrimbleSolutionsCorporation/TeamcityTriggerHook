
var disableTrigger = false;
var dialogLoaded = true;
var loadingBuilds = false;
var buildDependencies = [];

function getParamValue(paramName) {
  var url = window.location.search.substring(1); //get rid of "?" in querystring
  var qArray = url.split("&"); //get key-value pairs
  for (var i = 0; i < qArray.length; i++) {
    var pArr = qArray[i].split("="); //split key and value
    if (pArr[0] == paramName) return pArr[1]; //return value
  }
}

function getContextPath() {
  return window.location.href.split("/rest/")[0];
}

function resourceUrl(resourceName) {
  return (
    getContextPath() +
    "/rest/teamcity/latest/projects/" +
    getParamValue("projectId") +
    "/repos/" +
    getParamValue("repoName") +
    "/" +
    resourceName
  );
}

function closeModal() {
  var modal = window.parent.document.getElementById("myModal");
  modal.style.display = "none";
  modal.parentNode.removeChild(modal);
  dialogLoaded = false;
}

function GetBuildInformation(buildId, htmlId, callback) {
  var restPoint = resourceUrl("build") + "?id=" + buildId;

  $.ajax({
    url: restPoint,
    contentType: "application/json",
    dataType: "json",
    success: function (result) {
      callback(result, htmlId);
    },
    error: function (result) {
      console.log(result);
    },
    done: function (data) {},
  });
}

function TriggerExternalUrl(url, id, callback) {
  var restPoint =
    resourceUrl("triggerexternalurl") +
    "?url=" +
    encodeURIComponent(url) +
    "&method=GET";

  $.ajax({
    url: restPoint,
    contentType: "application/json",
    dataType: "json",
    success: function (result) {
      console.log(result);
      callback(result, id);
    },
    error: function (result) {
      console.log(result);
    },
    done: function (data) {},
  });
}

function CallExternalApiWithUrl(url, idReport) {
  var element = document.getElementById("ErrorMessageId");
  element.innerHTML = "";
  TriggerExternalUrl(url, idReport, function (data, idReportBack) {
    var element = document.getElementById("ErrorMessageId");
    element.innerHTML = data.message;
    element.style.color = "red";
  });
}

function TriggerBuildConfiguration(buildid, branch, callback) {
  var restPoint =
    resourceUrl("triggerbuild") +
    "?buildconfig=" +
    buildid +
    "&branch=" +
    getParamValue("branch") +
    "&prid=" +
    getParamValue("prId");

  $.ajax({
    url: restPoint,
    contentType: "application/json",
    dataType: "json",
    success: function (result) {
      var str = JSON.stringify(result, null, 2);
      console.log("Success : " + str);
      callback(result);
    },
    error: function (result) {
      var str = JSON.stringify(result, null, 2);
      console.log("Error : " + str);
    },
    done: function (data) {},
  });
}

function TriggerBuild(id, branch) {
  var element = document.getElementById("ErrorMessageId");
  element.innerHTML = "";
  if (disableTrigger) {
    return;
  }

  disableTrigger = true;
  TriggerBuildConfiguration(id, branch, function (data) {
    ReloadData();
    disableTrigger = false;
  });
}

function TriggerExternalTeamcityBuild(id, branch) {
  var element = document.getElementById("ErrorMessageId");
  element.innerHTML = "";
  if (disableTrigger) {
    return;
  }

  disableTrigger = true;
  TriggerBuildConfiguration(id, branch, function (data) {
    ReloadData();
    disableTrigger = false;
  });
}

var HookId = 0;

function CreateButtonWithRef(type, desc, wref, isEnabled) {
  var rowdata = "";
  HookId += 1;
  rowdata += '<div class="row">';
  rowdata += '  <div class="col-xs-8">';
  rowdata += "  <p>" + desc + "</p>";
  rowdata += "  </div>";

  rowdata += '  <div class="col-xs-2">';
  if (isEnabled) {
    if (type == "tab") {
      rowdata +=
        '<button class="btn btn-success" onclick=" window.open(\'' +
        wref +
        "','_blank')\"> Trigger</button>";
    }

    if (type == "rest") {
      rowdata +=
        '<button class="btn btn-success" onclick=" CallExternalApiWithUrl(\'' +
        wref +
        "', 'StatusExtHook_" +
        HookId +
        "')\"> Trigger</button>";
    }
  } else {
    rowdata +=
      '<button class="btn btn-danger"> Dependencies not ready</button>';
  }

  rowdata += "  </div>";

  rowdata += '  <div class="col-xs-2">';
  rowdata +=
    '  <span id="StatusExtHook_' +
    HookId +
    "\" style='margin-left: 10px;'></span>";
  rowdata += "  </div>";

  rowdata += "</div>";
  return rowdata;
}

function GetBuildStatusColumn(elem, isExternalTrigger) {
  var rowdata = "";

  // build status
  if (elem.queue.length === 0) {
    if (elem.state === "finished") {
      if (elem.status === "SUCCESS") {
        rowdata +=
          "<td class=\"buildstatuscol\"><i class='fa fa-check-circle' style='font-size:16px;color:green'></i></td>";
      } else {
        rowdata +=
          "<td class=\"buildstatuscol\"><i class='fa fa-exclamation-circle' style='font-size:16px;color:red'></i></td>";
      }
    } else {
      if (elem.status === "UNKNOWN") {
        rowdata +=
          "<td class='buildstatuscol\"><i class='fa fa-circle-o-notch fa-spin' style='font-size:16px;color:red'></i></td>";
      } else if (elem.status === "SUCCESS") {
        rowdata +=
          "<td class=\"buildstatuscol\"><i class='fa fa-circle-o-notch fa-spin' style='font-size:16px;color:green'></i></td>";
      } else if (elem.status === "not started" && !isExternalTrigger) {
        rowdata +=
          "<td class=\"buildstatuscol\"><i class='fa fa-exclamation-triangle' style='font-size:16px;color:red'></i></td>";
      } else if (elem.status === "not started" && isExternalTrigger) {
        rowdata += '<td class="buildstatuscol"><i></i></td>';
      } else {
        rowdata +=
          "<td class=\"buildstatuscol\"><i class='fa fa-circle-o-notch fa-spin' style='font-size:16px;color:red'></i></td>";
      }
    }
  } else {
    rowdata +=
      "<td class='buildstatuscol\"><i class='fa fa-circle-o-notch fa-spin' style='font-size:16px;color:green'></i></td>";
  }

  return rowdata;
}

function GetQueueColumn(elem) {
  var rowdata = "";
  rowdata += '<td class="buildqueuecol">';
  elem.queue.forEach(function (build) {
    rowdata +=
      "<p><a href='" +
      build.webUrl +
      "' target='_blank'>" +
      build.id +
      "<a></p>";
  });
  rowdata += "</td>";

  return rowdata;
}

function GetRemarksColumn(elem, isExternalDep, canTrigger) {
  var rowdata = "";

  if (isExternalDep && !canTrigger) {
    return '<td class="remarkscol">Dependencies are not ready or failing, cannot trigger</td>';
  }

  if (
    elem.queue.length === 0 &&
    elem.state === "not started" &&
    elem.status === "not started"
  ) {
    if (!isExternalDep) {
      return '<td class="remarkscol">no builds are queued for this pull request, something failed. pr needs manual trigger.</td>';
    } else {
      return '<td class="remarkscol">trigger to start</td>';
    }
  }

  if (elem.queue.length === 0) {
    rowdata += '<td class="remarkscol" id=\'' + elem.id + "_remarks'></td>";
    GetBuildInformation(elem.id, elem.id + "_remarks", function (data, htmlid) {
      var htmlid = document.getElementById(htmlid);
      var elem = "";
      for (var key in data) {
        if (key === "running-info") {
          elem += "<p>Total: " + data[key].percentageComplete + " %</p>";
        }
        if (key === "statusText") {
          elem += "<p>" + data[key] + "</p>";
        }
      }

      htmlid.innerHTML = elem;
    });
  } else {
    rowdata += '<td class="remarkscol"></td>';
  }

  return rowdata;
}

function GetTriggerColumn(elem, cantrigger, triggercallback) {
  var rowdata = "";

  if (
    elem.state === "not started" &&
    elem.status === "not started" &&
    elem.queue.length === 0 &&
    cantrigger === true
  ) {
    return (
      '<td class="triggerbuildcol"><span><i onclick="' +
      triggercallback +
      "('" +
      elem.key +
      "','" +
      elem.branchName +
      "');\" class='fa fa-play buttonhover'></span></td>"
    );
  }

  if (elem.state !== "finished" || elem.queue.length > 0 || !cantrigger) {
    rowdata += '<td class="triggerbuildcol"><span></span></td>';
  } else {
    rowdata +=
      '<td class="triggerbuildcol"><span><i onclick="' +
      triggercallback +
      "('" +
      elem.key +
      "','" +
      elem.branchName +
      "');\" class='fa fa-play buttonhover'></span></td>";
  }

  return rowdata;
}

function CreateBuildRow(elem, cantrigger, triggerCallback, isExternalTrigger) {
  // start row
  var rowdata = "<tr>";

  // trigger coloum trigger
  rowdata += GetTriggerColumn(elem, cantrigger, triggerCallback);

  // build configuration href
  if (elem.buildTypeId !== "") {
    rowdata +=
      '<td class="buildconfigcol"><a href=\'' +
      elem.webUrl +
      "' target='_blank'><div>" +
      elem.buildTypeId +
      "</div></a></td>";
  } else {
    rowdata +=
      '<td class="buildconfigcol"><a href=\'' +
      elem.webUrl +
      "' target='_blank'><div>" +
      elem.key +
      "</div></a></td>";
  }

  // build status
  rowdata += GetBuildStatusColumn(elem, isExternalTrigger);

  // queued elements
  rowdata += GetQueueColumn(elem);

  // remarks
  rowdata += GetRemarksColumn(elem, isExternalTrigger, cantrigger);

  // close row
  rowdata += "</tr>";

  return rowdata;
}

function GetBuildFromJson(
  json,
  jsonqueue,
  buildConfigurationKey,
  branchName,
  wrefconfig
) {
  var queuedBuilds = [];
  if (jsonqueue.count > 0) {
    jsonqueue.build.forEach(function (build) {
      if (branchName.endsWith(build.branchName)) {
        var queuedbuild = {
          id: build.id,
          buildTypeId: build.buildTypeId,
          state: "queued",
          href: build.href,
          webUrl: build.webUrl,
          branch: branchName,
        };
        queuedBuilds.push(queuedbuild);
      }
    });
  }

  if (json.count === 0) {
    // json builds
    var build = {
      key: buildConfigurationKey,
      id: "",
      buildTypeId: "",
      number: "",
      status: "not started",
      state: "not started",
      branchName: branchName,
      href: "",
      webUrl: wrefconfig,
      queue: queuedBuilds,
    };

    if (queuedBuilds.length > 0) {
      build.state = "queued";
      build.buildTypeId = queuedBuilds[0].buildTypeId;
    }
    return build;
  } else {
    var build = {
      key: buildConfigurationKey,
      id: json.build[0].id,
      buildTypeId: json.build[0].buildTypeId,
      number: json.build[0].number,
      status: json.build[0].status,
      state: json.build[0].state,
      branchName: branchName,
      href: json.build[0].href,
      webUrl: json.build[0].webUrl,
      queue: queuedBuilds,
    };
    return build;
  }
}

function GetExternalBuildsConfigurations(id, callback) {
  var restPoint =
    resourceUrl("externalbuilds") +
    "?id=" +
    id +
    "&prid=" +
    getParamValue("prId") +
    "&branch=" +
    getParamValue("branch") +
    "&hash=" +
    getParamValue("commit");

  $.ajax({
    url: restPoint,
    contentType: "application/json",
    dataType: "json",
    success: function (result) {
      callback(result, getParamValue("branch"), getParamValue("prId"));
    },
    error: function (result) {
      console.log("Error : " + result);
    },
    done: function (data) {},
  });
}

function GetExternalBuildConfigurationsGroup(id) {
  GetExternalBuildsConfigurations(id, function (data, branchName, prid) {
    if (typeof data.status !== "undefined") {
      // the variable is defined
      if (data.status === "error") {
        return;
      }
    }

    if (data) {
      if (id === "External1Id") {
        var name = "Tests";
        document.getElementById(id).innerHTML = name;
        document.getElementById(id).style.display = "inherit";

        // get dependent builds
        var canTriggerBuilds = true;
        var dependentBuilds = [];
        for (var key in data) {
          if (key.endsWith("_dep") && !key.endsWith("_queue")) {
            var json = JSON.parse(data[key]);
            var jsonqueue = JSON.parse(data[key + "_queue"]);
            var wrefconfig = data[key + "_wref"];
            var build = GetBuildFromJson(
              json,
              jsonqueue,
              key.replace("_dep", ""),
              branchName,
              wrefconfig
            );
            if (build.state !== "finished" || build.status !== "SUCCESS") {
              canTriggerBuilds = false;
            }
            dependentBuilds.push(build);
          }
        }

        // get builds to trigger
        var externalBuilds = [];
        for (var key in data) {
          if (
            key.endsWith("_build") &&
            !key.endsWith("_queue") &&
            !key.endsWith("_wref") &&
            !key.endsWith("_branch")
          ) {
            var json = JSON.parse(data[key]);
            var jsonqueue = JSON.parse(data[key + "_queue"]);
            var wref = data[key + "_wref"];
            var build = GetBuildFromJson(
              json,
              jsonqueue,
              key.replace("_build", ""),
              branchName,
              wref
            );
            externalBuilds.push(build);
          }
        }

        var rowdata = "";
        dependentBuilds.forEach(function (build) {
          rowdata += CreateBuildRow(build, false, false);
        });

        var tableBody = document.getElementById("tableDependenciesId");
        tableBody.innerHTML = rowdata;

        var rowdata = "";
        externalBuilds.forEach(function (build) {
          rowdata += CreateBuildRow(
            build,
            canTriggerBuilds,
            "TriggerExternalTeamcityBuild",
            true
          );
        });

        var tableBody = document.getElementById("tableBuildsId");
        tableBody.innerHTML = rowdata;
      }

      if (id === "External2Id") {
        var name = JSON.parse(data["ExternalBuildsTwoNameId"]).name;
        if (name === "") {
          document.getElementById(id).style.display = "none";
          return;
        }

        var extData = "";

        try {
          extData = JSON.parse(data["ext_references"]);
        } catch (err) {
          document.getElementById(id).style.display = "none";
          return;
        }

        document.getElementById(id).innerHTML = name;
        document.getElementById(id).style.display = "inherit";

        // get dependent builds
        var canTriggerBuilds = true;
        var dependentBuilds = [];
        for (var key in data) {
          if (key.endsWith("_dep") && !key.endsWith("_queue")) {
            var json = JSON.parse(data[key]);
            var jsonqueue = JSON.parse(data[key + "_queue"]);
            var wrefconfig = data[key + "_wref"];
            var build = GetBuildFromJson(
              json,
              jsonqueue,
              key.replace("_dep", ""),
              branchName,
              wrefconfig
            );
            if (
              build.state !== "finished" ||
              build.status !== "SUCCESS" ||
              build.queue.length !== 0
            ) {
              canTriggerBuilds = false;
            }
            dependentBuilds.push(build);
          }
        }

        var rowdata = "";
        dependentBuilds.forEach(function (build) {
          rowdata += CreateBuildRow(build, false, false);
        });

        var tableBody = document.getElementById(
          "tableDependenciesExternalTeamcityId"
        );
        tableBody.innerHTML = rowdata;

        var container = document.getElementById(
          "ExternalTeamcityBuildsContainerId"
        );
        var dataRow = "";
        var extData = JSON.parse(data["ext_references"]);
        HookId = 0;
        extData.forEach(function (datainternal) {
          var hook = JSON.parse(datainternal);
          var href = hook.url
            .replace("{branch}", branchName)
            .replace("{pr}", prid);
          dataRow += CreateButtonWithRef(
            hook.type,
            hook.desc,
            href,
            canTriggerBuilds
          );
        });
        container.innerHTML = dataRow;
      }
    }
  });
}

function GetBuildsConfigurations(callback) {
  var restPoint =
    resourceUrl("builds") +
    "?prid=" +
    getParamValue("prId") +
    "&branch=" +
    getParamValue("branch") +
    "&hash=" +
    getParamValue("commit");

  $.ajax({
    url: restPoint,
    contentType: "application/json",
    dataType: "json",
    success: function (result) {
      console.log(result);
      callback(result, getParamValue("branch"));
    },
    error: function (result) {
      console.log(result);
    },
    done: function (data) {},
  });
}

function GetMainBuilds() {
  GetBuildsConfigurations(function (data, branchName) {
    if (typeof data.status !== "undefined") {
      // the variable is defined
      if (data.status === "error") {
        return;
      }
    }
    var rowdata = "";
    var builds = [];
    for (var key in data) {
      if (
        data.hasOwnProperty(key) &&
        !key.endsWith("_queue") &&
        !key.endsWith("_wref")
      ) {
        var json = JSON.parse(data[key]);
        if (typeof json.exception !== "undefined") {
          // handle error
          continue;
        }

        var jsonqueue = JSON.parse(data[key + "_queue"]);
        var wref = data[key + "_wref"];
        var build = GetBuildFromJson(json, jsonqueue, key, branchName, wref);
        builds.push(build);
      }
    }

    var rowdata = "";
    builds.forEach(function (build) {
      rowdata += CreateBuildRow(build, true, "TriggerBuild", false);
    });

    var tableBody = document.getElementById("tableBuildsContentId");
    tableBody.innerHTML = rowdata;
  });
}

function ReloadData() {
    if (loadingBuilds) {
      var elementLog = document.getElementById("ErrorMessageId");
      elementLog.innerHTML = "Please, try again in 10 seconds, too much polling...";
      elementLog.style.color = "blue";
      return;
    }
    var element = document.getElementById("ErrorMessageId");
    element.innerHTML = "";
    loadingBuilds = true;
    if (dialogLoaded) {
      GetMainBuilds();
      setTimeout(function () {
        GetExternalBuildConfigurationsGroup("External1Id");
        GetExternalBuildConfigurationsGroup("External2Id");
        setTimeout(function () {
          loadingBuilds = false;
        }, 7000);
      }, 3000);
    }
}

$(document).ready(function () {
  console.log("ready!");
  buildDependencies = [];
  document.getElementById("External2Id").style.display = "none";
  document.getElementById("External1Id").style.display = "none";
  GetMainBuilds();
  GetExternalBuildConfigurationsGroup("External1Id");
  GetExternalBuildConfigurationsGroup("External2Id");
  dialogLoaded = true;
  ReloadData();
});
