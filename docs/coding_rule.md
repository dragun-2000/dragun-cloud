# CODING RULE - SINGLE CONVENTION

This document is the mandatory prompt and coding convention for all generated or modified code in this project.
All contributors (human and AI) must follow exactly one unified rule set.

## 1) Constants and literals (MANDATORY — read first)

### 1.1 Constant naming

- All constants must use `UPPER_SNAKE_CASE`.
- Use full words; avoid ambiguous abbreviations.
- Name format: `<DOMAIN>_<MEANING>[_<UNIT_OR_TYPE>]`.

Examples:

- `HTTP_STATUS_OK`
- `SPIRAL_REQUEST_TIMEOUT_SECONDS`
- `DATE_FORMAT_YYYY_MM_DD`
- `BATCH_IMPORT_COUNT_MESSAGE`

Forbidden:

- camelCase for constants (`httpStatusOk`)
- mixed style (`Http_Status_OK`)
- unclear abbreviations (`TMP_VAL`, `FT_DATE_UI` for new code)

### 1.2 No magic literals in business code

- Do not hardcode business literals (URLs, paths, model keys, message keys, protocol values) directly in logic.
- Centralize in `AppConstants`, `MessageConstants`, or `private static final` in the same compilation unit when scope is local.
- Allowed inline literals only when universally obvious:
  - `0`, `1` in simple loop/index operations
  - empty collection creation
  - language-required annotations/keywords

## 2) Imports (MANDATORY)

- **Wildcard imports are forbidden.** Do not use `import package.*` or `import static package.Class.*`.
- Use **explicit** imports only: one fully qualified type or static member per import line.
- Applies to Java and any language with equivalent “star import” syntax.

Forbidden:

```java
import java.util.*;
import static org.junit.Assert.*;
```

Required style:

```java
import java.util.ArrayList;
import java.util.List;
```

## 3) Core principle

- Preserve existing business logic and behavior.
- Refactoring is allowed only for readability, naming consistency, and maintainability.
- Never change input/output contract unless explicitly requested.

## 4) Naming (non-constant symbols)

- Class/Interface: `PascalCase`
- Method/Variable: `camelCase`
- Package: lowercase
- Boolean names should start with `is/has/can/should`

## 5) Error message and text

- User-facing messages must be centralized in `MessageConstants`.
- System keys, protocol keys, header names, JSON field names must be centralized in `AppConstants` or file-local `private static final` constants.
- Fix spelling and wording when touching related code.

## 6) Logic safety

- Do not alter current behavior while refactoring names/constants.
- Keep API responses, DB queries, and validation behavior unchanged unless explicitly requested.
- For each refactor commit, verify:
  - compile success (if environment permits)
  - no changed branching behavior
  - no changed default values

## 7) Formatting and style

- Follow Java standard formatting.
- Keep methods focused and small.
- Avoid duplicated code; extract helper methods with meaningful names.
- Add comments only for non-obvious business logic.

### 7.1 String composition (Java)

- **Do not use `+` to concatenate multiple string parts** in application/business code. Use `String.format(Locale, String, Object...)` when locale matters, or `String.format(String, Object...)` / `"%s".formatted(...)` when default locale is acceptable.
- Put reusable format patterns in constants (e.g. `AppConstants`) when they encode a stable contract.
- Trivial exception: a single `+` joining two literals in tests or throwaway scripts only — not in production service/controller code.

### 7.2 Spelling and English identifiers

- Use correct English spelling in identifiers and configuration keys (`register`, not `regist`; `forgot-password`, not `forgot-pasword`, etc.).
- When renaming config keys, prefer a **backward-compatible** fallback in `@Value` (e.g. new key with default from legacy key) until all environments are migrated.

## 8) AI prompt instruction (use this exactly)

When generating code for this project, AI must obey:

1. **Constants first mindset:** use `UPPER_SNAKE_CASE` constants and centralized literals per sections 1 and 5; no business magic strings in logic.
2. **No wildcard imports:** every import must be explicit (section 2).
3. Keep business logic unchanged unless the user explicitly asks to change logic.
4. Apply one naming convention for non-constant symbols: class `PascalCase`, method/variable `camelCase`.
5. Ensure spelling is correct and terminology consistent (section 7.2).
6. **String composition:** prefer `String.format` / `formatted` over chained `+` for multi-part strings (section 7.1).
7. Follow existing project architecture and Spring Boot conventions.
8. Produce clean, readable, maintainable code with minimal-risk refactor.

---

Version: 1.2  
Scope: Entire project  
Priority: Highest
