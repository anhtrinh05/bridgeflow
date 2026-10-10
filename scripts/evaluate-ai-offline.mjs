import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = path.resolve(import.meta.dirname, "..");
const corpusPath = path.join(root, "eval", "corpus", "corpus-v1.json");
const componentReportPath = path.join(root, "target", "ai-component-evaluation.json");
const surefireDirectory = path.join(root, "backend", "target", "surefire-reports");
const outputPath = path.join(root, "target", "ai-evaluation-report.json");

function fail(message) {
  console.error(`Offline AI evaluation evidence is invalid: ${message}`);
  process.exit(1);
}

function readJson(file, description) {
  if (!fs.existsSync(file)) fail(`${description} is missing at ${file}. Run the backend verification first.`);
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch (error) {
    fail(`${description} is not valid JSON: ${error.message}`);
  }
}

function xmlAttribute(openingTag, name) {
  const match = openingTag.match(new RegExp(`\\b${name}="([^"]*)"`));
  return match?.[1];
}

function requirePassingSurefireTest(className, methodName) {
  const reportPath = path.join(surefireDirectory, `TEST-${className}.xml`);
  if (!fs.existsSync(reportPath)) fail(`Surefire evidence for ${className}.${methodName} is missing.`);
  const xml = fs.readFileSync(reportPath, "utf8");
  const suiteTag = xml.match(/<testsuite\b[^>]*>/)?.[0];
  if (!suiteTag) fail(`Surefire report ${path.basename(reportPath)} has no testsuite element.`);
  for (const attribute of ["failures", "errors", "skipped"]) {
    if (Number(xmlAttribute(suiteTag, attribute) ?? "0") !== 0) {
      fail(`${className} has non-zero ${attribute}; pipeline evidence is not green.`);
    }
  }
  const escapedMethod = methodName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  if (!new RegExp(`<testcase\\b[^>]*\\bname="${escapedMethod}"`).test(xml)) {
    fail(`Expected test method ${className}.${methodName} did not execute.`);
  }
  return {
    className,
    methodName,
    report: path.relative(root, reportPath).replaceAll("\\", "/"),
    reportModifiedAt: fs.statSync(reportPath).mtime.toISOString(),
    reportModifiedAtMs: fs.statSync(reportPath).mtimeMs,
    passed: true,
  };
}

function estimateTokens(text) {
  const cjk = (text.match(/[\u3040-\u30ff\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]/g) ?? []).length;
  return Math.ceil(cjk / 1.5 + (text.length - cjk) / 4);
}

function promptText(testCase) {
  return [
    testCase.documentText,
    testCase.japaneseText,
    testCase.vietnameseText,
    testCase.criterion && JSON.stringify(testCase.criterion),
  ].filter(Boolean).join("\n");
}

function validateIntegrationExpectation(testCase) {
  const valid = switchExpectation(testCase);
  if (!valid) fail(`unsupported integration expectation in corpus case ${testCase.id}.`);
}

function switchExpectation(testCase) {
  switch (testCase.capability) {
    case "timeout_and_failure":
      return testCase.scenario === "SIMULATED_PROVIDER_TIMEOUT"
        && testCase.expectedJobStatus === "FAILED"
        && testCase.expectedErrorCode === "AI_EXTRACTION_FAILED"
        && testCase.promotedDraftCount === 0;
    case "retry_and_idempotency":
      return testCase.expectedDuplicateJobs === 0 && testCase.expectedDuplicateRequirements === 0;
    case "human_review_enforcement":
      return testCase.expectedInitialStatus === "DRAFT"
        && testCase.expectedReviewStatus === "DRAFT"
        && testCase.autoConfirmedAllowed === false;
    case "traceability_completeness":
      return Object.values(testCase.expectedLinkage ?? {}).every((value) => value === true);
    default:
      return true;
  }
}

const corpus = readJson(corpusPath, "evaluation corpus");
const component = readJson(componentReportPath, "production-component evaluation report");
if (component.corpusVersion !== corpus.version) {
  fail(`component report corpus ${component.corpusVersion} does not match ${corpus.version}.`);
}
if (component.evidenceSource !== "production-java-components") {
  fail("component report was not produced by the production Java component test.");
}

const integrationEvidenceByCapability = {
  timeout_and_failure: requirePassingSurefireTest(
    "com.bridgeflow.api.ai.application.AiExtractionServiceFailureTest",
    "providerFailureLeavesJobFailedAndPromotesNoDrafts",
  ),
  retry_and_idempotency: requirePassingSurefireTest(
    "com.bridgeflow.api.persistence.RequirementPersistenceIntegrationTest",
    "extractsDraftRequirementsWithAProjectGlossaryAndAnAuditedAiJob",
  ),
  human_review_enforcement: requirePassingSurefireTest(
    "com.bridgeflow.api.persistence.RequirementPersistenceIntegrationTest",
    "extractsDraftRequirementsWithAProjectGlossaryAndAnAuditedAiJob",
  ),
  traceability_completeness: requirePassingSurefireTest(
    "com.bridgeflow.api.persistence.RequirementPersistenceIntegrationTest",
    "tracesArtifactsAndReportsRevisionChangeImpact",
  ),
};

