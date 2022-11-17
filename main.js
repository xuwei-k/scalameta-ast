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

  const run = function(){
    try {
      const scalafmt = $("#scalafmt").val();
      const input = $("#input_scala").val();
      const outputType = $("input[name=output_type]:checked").val();

      const r = ScalametaAstMain.convert(
        input,
        $("#format").prop("checked") === true,
        scalafmt,
        outputType === undefined ? "" : outputType
      );
      $("#output_scala").text(r.ast);
      $("#info").text(`ast: ${r.astBuildMs} ms\nfmt: ${r.formatMs} ms`)
              .addClass("alert-success")
              .removeClass("alert-danger");

      const saveLimit = 1024;

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

  $("#format").change(function(){
    run();
    localStorage.setItem("format", ($("#format").prop("checked") === true).toString());
  });

  $(document).ready(function(){
    const savedSource = localStorage.getItem("source");
    const savedScalafmt = localStorage.getItem("scalafmt");

    if (savedScalafmt != null) {
      $("#scalafmt").val(savedScalafmt);
    } else {
      const defaultConfig = {
        maxColumn: 50,
        runner: {
          dialect: "Scala3"
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
