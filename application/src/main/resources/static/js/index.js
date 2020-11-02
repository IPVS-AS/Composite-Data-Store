$('#notification_red').hide();
$('#notification_green').hide();
var policydata = '';
var datamodelsdata = '';
var lastEditedId = '';

var editor_model = CodeMirror.fromTextArea(document.getElementById("model"), {
  lineNumbers: true,
  indentUnit: 4,
  styleActiveLine: true,
  matchBrackets: true,
  mode: 'casbin-conf',
  lineWrapping: true,
  theme: 'monokai',
});

var editor_policy = CodeMirror.fromTextArea(document.getElementById("policy"), {
  lineNumbers: true,
  indentUnit: 4,
  styleActiveLine: true,
  matchBrackets: true,
  mode: 'casbin-csv',
  lineWrapping: true,
  theme: 'monokai',
});

var editor_datamodel = CodeMirror.fromTextArea(
    document.getElementById("datamodeleditor"), {
      lineNumbers: true,
      indentUnit: 4,
      styleActiveLine: true,
      matchBrackets: true,
      mode: {name: "javascript", jsonld: true},
      lineWrapping: true,
      theme: 'monokai'
    });

editor_datamodel.setSize(466, 450);

$.get("/getPolicies", function (data, status) {
  policydata = data;
  //load access policy
  editor_model.setValue(data.accesspolicy["conf"]);
  editor_policy.setValue(data.accesspolicy["csv"]);

  var listitems = '<option value="accesspolicy">Access Policy</option><optgroup label="Execution Policies">';

  $.each(data.execution, function (key, value) {
    listitems += '<option value=' + key + '>' + key + '</option>';
  });
  listitems += '</optgroup>';
  $('#policyselect').append(listitems);

});

$.get("/getDatamodel", function (data, status) {
  datamodelsdata = data;

  // create datasource view
  let dataSourceHtml = '';

  $.each(data.datamodels.dataSources, function (key, value) {
    console.log(key, value)

    let criteria = '<span class="badge badge-secondary">No Criteria</span>';
    if (value.criteria != "") {
      criteria = '<span class="badge badge-primary">Has Criteria</span>';
    }

    let adapter = '<span class="badge badge-warning">No Adapter</span>';
    if (data.sourceAdapters.indexOf(key) != -1) {
      adapter = '<span class="badge badge-success">Adapter exists</span>';
    }

    let sourceEntryHtml =
        '<div class="list-group-item list-group-item-action"><div'
        + ' class="d-flex w-100 justify-content-between"><h5 class="mb-1">'
        + key + '</h5><p>' + adapter
        + '</p></div><div class="d-flex w-100'
        + ' justify-content-between"><div>'
        + criteria
        + '</div>'
        + '<span>'
        + '<button type="button" onclick="openEditWindow(\'' + key
        + '\',\'source\');"class="btn btn-outline-info btn-sm"><img'
        + ' src="./baseline-edit-24px.svg"></button>'
        + '&nbsp;<button type="button" onclick="deleteDatamodel(\'' + key
        + '\',\'source\');"class="btn btn-outline-danger btn-sm"><img'
        + '  src="./baseline-delete_forever-24px.svg"></button>'
        + '</span></div></div>';

    dataSourceHtml += sourceEntryHtml;

  });

  $('#datasources').html(dataSourceHtml);

  // create datadestination view
  let datadestinationHtml = '';

  $.each(data.datamodels.dataDestinations, function (key, value) {

    let functionality = '';

    $.each(value.functionalities, function (index, entry) {
      functionality += '<span class="badge badge-secondary">' + entry
          + '</span>&nbsp;';
    });

    let adapter = '<span class="badge badge-warning">No Adapter</span>';

    if (data.destinationAdapters.indexOf(key) != -1) {
      adapter = '<span class="badge badge-success">Adapter exists</span>';
    }

    let destinationEntryHtml =
        '<div class="list-group-item list-group-item-action"><div'
        + ' class="d-flex w-100 justify-content-between"><h5 class="mb-1">'
        + key + '</h5><p>' + adapter
        + '</p></div><div class="d-flex w-100 justify-content-between"><div>'
        + functionality
        + '</div>'
        + '<span>'
        + '<button type="button" onclick="openEditWindow(\'' + key
        + '\',\'destination\');"class="btn btn-outline-info btn-sm"><img'
        + '  src="./baseline-edit-24px.svg"></button>'
        + '&nbsp;<button type="button" onclick="deleteDatamodel(\'' + key
        + '\',\'destination\');"class="btn btn-outline-danger btn-sm"><img'
        + '  src="./baseline-delete_forever-24px.svg"></button>'
        + '</span></div></div>';

    datadestinationHtml += destinationEntryHtml;

  });

  $('#datadestinations').html(datadestinationHtml);

});