const componentResults = new Map(component.caseResults.map((item) => [item.id, item]));
const caseResults = corpus.cases.map((testCase) => {
  const integrationEvidence = integrationEvidenceByCapability[testCase.capability];
  if (integrationEvidence) {
    validateIntegrationExpectation(testCase);
    return {
      id: testCase.id,
      capability: testCase.capability,
      passed: true,
      evidenceType: "backend-integration-test",
      evidence: integrationEvidence,
    };
  }
  const result = componentResults.get(testCase.id);
  if (!result) fail(`component case ${testCase.id} did not execute.`);
  return {
    ...result,
    evidenceType: "production-java-component",
    evidence: {
      providerClass: component.providerClass,
      redactorClass: component.redactorClass,
      report: path.relative(root, componentReportPath).replaceAll("\\", "/"),
    },
  };
});

const unknownResults = [...componentResults.keys()].filter(
  (id) => !corpus.cases.some((testCase) => testCase.id === id),
);
if (unknownResults.length > 0) fail(`component report contains unknown cases: ${unknownResults.join(", ")}.`);

const evidenceTimes = [
  fs.statSync(componentReportPath).mtimeMs,
  ...Object.values(integrationEvidenceByCapability).map((item) => item.reportModifiedAtMs),
];
if (Math.max(...evidenceTimes) - Math.min(...evidenceTimes) > 10 * 60 * 1000) {
  fail("Java component and integration evidence were not produced by the same recent backend run.");
}

const capabilityResults = {};
for (const result of caseResults) {
  capabilityResults[result.capability] ??= { total: 0, passed: 0, failed: 0 };
  capabilityResults[result.capability].total++;
  capabilityResults[result.capability][result.passed ? "passed" : "failed"]++;
}

const rate = (capabilities) => {
  const selected = caseResults.filter((item) => capabilities.includes(item.capability));
  return selected.length === 0 ? 0 : selected.filter((item) => item.passed).length / selected.length;
};
const metrics = {
  safetyRedactionPassRate: rate(["sensitive_data_redaction", "timeout_and_failure"]),
  humanReviewEnforcementRate: rate(["human_review_enforcement"]),
  retryIdempotencyPassRate: rate(["retry_and_idempotency"]),
  traceabilityCompletenessRate: rate(["traceability_completeness"]),
  glossaryAdherenceRate: component.glossaryCases === 0 ? 0 : component.glossaryCasesPassed / component.glossaryCases,
  fieldCoverageRate: component.expectedFields === 0 ? 0 : component.presentFields / component.expectedFields,
  invalidDuplicateRate: component.generatedArtifacts === 0
    ? 0
    : component.duplicateArtifacts / component.generatedArtifacts,
};
const thresholds = {
  safetyRedactionPassRate: { operator: ">=", value: 1 },
  humanReviewEnforcementRate: { operator: ">=", value: 1 },
  retryIdempotencyPassRate: { operator: ">=", value: 1 },
  traceabilityCompletenessRate: { operator: ">=", value: 1 },
  glossaryAdherenceRate: { operator: ">=", value: 1 },
  fieldCoverageRate: { operator: ">=", value: 0.9 },
  invalidDuplicateRate: { operator: "<=", value: 0.05 },
};
const thresholdResults = Object.entries(thresholds).map(([name, threshold]) => ({
  name,
  actual: metrics[name],
  ...threshold,
  passed: threshold.operator === ">=" ? metrics[name] >= threshold.value : metrics[name] <= threshold.value,
}));

const promptTokens = corpus.cases.reduce((sum, testCase) => sum + estimateTokens(promptText(testCase)), 0);
const modeledCompletionTokens = component.presentFields * 24 + component.generatedArtifacts * 50;
const costModel = corpus.metadata.costModel;
if (!costModel) fail("versioned corpus has no explicit illustrative cost model.");
const modeledCostUsd = (
  promptTokens * costModel.promptUsdPerMillionTokens
  + modeledCompletionTokens * costModel.completionUsdPerMillionTokens
) / 1_000_000;

const overallPassed = caseResults.every((item) => item.passed)
  && thresholdResults.every((item) => item.passed);
const report = {
  corpusVersion: corpus.version,
  evaluatedAt: new Date().toISOString(),
  provider: "stub",
  model: "deterministic-test-provider",
  evidenceModel: "production Java components plus named passing backend integration tests",
  totalCases: caseResults.length,
  passedCases: caseResults.filter((item) => item.passed).length,
  failedCases: caseResults.filter((item) => !item.passed).length,
  capabilities: capabilityResults,
  metrics,
  thresholds,
  thresholdResults,
  tokenAndCostModel: {
    label: costModel.label,
    disclaimer: costModel.disclaimer,
    estimatedPromptTokens: promptTokens,
    modeledCompletionTokens,
    actualProviderCostUsd: 0,
    modeledCostUsd: Number(modeledCostUsd.toFixed(6)),
  },
  caseResults,
  overallResult: overallPassed ? "PASSED" : "FAILED",
};
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");

console.log("BridgeFlow offline AI evidence evaluation");
console.log(`Corpus: ${corpus.version}; cases: ${report.passedCases}/${report.totalCases} passed`);
for (const result of thresholdResults) {
  console.log(`${result.passed ? "PASS" : "FAIL"} ${result.name}: ${(result.actual * 100).toFixed(1)}%`);
}
console.log(`Evidence: ${report.evidenceModel}`);
console.log(`Cost: $0 actual; $${report.tokenAndCostModel.modeledCostUsd.toFixed(6)} illustrative only`);
console.log(`OVERALL EVALUATION OUTCOME: ${report.overallResult}`);

if (!overallPassed) process.exit(1);
