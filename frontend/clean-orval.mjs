/**
 * clean-orval.mjs — Post-Processing Script for Orval Code Generation
 *
 * This script runs automatically after `npm run generate:api` (Orval).
 * It applies two critical transformations to the generated TypeScript output:
 *
 *   Phase 1 — refactorOrvalTypes()
 *     Cleans up Orval's "flattened" generic types. Orval generates a dedicated
 *     TypeScript type for every unique response shape (e.g., GlobalResponseMemberDto,
 *     GlobalResponseListPaymentDto). This phase collapses them into a single
 *     generic `GlobalResponse<T>` wrapper, deletes the redundant per-shape files,
 *     and normalises SortOrder enums the same way.
 *
 *   Phase 2 — generateSingletons()
 *     Orval factory functions like `getMembers()` return a NEW object on every
 *     call, which causes React infinite loops when used as hook dependencies.
 *     This phase appends a pre-called singleton (e.g., `membersApi`) to each
 *     endpoint file so consumers can import a stable reference directly.
 *
 * Usage:
 *   node clean-orval.mjs           (called automatically by npm scripts)
 *
 * Prerequisites:
 *   - Run after Orval has finished generating code into ./src/api/generated
 *   - Expects the Orval `mode: 'tags-split'` folder structure
 */

import fs from 'fs';
import path from 'path';

/** Root directory where Orval places all generated API code. */
const API_DIR = path.resolve('./src/api/generated');

// ─────────────────────────────────────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Recursively collects all `.ts` files under a given directory.
 *
 * @param {string}   dirPath       - Absolute path to the directory to scan.
 * @param {string[]} arrayOfFiles  - Accumulator (used by recursion).
 * @returns {string[]} Array of absolute paths to every `.ts` file found.
 */
