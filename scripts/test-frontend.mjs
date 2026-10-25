import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import vm from "node:vm";
import ts from "typescript";

async function importSource(path) {
  const source = readFileSync(new URL(`../${path}`, import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
  });
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`);
}

const { bridgeFlowApi } = await importSource("lib/bridgeflow-api.ts");
const { workspaceMeta, readWorkspaceLocation, defaultRequirementTab } = await importSource("lib/workspace-navigation.ts");

test("all workspace destinations survive reload, and sidebar shortcuts open their task tab", () => {
  for (const view of Object.keys(workspaceMeta)) {
    const tab = defaultRequirementTab(view);
    assert.deepEqual(readWorkspaceLocation(`#view=${view}&tab=${tab}`), { view, tab });
  }
  assert.equal(readWorkspaceLocation("#view=questions").tab, "artifacts");
  assert.equal(readWorkspaceLocation("#view=test-cases").tab, "test-cases");
  assert.equal(readWorkspaceLocation("#view=changes").tab, "traceability");
  assert.deepEqual(readWorkspaceLocation("#view=toString&tab=invalid"), { view: "overview", tab: "analysis" });
  assert.equal(readWorkspaceLocation("#view=requirements&tab=history").tab, "history");
});

test("requirement summary and relation candidates include pages beyond the first 100", async (context) => {
  const pages = [];
  context.mock.method(globalThis, "fetch", async (url) => {
    const parsed = new URL(url, "http://test.local");
    assert.ok(parsed.pathname.endsWith("/projects/project-a/requirements"));
    assert.equal(parsed.searchParams.get("size"), "100");
    const page = Number(parsed.searchParams.get("page"));
    pages.push(page);
    const start = page * 100;
    const items = Array.from({ length: Math.min(100, 205 - start) }, (_, i) => ({ id: `req-${start + i}` }));
    return Response.json({ items, page, totalPages: 3 });
  });
  const requirements = await bridgeFlowApi.listAllRequirements("project-a");
  assert.deepEqual(pages, [0, 1, 2]);
  assert.equal(requirements.length, 205);
  assert.equal(requirements.at(-1).id, "req-204");
});

test("pagination reports a failed later page instead of returning misleading partial totals", async (context) => {
  context.mock.method(globalThis, "fetch", async (url) => new URL(url, "http://test.local").searchParams.get("page") === "0"
    ? Response.json({ items: [{ id: "req-1" }], totalPages: 2 })
    : Response.json({ message: "Project access expired" }, { status: 403 }));
  await assert.rejects(bridgeFlowApi.listAllRequirements("project-a"), /Project access expired/);
});

test("member reads use the selected project and propagate forbidden access", async (context) => {
  const requests = [];
  context.mock.method(globalThis, "fetch", async (url) => {
    requests.push(new URL(url, "http://test.local").pathname);
    return Response.json({ message: "Không có quyền" }, { status: 403 });
  });
  await assert.rejects(bridgeFlowApi.listProjectMembers("private-project"), /Không có quyền/);
  assert.ok(requests[0].endsWith("/projects/private-project/members"));
});

// Load the actual component with a minimal hook/element harness. The submit
// event deliberately loses currentTarget while the HTTP request is pending,
// reproducing React's event lifecycle without a browser or extra dependencies.
async function relationHarness(createRelation) {
  const state = [];
  let cursor = 0;
  let mounted = false;
  const effects = [];
  const timers = [];
  let loads = 0;
  const api = {
    getRequirementTraceability: async () => { loads++; return { revisions: [], relations: [] }; },
    getRequirementChangeImpact: async () => ({ reasons: [], affectedArtifacts: [], impactLevel: "LOW" }),
    listAllRequirements: async () => [{ id: "source", displayKey: "REQ-1" }, { id: "target", displayKey: "REQ-2" }],
    listProjects: async () => [{ id: "project", role: "ADMIN" }],
    createRequirementRelation: createRelation,
  };
  const react = {
    useState(initial) {
      const slot = cursor++;
      if (!(slot in state)) state[slot] = initial;
      return [state[slot], (value) => { state[slot] = typeof value === "function" ? value(state[slot]) : value; }];
    },
    useCallback: (callback) => callback,
    useEffect: (callback) => { if (!mounted) effects.push(callback); },
  };
  const jsx = (type, props) => ({ type, props });
  const exports = {};
  const source = readFileSync(new URL("../components/requirement-traceability.tsx", import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX },
  });
  const sandbox = {
    exports,
    Error,
    require(name) {
      if (name === "react") return react;
      if (name === "react/jsx-runtime") return { jsx, jsxs: jsx };
      if (name === "@/lib/bridgeflow-api") return { bridgeFlowApi: api };
      return new Proxy({}, { get: (_, prop) => String(prop) });
    },
    window: { setTimeout: (callback) => { timers.push(callback); }, clearTimeout() {} },
    FormData: class { constructor(form) { this.values = { ...form.values }; } get(name) { return this.values[name]; } },
  };
  vm.runInNewContext(outputText, sandbox);
  const render = () => {
    cursor = 0;
    const tree = exports.RequirementTraceabilityWorkspace({
      requirement: { id: "source", projectId: "project", revisions: [] }, revision: { id: "revision" },
    });
    mounted = true;
    return tree;
  };
  render();
  effects.forEach((effect) => effect());
  timers.forEach((timer) => timer());
  await new Promise(setImmediate);
  function find(node, predicate) {
    if (!node || typeof node !== "object") return null;
    if (predicate(node)) return node;
    const children = Array.isArray(node) ? node : [node.props?.children];
    for (const child of children) { const result = find(child, predicate); if (result) return result; }
    return null;
  }
  let resetCount = 0;
  const form = { values: { targetRequirementId: "target", relationType: "DEPENDS_ON" }, reset() { resetCount++; } };
  const event = { currentTarget: form, preventDefault() {} };
  const handler = find(render(), (node) => node.type === "form").props.onSubmit;
  return { handler, event, render, find, resetCount: () => resetCount, loadCount: () => loads };
}

test("relation creation resets the captured form after React clears currentTarget, then reloads", async () => {
  let finish;
  let submitted;
  const pending = new Promise((resolve) => { finish = resolve; });
  const harness = await relationHarness(async (...args) => { submitted = args; await pending; });
  const submission = harness.handler(harness.event);
  harness.event.currentTarget = null;
  finish();
  await submission;
  assert.deepEqual(submitted, ["source", "target", "DEPENDS_ON"]);
  assert.equal(harness.resetCount(), 1);
  assert.equal(harness.loadCount(), 2);
  assert.equal(harness.find(harness.render(), (node) => node.props?.children === "Cannot read properties of null (reading 'reset')"), null);
});

test("a rejected relation keeps the form selection and displays the server error", async () => {
  const harness = await relationHarness(async () => { throw new Error("Relation already exists"); });
  await harness.handler(harness.event);
  assert.equal(harness.resetCount(), 0);
  assert.equal(harness.loadCount(), 1);
  assert.ok(harness.find(harness.render(), (node) => node.props?.children?.includes?.("Relation already exists")));
});
