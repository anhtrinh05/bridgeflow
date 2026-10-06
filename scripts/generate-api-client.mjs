import { mkdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

import openapiTS, { astToString } from "openapi-typescript";

const repositoryRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const outputPath = resolve(repositoryRoot, "lib/generated/bridgeflow-api.ts");
const contractUrl = process.env.OPENAPI_URL ?? "http://127.0.0.1:8080/v3/api-docs";

const response = await fetch(contractUrl);
if (!response.ok) {
  throw new Error(`Unable to load OpenAPI contract (${response.status}) from ${contractUrl}`);
}

const contract = await response.json();
const ast = await openapiTS(contract);
const output = astToString(ast);

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, output, "utf8");
console.log(`Generated ${outputPath} from ${contractUrl}`);