function getAllFiles(dirPath, arrayOfFiles = []) {
  const files = fs.readdirSync(dirPath);

  files.forEach(function (file) {
    if (fs.statSync(dirPath + '/' + file).isDirectory()) {
      arrayOfFiles = getAllFiles(dirPath + '/' + file, arrayOfFiles);
    } else {
      arrayOfFiles.push(path.join(dirPath, '/', file));
    }
  });

  return arrayOfFiles.filter((f) => f.endsWith('.ts'));
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase 1: Refactor Orval's flattened DTOs into generic wrappers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Phase 1: Clean up Orval-generated types to use generic wrappers.
 *
 * **Problem:**
 * Orval produces a separate TypeScript interface for every unique API response.
 * For example, if the backend returns `GlobalResponse<MemberDto>`, Orval creates
 * a file `globalResponseMemberDto.ts` with a flattened type `GlobalResponseMemberDto`.
 * The same happens for lists (`GlobalResponseListPaymentDto`) and for `SortOrder`
 * enums per entity (`MemberSearchRequestDtoSortOrder`).
 *
 * This leads to:
 *   - Hundreds of near-identical boilerplate files
 *   - Inconsistent naming across consumer code
 *   - Difficult manual maintenance
 *
 * **What this function does (in order):**
 *
 *   1. **Import cleanup** — Scans every `.ts` file's import blocks and replaces
 *      flattened imports (e.g., `GlobalResponseMemberDto`) with the unwrapped
 *      inner type (e.g., `MemberDto`). Java primitives (`Void`, `Integer`,
 *      `String`, `Boolean`) are dropped entirely since they map to TS builtins.
 *
 *   2. **Delete redundant GlobalResponseXxx files** — Removes the per-shape
 *      `globalResponseXxx.ts` files that Orval generated, since they are no
 *      longer needed after the generic `GlobalResponse<T>` is in place.
 *
 *   3. **Clean types/index.ts** — Strips the `export * from './globalResponseXxx'`
 *      and `export * from './xxxSortOrder'` re-exports that reference the deleted files.
 *
 *   4. **Inline type replacement** — Replaces every occurrence of flattened types
 *      in function signatures and return types:
 *        - `GlobalResponseMemberDto`        → `GlobalResponse<MemberDto>`
 *        - `GlobalResponseListPaymentDto`   → `GlobalResponse<PaymentDto[]>`
 *        - `GlobalResponseVoid`             → `GlobalResponse<void>`
 *        - `GlobalResponseInteger`          → `GlobalResponse<number>`
 *
 *   5. **SortOrder normalisation** — Collapses per-entity sort-order enums
 *      (e.g., `MemberSearchRequestDtoSortOrder`) into a single shared `SortOrder`
 *      type, deleting the per-entity files.
 *
 *   6. **Auto-import insertion** — If a file now references `GlobalResponse` or
 *      `SortOrder` but lacks the corresponding import, one is prepended.
 *
 *   7. **Generate GlobalResponse<T> wrapper** — Writes a reusable generic
 *      `GlobalResponse<T>` interface into `types/globalResponse.ts` and ensures
 *      it is re-exported from `types/index.ts`.
 */
function refactorOrvalTypes() {
  const files = getAllFiles(API_DIR);
  let globalResponseFiles = 0;
  let sortOrderFiles = 0;

  files.forEach((file) => {
    let content = fs.readFileSync(file, 'utf8');
    let hasChanges = false;

    const baseName = path.basename(file).replace('.ts', '');
    const isTypesIndex = baseName === 'index' && file.includes('/types/');

    // Track whether this file needs a `GlobalResponse` import added later
    let usesGlobalResponse = false;

    // ── Step 1: Clean up import blocks ──────────────────────────────────
    // Parse each `import { ... } from '...'` block and remove flattened
    // GlobalResponseXxx / XxxSortOrder imports, replacing them with the
    // unwrapped inner type where appropriate.
    const importRegex = /(import(?: type)?)\s*\{([^}]+)\}\s*from\s*'([^']+)';/g;
    content = content.replace(importRegex, (match, importTypeToken, importsStr, fromStr) => {
      const imports = importsStr
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      const filteredImports = [];
      let hadChanges = false;

      imports.forEach((imp) => {
        if (imp.startsWith('GlobalResponse') && imp !== 'GlobalResponse') {
          // e.g. GlobalResponseMemberDto → keep MemberDto, GlobalResponseListPaymentDto → keep PaymentDto
          const innerImp = imp.substring('GlobalResponse'.length);
          let unwrappedTarget = innerImp;
          if (innerImp.startsWith('List')) {
            unwrappedTarget = innerImp.substring(4);
          }
          // Java primitives map to TS builtins — no import needed
          if (
            unwrappedTarget !== 'Void' &&
            unwrappedTarget !== 'Integer' &&
            unwrappedTarget !== 'String' &&
            unwrappedTarget !== 'Boolean'
          ) {
            if (!filteredImports.includes(unwrappedTarget) && !imports.includes(unwrappedTarget)) {
              filteredImports.push(unwrappedTarget);
            }
          }
          hadChanges = true;
        } else if (imp.endsWith('SortOrder') && imp !== 'SortOrder') {
          // Remove per-entity SortOrders (e.g. MemberSearchRequestDtoSortOrder)
          hadChanges = true;
        } else {
          filteredImports.push(imp);
        }
      });

      if (hadChanges) {
        if (imports.some((imp) => imp.startsWith('GlobalResponse') && imp !== 'GlobalResponse')) {
          usesGlobalResponse = true;
        }
        hasChanges = true;
      }

      if (filteredImports.length === 0) {
        return '';
      }
      return `${importTypeToken} {\n  ${filteredImports.join(',\n  ')}\n} from '${fromStr}';`;
    });

    // ── Step 2: Delete redundant GlobalResponseXxx.ts files ─────────────
    if (baseName.startsWith('globalResponse') && baseName.length > 'globalResponse'.length) {
      fs.unlinkSync(file);
      globalResponseFiles++;
      return; // Stop processing this file — it no longer exists
    }

    // ── Step 3: Clean types/index.ts re-exports ─────────────────────────
    if (isTypesIndex) {
      // Remove re-exports of deleted GlobalResponseXxx files
      content = content.replace(/export \* from '\.\/globalResponse[A-Z]\w*';\n?/g, '');
      // Remove re-exports of deleted per-entity SortOrder files
      content = content.replace(/export \* from '\.\/(\w+)SortOrder';\n?/g, (match, prefix) => {
        if (prefix !== 'sortOrder' && prefix !== '') {
          return '';
        }
        return match;
      });
      hasChanges = true;
    }

    // ── Step 4: Inline type replacement ─────────────────────────────────
    // Replace flattened type names with generic GlobalResponse<T> syntax.
    if (content.match(/GlobalResponse(\w+)/)) {
      hasChanges = true;
      usesGlobalResponse = true;
      content = content.replace(/GlobalResponse(\w+)/g, (match, innerType) => {
        if (innerType === 'Void') return 'GlobalResponse<void>';
        if (innerType === 'Integer') return 'GlobalResponse<number>';
        if (innerType === 'Boolean') return 'GlobalResponse<boolean>';
        if (innerType === 'String') return 'GlobalResponse<string>';
        // List types: GlobalResponseListPaymentDto → GlobalResponse<PaymentDto[]>
        if (innerType.startsWith('List')) {
          return `GlobalResponse<${innerType.substring(4)}[]>`;
        }
        return `GlobalResponse<${innerType}>`;
      });
    }

    // ── Step 5: SortOrder normalisation ──────────────────────────────────
    // Collapse per-entity sort enums (e.g. MemberSearchRequestDtoSortOrder)
    // into the shared SortOrder type.
    if (content.match(/\w+SortOrder/g)) {
      content = content.replace(/\w+SortOrder/g, 'SortOrder');
      hasChanges = true;
    }

    // Delete per-entity SortOrder files (e.g. memberSearchRequestDtoSortOrder.ts)
    if (baseName.endsWith('SortOrder') && baseName !== 'sortOrder') {
      fs.unlinkSync(file);
      sortOrderFiles++;
      return;
    }

    // ── Step 6: Auto-import insertion ────────────────────────────────────
    // If the file references SortOrder but has no import for it, prepend one.
    const hasSortOrderImport = /import(?: type)?\s*\{[^}]*\bSortOrder\b[^}]*\}\s*from/.test(
      content
    );
    if (
      content.includes('SortOrder') &&
      !content.includes('export type SortOrder') &&
      !content.includes('export enum SortOrder') &&
      !hasSortOrderImport
    ) {
      const isEndpoint = file.includes('/endpoints.ts') || !file.includes('/types/');
      const importPath = isEndpoint ? '../types' : '.';
      content = `import type { SortOrder } from '${importPath}';\n` + content;
      hasChanges = true;
    }

    // If the file references GlobalResponse but has no import for it, prepend one.
    if (usesGlobalResponse) {
      const relativePathToConstants = file.includes('/types/')
        ? './globalResponse'
        : '../types/globalResponse';
      if (!content.includes('import type { GlobalResponse }')) {
        content = `import type { GlobalResponse } from '${relativePathToConstants}';\n` + content;
      }
    }

    // Write back only if modifications were made
    if (hasChanges) {
      fs.writeFileSync(file, content, 'utf8');
    }
  });

  // ── Step 7: Generate the generic GlobalResponse<T> wrapper ────────────
  // This single file replaces all the per-shape GlobalResponseXxx.ts files
  // that were deleted above.
  const customGlobalResponsePath = path.join(API_DIR, 'types', 'globalResponse.ts');
  const customContent = `import type { GlobalMsg } from './globalMsg';
import type { FieldMsg } from './fieldMsg';
import type { PaginationMeta } from './paginationMeta';

export interface GlobalResponse<T = any> {
  statusCode: number;
  statusDesc: string;
  globalMessages: GlobalMsg[];
  fieldMessages: FieldMsg[];
  responseData?: T;
  meta?: PaginationMeta;
}
`;
  if (fs.existsSync(path.join(API_DIR, 'types'))) {
    fs.writeFileSync(customGlobalResponsePath, customContent, 'utf8');

    // Ensure the generic wrapper is re-exported from types/index.ts
    const indexFilePath = path.join(API_DIR, 'types', 'index.ts');
    if (fs.existsSync(indexFilePath)) {
      let indexContent = fs.readFileSync(indexFilePath, 'utf8');
      if (!indexContent.includes('./globalResponse')) {
        indexContent += "export * from './globalResponse';\n";
        fs.writeFileSync(indexFilePath, indexContent, 'utf8');
      }
    }
  }

  console.log(`Phase 1 complete: Removed ${globalResponseFiles} GlobalResponse and ${sortOrderFiles} SortOrder duplicate schemas. Generic wrapper auto-generated.`);
}

