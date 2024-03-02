"use strict";

import { ScalametaAstMainScalafixCompat } from "./scalafix-compat/main.js";
import { ScalametaAstMainLatest } from "./latest/main.js";

[ScalametaAstMainLatest, ScalametaAstMainScalafixCompat].forEach((main) => {
  try {
    // force initialize for avoid error
    main.convert("", true, "", "", "", false, "", "", "", false, false);
  } catch (e) {
    console.log(e);
  }
});

import {
  html,
  render,
  useState,
  useRef,
  useEffect,
} from "https://unpkg.com/htm@3.1.1/preact/standalone.module.js";

import hljs from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/highlight.min.js";
import scala from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/languages/scala.min.js";
hljs.registerLanguage("scala", scala);

const App = () => {
  const [summary, setSummary] = useState("close header");
  const [scalafmtConfig, setScalafmtConfig] = useState("");
  const [inputScala, setInputScala] = useState("def a = b");
  const [scalameta, setScalameta] = useState("latest");
  const [outputType, setOutputType] = useState("syntactic");
  const [packageName, setPackageName] = useState("fix");
  const [patch, setPatch] = useState("replace");
  const [format, setFormat] = useState(true);
  const [wildcardImport, setWildcardImport] = useState(false);
  const [ruleName, setRuleName] = useState("Example");
  const [dialect, setDialect] = useState("Auto");
  const [removeNewFields, setRemoveNewFields] = useState(false);
  const [initialExtractor, setInitialExtractor] = useState(false);
  const [outputScala, setOutputScala] = useState("");

  const changeDetails = (e) => {
    switch (e.newState) {
      case "open":
        setSummary("close header");
        break;
      case "closed":
        setSummary("open header");
        break;
    }
  };

  const main =
    scalameta == "latest"
      ? ScalametaAstMainLatest
      : ScalametaAstMainScalafixCompat;

  const formatInput = () => {
    const result = main.format(inputScala, scalafmtConfig);
    setInputScala(result);
  };

  const run = () => {
    [
      scalafmtConfig,
      inputScala,
      scalameta,
      outputType,
      packageName,
      patch,
      format,
      wildcardImport,
      ruleName,
    ].forEach((i) => {
      console.log(i);
    });

    const r = main.convert(
      inputScala,
      format,
      scalafmtConfig,
      outputType,
      packageName,
      wildcardImport,
      ruleName,
      dialect,
      patch,
      removeNewFields,
      initialExtractor,
    );
    setOutputScala(r.ast);
    document.getElementById("output_scala").removeAttribute("data-highlighted");
    hljs.highlightAll();
  };

  return html` <div class="container mw-100">
    <details open ontoggle="${(e) => changeDetails(e)}">
      <summary>${summary}</summary>
      <div class="row">
        <div class="col-5">
          <pre id="info" class="alert" style="height: 100px"></pre>
        </div>
        <div class="col-3">
          <div class="row">
            <div>
              <input
                type="checkbox"
                name="format"
                checked=${format}
                onChange=${(e) => setFormat(e.target.checked)}
              />
              <label for="format">format output by scalafmt</label>
            </div>
            <div>
              <input
                type="checkbox"
                name="wildcard_import"
                checked=${wildcardImport}
                onChange=${(e) => setWildcardImport(e.target.checked)}
              />
              <label for="wildcard_import">wildcard import</label>
            </div>
            <div>
              <input
                type="checkbox"
                id="remove_new_fields"
                name="remove_new_fields"
              />
              <label for="remove_new_fields"
                >remove <code>@newField</code> for
                <code>unapply</code> compatibility</label
              >
            </div>
            <div>
              <input
                type="checkbox"
                id="initial_extractor"
                name="initial_extractor"
              />
              <label for="initial_extractor"
                ><code>Initial</code> extractor</label
              >
            </div>
          </div>
          <div class="row">
            <p></p>
          </div>
          <div class="row">
            <div class="col">
              <button class="btn btn-primary" onclick=${() => formatInput()}>
                format input scala code
              </button>
            </div>
            <div class="col">
              <button
                class="btn btn-primary"
                onclick=${() => localStorage.clear()}
              >
                clear local storage
              </button>
            </div>
          </div>
        </div>
        <div class="col-2">
          <div class="row">
            <fieldset>
              <legend>output type</legend>
              <div>
                <input
                  type="radio"
                  name="output_type"
                  value="raw"
                  checked=${outputType === "raw"}
                  onChange=${() => setOutputType("raw")}
                />
                <label for="raw">Raw Scalameta</label>
              </div>
              <div>
                <input
                  type="radio"
                  name="output_type"
                  value="syntactic"
                  checked=${outputType === "syntactic"}
                  onChange=${() => setOutputType("syntactic")}
                />
                <label for="syntactic">Scalafix SyntacticRule</label>
              </div>
              <div>
                <input
                  type="radio"
                  name="output_type"
                  value="semantic"
                  checked=${outputType === "semantic"}
                  onChange=${() => setOutputType("semantic")}
                />
                <label for="semantic">Scalafix SemanticRule</label>
              </div>
              <div>
                <input
                  type="radio"
                  name="output_type"
                  value="tokens"
                  checked=${outputType === "tokens"}
                  onChange=${() => setOutputType("tokens")}
                />
                <label for="tokens">Tokens</label>
              </div>
            </fieldset>
          </div>
          <div class="row">
            <div>
              <label for="dialect">dialect</label>
              <select name="dialect" id="dialect">
                <option value="Auto">Auto</option>
                <option value="Scala3">Scala3</option>
                <option value="Scala213Source3">Scala213Source3</option>
                <option value="Scala213">Scala213</option>
                <option value="Scala212Source3">Scala212Source3</option>
                <option value="Scala212">Scala212</option>
                <option value="Scala211">Scala211</option>
                <option value="Scala210">Scala210</option>
              </select>
            </div>
          </div>
        </div>
        <div class="col-2">
          <div class="row">
            <div>
              <label for="scalameta">scalameta version</label>
              <select
                name="scalameta"
                value=${scalameta}
                onChange=${(e) => setScalameta(e.target.value)}
              >
                <option value="scalafix">scalafix 0.10.x compatible</option>
                <option value="latest">scalafix 0.11.x compatible</option>
              </select>
            </div>
          </div>
          <div class="row">
            <div>
              <label for="package">package</label>
              <input
                maxlength="256"
                id="package"
                value=${packageName}
                oninput=${(x) => setPackageName(x.target.value)}
              />
            </div>
          </div>
          <div class="row">
            <div>
              <label for="rule_name">rule name</label>
              <input
                type="text"
                id="rule_name"
                maxlength="128"
                value=${ruleName}
                oninput=${(x) => setRuleName(x.target.value)}
              />
            </div>
          </div>
          <div class="row">
            <div>
              <label for="patch"
                ><a
                  target="_blank"
                  href="https://scalacenter.github.io/scalafix/docs/developers/patch.html"
                  >patch</a
                ></label
              >
              <select
                name="patch"
                value=${patch}
                onChange=${(e) => setPatch(e.target.value)}
              >
                <option value="warn">lint warn</option>
                <option value="error">lint error</option>
                <option value="info">lint info</option>
                <option value="replace">replace</option>
                <option value="left">add left</option>
                <option value="right">add right</option>
                <option value="empty">empty</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </details>
    <div class="row">
      <div class="col">
        <textarea
          style="width: 100%; height: 800px"
          onkeyup=${(e) => {
            setInputScala(e.target.value);
            run();
          }}
          value=${inputScala}
        ></textarea>
      </div>
      <div class="col">
        <pre>
        <code
          class="language-scala"
          id="output_scala"
          style="width: 100%; height: 800px; background-color:rgb(233, 233, 233);"
        >${outputScala}</code>
        </pre>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <p>
          <a
            href="https://scalameta.org/scalafmt/docs/configuration.html"
            target="_blank"
            >scalafmt config</a
          >
        </p>
        <textarea style="width: 100%; height: 200px">
${scalafmtConfig}</textarea
        >
      </div>
    </div>
    <div class="row" id="footer"></div>
  </div>`;
};

render(html`<${App} />`, document.getElementById("root"));