// on select change
$("#policyselect").change(function () {
  var value = this.value;

  if (value == "accesspolicy") {
    editor_model.setValue(policydata.accesspolicy["conf"]);
    editor_policy.setValue(policydata.accesspolicy["csv"]);
  } else {
    editor_model.setValue(policydata.execution[value]["conf"]);
    editor_policy.setValue(policydata.execution[value]["csv"]);
  }

});

// Button clicks

$("#btn_validate").click(function () {
  $('#notification_red').hide();
  $('#notification_green').hide();

  $.post("/validatePolicy",
      {csv: editor_policy.getValue(), conf: editor_model.getValue()})
  .done(function (data) {

    if (data == "true") {
      $('#notification_green').html("Syntax seems to be alright!");
      $('#notification_green').show();
    } else {
      $('#notification_red').html(
          "Something seems to be wrong! <hr> <code>" + data + "</code>");
      $('#notification_red').show();
    }

  });

});

$("#btn_savePolicy").click(function () {

  $('#notification_red').hide();
  $('#notification_green').hide();

  $.post("/writePolicies", {
    filename: $("#policyselect").val(),
    csv: editor_policy.getValue(),
    conf: editor_model.getValue()
  })
  .done(function (data) {
    if (data == "true") {

      Swal.fire({
        type: 'success',
        title: 'Policy saved!',
        text: 'Page will now be reloaded!',
        showConfirmButton: false,
        timer: 1500
      }).then((result) => {
        location.reload();
      })

    } else {
      $('#notification_red').html(
          "Something seems to be wrong! <hr> <code>" + data + "</code>");
      $('#notification_red').show();
    }

  });

});

$("#btn_savedatamodel").click(function () {

  let datajson = JSON.parse(editor_datamodel.getValue());
  let isDatasource = true;
  if (datajson.hasOwnProperty("functionalities")) {
    isDatasource = false;
  }

  if (lastEditedId === "") {
    lastEditedId = datajson._id;
  }

  $.post("/writeDatamodel",
      {
        datamodel: editor_datamodel.getValue(),
        isDatasource: isDatasource,
        originalID: lastEditedId
      })
  .done(function (data) {

    if (data == "true") {

      Swal.fire({
        type: 'success',
        title: 'Model saved',
        text: 'Page will now be reloaded!',
        showConfirmButton: false,
        timer: 1500
      }).then((result) => {
        location.reload();
      })

    } else {
      $('#notification_red').html(
          "Something seems to be wrong! <hr> <code>" + data + "</code>");
      $('#notification_red').show();
    }

  });
});

$("#btn_addPolicy").click(function () {

  Swal.fire({
    title: 'Enter a Policy name',
    input: 'text',
    inputAttributes: {
      autocapitalize: 'off'
    },
    showCancelButton: true,
    confirmButtonText: 'Create Policy',
    showLoaderOnConfirm: true,
    preConfirm: (policyname) => {

      if (!/^[a-zA-Z0-9]+$/.test(policyname)) {
        Swal.showValidationMessage(
            `Policyname must be alphanumeric`
        )
      }

      if (policyname === "accesspolicy") {
        Swal.showValidationMessage(
            `Multiple Accesspolicies are not supported!`
        )
      }

      if (policydata.execution[policyname] != null) {
        Swal.showValidationMessage(
            `This Executionpolicy already exists!`
        )
      }

    },
    allowOutsideClick: () => !Swal.isLoading()
  }).then((result) => {

    if (result.value !== "" && result.value !== undefined) {

      let csv = "## " + result.value + " ##\n"
          + "\n"
          + "#p, DATASOURCE_ID, DATADESTINATION_FUNCTIONALITY,"
          + " DATADESTINATION_ID, SOMEATTRIBUTE\n";

      let conf = "[request_definition]\n"
          + "r = dataobj\n"
          + "\n"
          + "[policy_definition]\n"
          + "p = datasourceid, functionality, datadestinationid, attribute\n"
          + "\n"
          + "[policy_effect]\n"
          + "e = some(where (p.eft == allow))\n"
          + "\n"
          + "[matchers]\n"
          + "m = r.dataobj.id == p.datasourceid && r.dataobj.attr.attribute == p.attribute";

      policydata.execution[result.value] = {
        "csv": csv,
        "conf": conf
      };

      let insert = '<option value=' + result.value + '>' + result.value
          + '</option></optgroup>';

      let listitems = $('#policyselect').html().replace("</optgroup>", insert);
      $('#policyselect').html(listitems);

      $('#policyselect').val(result.value).change();

      $("#btn_savePolicy").click();

    }

  })

});

