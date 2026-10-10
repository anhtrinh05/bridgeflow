import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = path.resolve(import.meta.dirname, "..");
const safeDirectory = `safe.directory=${root.replaceAll("\\", "/")}`;
const gitFiles = (...arguments_) => execFileSync("git", ["-c", safeDirectory, "ls-files", ...arguments_], {
  cwd: root,
  encoding: "utf8",
}).split("\0").filter(Boolean);
const tracked = gitFiles("-z");
const untrackedCandidateFiles = gitFiles("--others", "--exclude-standard", "-z");
const candidateFiles = [...new Set([...tracked, ...untrackedCandidateFiles])];

const forbiddenEnvironmentFiles = candidateFiles.filter((file) =>
  /(^|\/)\.env(?:\.|$)/.test(file)
  && !file.endsWith(".example")
);
const patterns = [
  { name: "private key", value: /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/ },
  { name: "OpenAI-style API key", value: /\bsk-[A-Za-z0-9_-]{20,}\b/ },
  { name: "GitHub token", value: /\bgh[pousr]_[A-Za-z0-9]{20,}\b/ },
  { name: "AWS access key", value: /\b(?:AKIA|ASIA)[A-Z0-9]{16}\b/ },
  { name: "hard-coded bearer token", value: /Authorization\s*:\s*Bearer\s+[A-Za-z0-9._~-]{20,}/i },
];
const findings = forbiddenEnvironmentFiles.map((file) => `${file}: runtime environment file is present in the candidate tree`);

for (const file of candidateFiles) {
  const absolute = path.join(root, file);
  const stat = fs.statSync(absolute);
  if (!stat.isFile() || stat.size > 1_000_000) continue;
  const bytes = fs.readFileSync(absolute);
  if (bytes.includes(0)) continue;
  const text = bytes.toString("utf8");
  for (const pattern of patterns) {
    if (pattern.value.test(text)) findings.push(`${file}: ${pattern.name}`);
  }
}

if (findings.length > 0) {
  console.error("Tracked secret-pattern scan failed:");
  findings.forEach((finding) => console.error(`- ${finding}`));
  process.exit(1);
}

console.log(`Candidate secret-pattern scan passed (${candidateFiles.length} tracked/untracked files checked).`);
