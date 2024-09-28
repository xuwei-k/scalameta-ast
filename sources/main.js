"use strict";

import { format as formatFunction , convert } from "./latest/main.js";

import {
  html,
  render,
  useState,
  useEffect,
  useRef,
} from "https://unpkg.com/htm@3.1.1/preact/standalone.module.js";

import hljs from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/highlight.min.js";
import scala from "https://unpkg.com/@highlightjs/cdn-assets@11.9.0/es/languages/scala.min.js";
hljs.registerLanguage("scala", scala);

function fromBase64(base64) {
  const binString = atob(base64);
  const bytes = Uint8Array.from(binString, (m) => m.codePointAt(0));
  return new TextDecoder().decode(bytes);
}

function toBase64(text) {
  const bytes = new TextEncoder().encode(text);
  const binString = Array.from(bytes, (byte) =>
    String.fromCodePoint(byte),
  ).join("");
  return btoa(binString);
}

const getFromStorageOr = (key, defaultValue, fun) => {
  const p = new URLSearchParams(location.search);
  const fromQuery = p.get(key);
  if (fromQuery == null) {
    const saved = localStorage.getItem(key);
    if (saved === null) {
      return defaultValue;
    } else {
      if (fun == null) {
        return saved;
      } else {
        return fun(saved);
      }
    }
  } else {
    try {
      if (fun == null) {
        return fromBase64(fromQuery);
      } else {
        return fun(fromBase64(fromQuery));
      }
    } catch (e) {
      console.log(e);
      return defaultValue;
    }
  }
};

const getBoolFromStorageOr = (key, defaultValue) => {
  return getFromStorageOr(key, defaultValue, (a) => a === "true");
};

const screenWidth = Math.max(
  document.body.scrollWidth,
  document.documentElement.scrollWidth,
  document.body.offsetWidth,
  document.documentElement.offsetWidth,
  document.documentElement.clientWidth,
);

let defaultMaxColumn = 50;
if (screenWidth >= 1500) {
  defaultMaxColumn = 80;
} else if (screenWidth >= 1400) {
  defaultMaxColumn = 70;
} else if (screenWidth >= 1300) {
  defaultMaxColumn = 60;
}

