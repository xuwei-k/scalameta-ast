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

const getFromStorageOr = (key, defaultValue, fun) => {
  const saved = localStorage.getItem(key);
  if (saved === null) {
    console.log("not found saved value. use default " + defaultValue);
    return defaultValue;
  } else {
    console.log(`found saved value "${key}" = "${saved}"`);
    if (fun == null) {
      return saved;
    } else {
      console.log("convert " + key + " " + fun(saved));
      return fun(saved);
    }
  }
};

const getBoolFromStorageOr = (key, defaultValue) => {
  return getFromStorageOr(key, defaultValue, (a) => a === "true");
};

const defaultScalafmtConfig = `
        maxColumn = 50
        runner.dialect = "Scala3"
        align.preset = "none"
        continuationIndent.defnSite = 2
        continuationIndent.extendSite = 2
      `
  .split("\n")
  .map((c) => c.trim())
  .filter((c) => c.length > 0)
  .join("\n");

const initialSource = getFromStorageOr("source", "def a = b");
const initialScalafmt = getFromStorageOr("scalafmt", defaultScalafmtConfig);
const initialPackage = getFromStorageOr("package", "fix");
const initialRuleName = getFromStorageOr("rule_name", "Example");
const initialDialect = getFromStorageOr("dialect", "Auto");
const initialScalameta = getFromStorageOr("scalameta", "latest");
const initialPatch = getFromStorageOr("patch", "replace");
const initialOutputType = getFromStorageOr("output_type", "syntactic");

const initialFormat = getBoolFromStorageOr("format", true);
const initialWildcardImport = getBoolFromStorageOr("wildcard_import", false);
const initialRemoveNewFields = getBoolFromStorageOr("remove_new_fields", false);
const initialInitialExtractor = getBoolFromStorageOr(
  "initial_extractor",
  false,
);

const App = () => {
  const [summary, setSummary] = useState("close header");
  const [inputScala, setInputScala] = useState(initialSource);
  const [scalafmtConfig, setScalafmtConfig] = useState(initialScalafmt);
  const [packageName, setPackageName] = useState(initialPackage);
  const [ruleName, setRuleName] = useState(initialRuleName);
  const [dialect, setDialect] = useState(initialDialect);
  const [scalameta, setScalameta] = useState(initialScalameta);
  const [patch, setPatch] = useState(initialPatch);
  const [outputType, setOutputType] = useState(initialOutputType);

  const [format, setFormat] = useState(initialFormat);
  const [wildcardImport, setWildcardImport] = useState(initialWildcardImport);
  const [removeNewFields, setRemoveNewFields] = useState(
    initialRemoveNewFields,
  );
  const [initialExtractor, setInitialExtractor] = useState(
    initialInitialExtractor,
  );

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

  console.log([
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
  ]);

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

  let result = "";
  let info = "";
  let infoClass = "";

  if (r.ast == null) {
    info = r.errorString;
    infoClass = "alert alert-danger";
  } else {
    result = hljs.highlight(r.ast, {
      language: "scala",
    }).value;
    info = `ast: ${r.astBuildMs} ms\nfmt: ${r.formatMs} ms`;
    infoClass = "alert alert-success";
    console.log(r.ast);

    [
      ["source", inputScala],
      ["scalafmt", scalafmtConfig],
      ["package", packageName],
      ["rule_name", ruleName],
      ["dialect", dialect],
      ["scalameta", scalameta],
      ["patch", patch],
      ["output_type", outputType],

      ["format", format],
      ["wildcard_import", wildcardImport],
      ["remove_new_fields", removeNewFields],
      ["initial_extractor", initialExtractor],
    ].forEach((xs) => {
      localStorage.setItem(xs[0], xs[1]);
    });
  }

  const disableScalafixRuleTemplateInput = outputType === "raw" || outputType === "tokens";

  return html` <div class="container mw-100">
    <details open ontoggle="${(e) => changeDetails(e)}">
      <summary>${summary}</summary>
      <div class="row">
        <div class="col-5">
          <pre id="info" class=${infoClass} style="height: 100px">${info}</pre>
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
                disabled=${disableScalafixRuleTemplateInput}
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
              <select
                name="dialect"
                value=${dialect}
                onChange=${(e) => setDialect(e.target.value)}
              >
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
                disabled=${disableScalafixRuleTemplateInput}
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
                disabled=${disableScalafixRuleTemplateInput}
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
                disabled=${disableScalafixRuleTemplateInput}
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
          onkeyup=${(e) => setInputScala(e.target.value)}
          value=${inputScala}
        ></textarea>
      </div>
      <div class="col">
        <pre>
        <code
          class="language-scala hljs"
          dangerouslySetInnerHTML=${{ __html: result }}
          style="width: 100%; height: 800px; background-color:rgb(233, 233, 233);"
        ></code>
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