// ─────────────────────────────────────────────────────────────────────────────
// Phase 2: Generate singleton exports from Orval factory functions
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Phase 2: Generate singleton exports from Orval factory functions.
 *
 * **Problem:**
 * Orval generates factory functions like `export const getMembers = () => { ... }`.
 * Each call returns a NEW object, which causes React infinite loops when used
 * as hook dependencies without `useMemo`.
 *
 * **What this function does:**
 * Scans each endpoint directory (excluding `types/`) for factory functions
 * matching `export const getXxx = () =>` and appends a pre-called singleton:
 *
 *   export const membersApi = getMembers();
 *
 * **Naming convention:**
 *   getMembers       → membersApi
 *   getAuthentication → authenticationApi
 *   getDashboard     → dashboardApi
 *
 * Consumers import the singleton directly — `membersApi.addMember(dto)` —
 * with no `useMemo` wrappers needed.
 */
function generateSingletons() {
  // Scan only endpoint directories (skip the shared `types/` folder)
  const endpointDirs = fs.readdirSync(API_DIR).filter((entry) => {
    const fullPath = path.join(API_DIR, entry);
    return fs.statSync(fullPath).isDirectory() && entry !== 'types';
  });

  let count = 0;

  endpointDirs.forEach((dir) => {
    const dirPath = path.join(API_DIR, dir);
    const files = fs.readdirSync(dirPath).filter((f) => f.endsWith('.ts'));

    files.forEach((file) => {
      const filePath = path.join(dirPath, file);
      let content = fs.readFileSync(filePath, 'utf8');

      // Match Orval factory pattern: export const getXxx = () =>
      const factoryRegex = /export const (get\w+) = \(\) =>/g;
      let match;
      const singletons = [];

      while ((match = factoryRegex.exec(content)) !== null) {
        const factoryName = match[1]; // e.g. "getMembers"
        // Derive singleton name: getMembers → membersApi, getAuthentication → authenticationApi
        const baseName = factoryName.replace(/^get/, '');
        const singletonName = baseName.charAt(0).toLowerCase() + baseName.slice(1) + 'Api';
        singletons.push({ factoryName, singletonName });
      }

      if (singletons.length > 0) {
        // Append singleton exports at the end of the file
        const singletonLines = singletons
          .map(({ factoryName, singletonName }) =>
            `\n/** Pre-called singleton — import this instead of calling ${factoryName}() directly. */\nexport const ${singletonName} = ${factoryName}();`
          )
          .join('\n');

        content += singletonLines + '\n';
        fs.writeFileSync(filePath, content, 'utf8');
        count++;
      }
    });
  });

  console.log(`Phase 2 complete: Created stable API singletons in ${count} endpoint file(s).`);
}

// ─────────────────────────────────────────────────────────────────────────────
// Execution
// ─────────────────────────────────────────────────────────────────────────────

refactorOrvalTypes();
generateSingletons();