const defaultScalafmtConfig = `
        maxColumn = ${defaultMaxColumn}
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
const initialRuleName = getFromStorageOr("rule_name", "");
const initialDialect = getFromStorageOr("dialect", "Auto");
const initialScalameta = getFromStorageOr("scalameta", "latest");
const initialPatch = getFromStorageOr("patch", "warn");
const initialOutputType = getFromStorageOr("output_type", "raw");

const initialEnableRichEditor = getBoolFromStorageOr(
  "enable_rich_editor",
  true,
);
const initialFormat = getBoolFromStorageOr("format", true);
const initialWildcardImport = getBoolFromStorageOr("wildcard_import", false);
const initialExplanation = getBoolFromStorageOr("explanation", true);
const initialPathFilter = getBoolFromStorageOr("path_filter", false);
const initialRemoveNewFields = getBoolFromStorageOr("remove_new_fields", false);
const initialInitialExtractor = getBoolFromStorageOr(
  "initial_extractor",
  false,
);

let initialized = false;

const inTest = window.navigator.userAgent === "playwright test";

const App = () => {
  const [scalametaV1, setScalametaV1] = useState("");
  const [scalametaV2, setScalametaV2] = useState("");
  const [githubUrl, setGithubUrl] = useState("");

  const [summary, setSummary] = useState("close header");
  const [inputScala, setInputScala] = useState(initialSource);
  const [scalafmtConfig, setScalafmtConfig] = useState(initialScalafmt);
  const [packageName, setPackageName] = useState(initialPackage);
  const [ruleName, setRuleName] = useState(initialRuleName);
  const [dialect, setDialect] = useState(initialDialect);
  const [scalameta, setScalameta] = useState(initialScalameta);
  const [patch, setPatch] = useState(initialPatch);
  const [outputType, setOutputType] = useState(initialOutputType);

  const [enableRichEditor, setEnableRichEditor] = useState(
    initialEnableRichEditor,
  );
  const [format, setFormat] = useState(initialFormat);
  const [wildcardImport, setWildcardImport] = useState(initialWildcardImport);
  const [explanation, setExplanation] = useState(initialExplanation);
  const [pathFilter, setPathFilter] = useState(initialPathFilter);
  const [removeNewFields, setRemoveNewFields] = useState(
    initialRemoveNewFields,
  );
  const [initialExtractor, setInitialExtractor] = useState(
    initialInitialExtractor,
  );
  const [headerStyle, setHeaderStyle] = useState("");

  let inputScalaDataStyle;
  let inputScalaStyle;

  if (enableRichEditor) {
    if (!inTest) {
      inputScalaDataStyle = "display:none;";
    }
    inputScalaStyle = "width: 100%; height: 800px;";
  } else {
    inputScalaDataStyle = "width: 100%; height: 800px;";
    inputScalaStyle = "display:none;";
  }

  const changeDetails = (e) => {
    switch (e.newState) {
      case "open":
        setHeaderStyle("");
        setSummary("close header");
        break;
      case "closed":
        setHeaderStyle("display:none;");
        setSummary("open header");
        break;
    }
  };

  const formatInput = () => {
    const res = formatFunction(inputScala, scalafmtConfig);
    if (res.error === null) {
      setInputScala(res.result);
      cm.current.setValue(res.result);
    }
  };

  let r = convert(
    inputScala,
    outputType,
    packageName,
    wildcardImport,
    ruleName,
    dialect,
    patch,
    removeNewFields,
    initialExtractor,
    explanation,
    pathFilter,
  );

  if (r.ast == null || format === false) {
    r.formatMs = 0;
  } else {
    const res = formatFunction(r.ast, scalafmtConfig);
    if (res.error === null) {
      r = {
        ast: res.result,
        astBuildMs: r.astBuildMs,
        formatMs: res.time,
      };
    } else {
      r = {
        ast: null,
        errorString: res.error,
      };
    }
  }

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
    const scalafixVersion = scalameta == "latest" ? "0.13.0" : "0.10.4";
    const scalafixUrl = (s) =>
      `https://github.com/scalacenter/scalafix/blob/v${scalafixVersion}/scalafix-core/src/main/scala/scalafix/${s}.scala`;

    if (["syntactic", "semantic"].includes(outputType)) {
      [
        ["Patch", "patch/Patch"],
        ["SyntacticDocument", "v1/SyntacticDocument"],
        ["SemanticDocument", "v1/SyntacticDocument"],
        ["SyntacticRule", "v1/Rule"],
        ["SemanticRule", "v1/Rule"],
        ["Diagnostic", "lint/Diagnostic"],
        ["LintSeverity", "lint/LintSeverity"],
      ].forEach(([x1, x2]) => {
        result = result.replaceAll(
          `<span class="hljs-type">${x1}</span>`,
          `<a target="_blank" href='${scalafixUrl(x2)}'><span class="hljs-type">${x1}</span></a>`,
        );
      });
    }

    info = `ast: ${r.astBuildMs} ms\nfmt: ${r.formatMs} ms`;
    infoClass = "alert alert-success";

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
      ["explanation", explanation],
      ["path_filter", pathFilter],
      ["enable_rich_editor", enableRichEditor],
    ].forEach(([key, val]) => {
      if (val.toString().length <= 1024) {
        localStorage.setItem(key, val);
      }
    });
  }

  if (initialized === false) {
    fetch("./latest/build_info.json")
      .then((res) => res.json())
      .then((json) => {
        initialized = true;
        setScalametaV2(json.scalametaVersion);
        setGithubUrl(
          `https://github.com/xuwei-k/scalameta-ast/tree/${json.gitHash}`,
        );
      });
  }

  const disableScalafixRuleTemplateInput = [
    "raw",
    "tokens",
    "comment",
  ].includes(outputType);

  const disableCompat =
    scalameta != "scalafix" || ["tokens", "comment"].includes(outputType);

  const cm = useRef(null);

  useEffect(() => {
    if (cm.current === null) {
      cm.current = CodeMirror(document.getElementById("input_scala"), {
        lineNumbers: true,
        matchBrackets: true,
        value: inputScala,
        mode: "text/x-scala",
      });
      cm.current.setSize("100%", "100%");
    }
    return () => {};
  }, [inputScala, formatInput]);

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
                id="format"
                checked=${format}
                onChange=${(e) => setFormat(e.target.checked)}
              />
              <label for="format">format output by scalafmt</label>
            </div>
            <div>
              <input
                type="checkbox"
                name="wildcard_import"
                id="wildcard_import"
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
                disabled=${disableCompat}
                checked=${removeNewFields}
                onChange=${(e) => setRemoveNewFields(e.target.checked)}
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
                disabled=${disableCompat}
                checked=${initialExtractor}
                onChange=${(e) => setInitialExtractor(e.target.checked)}
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
              <button
                class="btn btn-primary"
                id="clear_local_storage"
                onclick=${() => localStorage.clear()}
              >
                clear local storage
              </button>
            </div>
            <div class="col">
              <button
                class="btn btn-primary"
                id="share"
                onclick=${() =>
                  navigator.clipboard.writeText(
                    window.location.origin +
                      window.location.pathname +
                      "?" +
                      [["source", inputScala]]
                        .map(([k, v]) => k + "=" + toBase64(v))
                        .join("&"),
                  )}
              >
                share
              </button>
            </div>
          </div>
        </div>
        <div class="col-2">
          <div class="row">
            <fieldset>
              <legend>output type</legend>
              <div>
                <label>
                  <input
                    type="radio"
                    name="output_type"
                    value="raw"
                    checked=${outputType === "raw"}
                    onChange=${() => setOutputType("raw")}
                  />
                  <span>Raw Scalameta</span>
                </label>
              </div>
              <div>
                <label>
                  <input
                    type="radio"
                    name="output_type"
                    value="syntactic"
                    checked=${outputType === "syntactic"}
                    onChange=${() => setOutputType("syntactic")}
                  />
                  <span>Scalafix SyntacticRule</span>
                </label>
              </div>
              <div>
                <label>
                  <input
                    type="radio"
                    name="output_type"
                    value="semantic"
                    checked=${outputType === "semantic"}
                    onChange=${() => setOutputType("semantic")}
                  />
                  <span>Scalafix SemanticRule</span>
                </label>
              </div>
              <div>
                <label>
                  <input
                    type="radio"
                    name="output_type"
                    value="tokens"
                    checked=${outputType === "tokens"}
                    onChange=${() => setOutputType("tokens")}
                  />
                  <span>Tokens</span>
                </label>
              </div>
              <div>
                <label>
                  <input
                    type="radio"
                    name="output_type"
                    value="comment"
                    checked=${outputType === "comment"}
                    onChange=${() => setOutputType("comment")}
                  />
                  <span>Comment</span>
                </label>
              </div>
            </fieldset>
          </div>
          <div class="row">
            <div>
              <label for="dialect">dialect</label>
              <select
                name="dialect"
                id="dialect"
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
                id="scalameta"
                value=${scalameta}
                onChange=${(e) => setScalameta(e.target.value)}
              >
                <option value="scalafix">
                  scalafix 0.10.x compatible ${scalametaV1}
                </option>
                <option value="latest">
                  scalafix 0.13.x compatible ${scalametaV2}
                </option>
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
          <div>
            <input
              type="checkbox"
              name="explanation"
              id="explanation"
              disabled=${disableScalafixRuleTemplateInput}
              checked=${explanation}
              onChange=${(e) => setExplanation(e.target.checked)}
            />
            <label for="explanation">explanation</label>
          </div>
          <div>
            <input
              type="checkbox"
              name="path_filter"
              id="path_filter"
              disabled=${disableScalafixRuleTemplateInput}
              checked=${pathFilter}
              onChange=${(e) => setPathFilter(e.target.checked)}
            />
            <label for="path_filter">path filter</label>
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
                id="patch"
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
                <option value="remove">remove tokens</option>
                <option value="around">add around</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </details>
    <div class="row">
      <div class="col">
        <div style=${headerStyle}>
          <button
            class="btn btn-secondary"
            style="border-bottom-left-radius: 0; border-bottom-right-radius: 0;"
            onclick=${() => formatInput()}
            id="format_input"
          >
            format input scala code
          </button>
          <input
            type="checkbox"
            name="rich editor"
            id="enable_rich_editor"
            checked=${enableRichEditor}
            onChange=${(e) => setEnableRichEditor(e.target.checked)}
          />
          <label for="enable_rich_editor">enable rich editor</label>
        </div>
        <div
          id="input_scala"
          style=${inputScalaStyle}
          onkeyup=${(e) => setInputScala(cm.current.getValue())}
          onChange=${(e) => setInputScala(cm.current.getValue())}
        ></div>
        <textarea
          style=${inputScalaDataStyle}
          id="input_scala_data"
          onkeyup=${(e) => setInputScala(e.target.value)}
          onChange=${(e) => setInputScala(e.target.value)}
          value=${inputScala}
        ></textarea>
      </div>
      <div class="col">
        <div style=${headerStyle}>
          <button
            class="btn btn-secondary"
            style="border-bottom-left-radius: 0; border-bottom-right-radius: 0;"
            id="copy"
            onclick=${() => {
              navigator.clipboard.writeText(r.ast);
              if (window.getSelection) {
                const selection = window.getSelection();
                const range = document.createRange();
                range.selectNodeContents(
                  document.getElementById("output_scala"),
                );
                selection.removeAllRanges();
                selection.addRange(range);
              }
            }}
          >
            copy
          </button>
        </div>
        <pre>
        <code
          id="output_scala"
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
        <textarea
          style="width: 100%; height: 200px"
          id="scalafmt"
          onkeyup=${(e) => setScalafmtConfig(e.target.value)}
          onChange=${(e) => setScalafmtConfig(e.target.value)}
          value=${scalafmtConfig}
        ></textarea>
      </div>
    </div>
    <div class="row" id="footer">
      <a target="_blank" href=${githubUrl}>${githubUrl}</a>
    </div>
  </div>`;
};

render(html`<${App} />`, document.getElementById("root"));
