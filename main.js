$(function(){
  $("#copy_button").click(function(){
    $("#output_scala").select();
    document.execCommand("copy");
  });

  $("#format_input").click(function(){
    const input = $("#input_scala").val();
    const result = ScalametaAstMain.format(input);
    if (input != result) {
      $("#input_scala").val(result);
    }
  });

  const run = function(){
    try {
      const input = $("#input_scala").val()

      try {
        localStorage.setItem("source", input);
      } catch(e) {
        console.trace(e);
      }

      const r = ScalametaAstMain.convert(
        input,
        $("#format").prop("checked") === true
      );
      $("#output_scala").text(r.ast);
      $("#info").text(`ast: ${r.astBuildMs} ms\nfmt: ${r.formatMs} ms`)
              .addClass("alert-success")
              .removeClass("alert-danger");
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
    if (savedSource != null) {
      $("#input_scala").val(savedSource);
    } else {
      $("#input_scala").val("def a = b");
    }

    if (localStorage.getItem("format") === "false") {
      $("#format").prop("checked", false);
    }

    run();
  });
});
