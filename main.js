$(function(){
  $("#copy_button").click(function(){
    $("#output_scala").select();
    document.execCommand("copy");
  });

  $("#format_input").click(function(){
    const input = $("#input_scala").val();
    const scalafmt = $("#scalafmt").val();
    const result = ScalametaAstMain.format(input, scalafmt);
    if (input != result) {
      $("#input_scala").val(result);
    }
  });

  $("#clear_local_storage").click(function(){
    localStorage.clear();
  });

  const run = function(){
    try {
      const scalafmt = $("#scalafmt").val();
      const input = $("#input_scala").val();
      const outputType = $("input[name=output_type]:checked").val();
      const package = $("#package").val();
      const ruleName = $("#rule_name").val();

      const r = ScalametaAstMain.convert(
        input,
        $("#format").prop("checked") === true,
        scalafmt,
        outputType === undefined ? "" : outputType,
        package === undefined ? "" : package,
        $("#wildcard_import").prop("checked") === true,
        ruleName === undefined ? "" : ruleName,
      );
      $("#output_scala").text(r.ast);
      $("#info").text(`ast: ${r.astBuildMs} ms\nfmt: ${r.formatMs} ms`)
              .addClass("alert-success")
              .removeClass("alert-danger");

      const saveLimit = 1024;

      try {
        localStorage.setItem("rule_name", ruleName);
      } catch(e) {
        console.trace(e);
      }
      try {
        localStorage.setItem("package", package);
      } catch(e) {
        console.trace(e);
      }
      if (input.length < saveLimit) {
        try {
          localStorage.setItem("source", input);
        } catch(e) {
          console.trace(e);
        }
      }
      if (scalafmt.length < saveLimit) {
        try {
          localStorage.setItem("scalafmt", scalafmt);
        } catch(e) {
          console.trace(e);
        }
      }
      try {
        localStorage.setItem("output_type", outputType);
      } catch(e) {
        console.trace(e);
      }
    } catch(e) {
      console.trace(e);
      $("#output_scala").text("");
      $("#info").text(e)
              .addClass("alert-danger")
              .removeClass("alert-success");
    }
  };

  $("#input_scala").keyup(function(event){
    run();
  });

  $("#package").keyup(function(event){
    run();
  });

  $("#rule_name").keyup(function(event){
    run();
  });

  $("input[name=output_type]").on("change", function() {
    run();
  });

  $("#format").change(function(){
    run();
    localStorage.setItem("format", ($("#format").prop("checked") === true).toString());
  });

  $("#wildcard_import").change(function(){
    run();
    localStorage.setItem("wildcard_import", ($("#wildcard_import").prop("checked") === true).toString());
  });

  $(document).ready(function(){
    const savedSource = localStorage.getItem("source");
    const savedScalafmt = localStorage.getItem("scalafmt");
    const savedPackage = localStorage.getItem("package");
    const savedRuleName = localStorage.getItem("rule_name");

    if (savedPackage != null) {
      $("#rule_name").val(savedRuleName);
    } else {
      $("#rule_name").val("fix");
    }

    if (savedPackage != null) {
      $("#package").val(savedPackage);
    } else {
      $("#package").val("fix");
    }

    if (savedScalafmt != null) {
      $("#scalafmt").val(savedScalafmt);
    } else {
      const defaultConfig = {
        maxColumn: 50,
        runner: {
          dialect: "Scala3"
        },
        align: {
          preset: "none"
        },
        continuationIndent: {
          defnSite: 2,
          extendSite: 2
        }
      };
      $("#scalafmt").val(JSON.stringify(defaultConfig, null, "  "));
    }

    if (savedSource != null) {
      $("#input_scala").val(savedSource);
    } else {
      $("#input_scala").val("def a = b");
    }

    if (localStorage.getItem("format") === "false") {
      $("#format").prop("checked", false);
    }

    if (localStorage.getItem("wildcard_import") === "true") {
      $("#wildcard_import").prop("checked", true);
    }

    switch(localStorage.getItem("output_type")) {
      case "semantic":
        $("input[name=output_type][value='semantic']").prop("checked", true);
        break;
      case "syntactic":
        $("input[name=output_type][value='syntactic']").prop("checked", true);
        break;
      default:
        $("input[name=output_type][value='raw']").prop("checked", true);
    }

    run();
  });
});