$("#btn_deletePolicy").click(function () {

  if ($("#policyselect").val() === "accesspolicy") {
    Swal.fire({
      title: 'Information',
      text: "Accesspolicy cannot be deleted",
      type: 'info',
      showCancelButton: false,
      confirmButtonColor: '#3085d6',
      confirmButtonText: 'OK'
    });

    return;
  }

  Swal.fire({
    title: 'Are you sure?',
    text: "Executionpolicy " + $("#policyselect").val() + " will be deleted!",
    type: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Yes, delete it'
  }).then((result) => {
    if (result.value) {

      $.ajax({
        url: '/deletePolicy',
        type: 'DELETE',
        data: {policyname: $("#policyselect").val()},
        success: function (data) {

          if (data == "true") {

            Swal.fire({
              type: 'success',
              title: 'Deleted',
              text: 'Page will now be reloaded!',
              showConfirmButton: false,
              timer: 1500
            }).then((result) => {
              location.reload();
            })

          } else {
            $('#notification_red').html(
                "Something seems to be wrong! <hr> <code>" + data + "</code>");
            $('#notification_red').show();
          }
        }
      });

    }

  });

});

$("#btn_addDatasource").click(function () {

  let jsontodisplay = '{\n'
      + '    "_id": "UNIQUE_ID_HERE",\n'
      + '    "location": {\n'
      + '        "host": "localhost",\n'
      + '        "port": "80",\n'
      + '        "user": "root",\n'
      + '        "pass": "pass",\n'
      + '        "entrypoint": "DATABASE_NAME",\n'
      + '        "attr": {},\n'
      + '        "path": "TABLE_OR_COLLECTION"\n'
      + '    },\n'
      + '    "criteria": "ID=1234",\n'
      + '    "attr": {\n'
      + '        "key1": "optional_data",\n'
      + '        "key2": "optional_data"\n'
      + '    }\n'
      + '}';

  jsontodisplay = JSON.parse(jsontodisplay);
  jsontodisplay = JSON.stringify(jsontodisplay, null, 4);

  // set to "" so its a new model to be saved
  lastEditedId = "";
  $('#exampleModalLong').modal();

  editor_datamodel.setValue(jsontodisplay);

  setTimeout(function () {
    editor_datamodel.refresh();
  }, 200);

});

$("#btn_addDatadestination").click(function () {

  let jsontodisplay = '{\n'
      + '    "_id": "UNIQUE_ID_HERE",\n'
      + '    "location": {\n'
      + '        "host": "localhost",\n'
      + '        "port": "80",\n'
      + '        "user": "root",\n'
      + '        "pass": "pass",\n'
      + '        "entrypoint": "DATABASE_NAME",\n'
      + '        "attr": {},\n'
      + '        "path": "TABLE_OR_COLLECTION"\n'
      + '    },\n'
      + '    "functionalities": [\n'
      + '        "SAVE_DATA_EXAMPLE",\n'
      + '        "DELETE_DATA_EXAMPLE"\n'
      + '    ],\n'
      + '    "attr": {\n'
      + '        "key1": "optional_data",\n'
      + '        "key2": "optional_data"\n'
      + '    }\n'
      + '}';
  jsontodisplay = JSON.parse(jsontodisplay);
  jsontodisplay = JSON.stringify(jsontodisplay, null, 4);

  // set to "" so its a new model to be saved
  lastEditedId = "";
  $('#exampleModalLong').modal();

  editor_datamodel.setValue(jsontodisplay);

  setTimeout(function () {
    editor_datamodel.refresh();
  }, 200);

});

// ## normal function ##

//Opens editor window for datamodel editing
function openEditWindow(modelid, type) {

  let jsontodisplay = '';
  if (type === "source") {
    jsontodisplay = datamodelsdata.datamodels.dataSources[modelid];
  } else {
    jsontodisplay = datamodelsdata.datamodels.dataDestinations[modelid];
  }
  lastEditedId = jsontodisplay._id;
  jsontodisplay = JSON.stringify(jsontodisplay, null, 4);

  $('#exampleModalLong').modal();

  editor_datamodel.setValue(jsontodisplay);

  setTimeout(function () {
    editor_datamodel.refresh();
  }, 200);

}

function deleteDatamodel(modelid, type) {

  Swal.fire({
    title: 'Are you sure?',
    text: "Datamodel " + modelid + " will be deleted!",
    type: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Yes, delete it'
  }).then((result) => {
    if (result.value) {

      let isDatasource = false;
      if (type === "source") {
        isDatasource = true;
      }

      $.ajax({
        url: '/deleteDatamodel',
        type: 'DELETE',
        data: {modelname: modelid, isDatasource: isDatasource},
        success: function (data) {

          if (data == "true") {

            Swal.fire({
              type: 'success',
              title: 'Deleted',
              text: 'Page will now be reloaded!',
              showConfirmButton: false,
              timer: 1500
            }).then((result) => {
              location.reload();
            })

          } else {
            $('#notification_red').html(
                "Something seems to be wrong! <hr> <code>" + data + "</code>");
            $('#notification_red').show();
          }
        }

      });

    }

  });

}
