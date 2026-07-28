import { mkdirSync, writeFileSync } from "node:fs";

const output = "docs/erd/course-exam-matrix-chen.svg";
const W = 2200;
const H = 1700;

const entities = {
  course:      { name: "COURSES", x: 920, y: 125, w: 360, key: "id · PK", attrs: "course_name, course_code",
                 keyPos: [770, 167], attrPos: [1100, 82] },
  matrix:      { name: "EXAM_MATRICES", x: 920, y: 385, w: 360, key: "id · PK", attrs: "course_id · FK, title, description",
                 keyPos: [760, 427], attrPos: [1430, 520] },
  row:         { name: "EXAM_MATRIX_ROWS", x: 890, y: 700, w: 420, key: "id · PK", attrs: "matrix_id, chapter_id, topic, order_index",
                 keyPos: [700, 855], attrPos: [1150, 855] },
  level:       { name: "CUSTOM_LEARNING_LEVELS", x: 1330, y: 1010, w: 440, key: "id · PK", attrs: "matrix_id · FK, code, name, sort_order" },
  chapter:     { name: "CHAPTERS", x: 140, y: 700, w: 360, key: "chapter_id · PK", attrs: "course_id · FK, title" },
  outcome:     { name: "LEARNING_OUTCOMES", x: 1700, y: 700, w: 390, key: "id + course_id · PK", attrs: "course_id · FK, group_learning_outcome_id" },
  group:       { name: "GROUP_LEARNING_OUTCOMES", x: 1660, y: 385, w: 430, key: "id + course_id · PK", attrs: "course_id · FK, code",
                 keyPos: [1510, 427], attrPos: [1875, 342] },
  job:         { name: "QUIZ_GENERATION_JOBS", x: 860, y: 1280, w: 480, key: "id · PK", attrs: "course_id, exam_matrix_id, quiz_id, requested_by" },
  user:        { name: "USERS", x: 140, y: 1280, w: 360, key: "id · PK", attrs: "email" },
  quiz:        { name: "QUIZZES", x: 1700, y: 1280, w: 390, key: "id · PK", attrs: "course_id · FK, quiz_title" },
};

for (const entity of Object.values(entities)) {
  entity.h = 84;
  entity.cx = entity.x + entity.w / 2;
  entity.cy = entity.y + entity.h / 2;
}

const relations = [
  { a: "course", b: "matrix", name: "OWNS", cardA: "1", cardB: "N", x: 1100, y: 300, fk: "exam_matrices.course_id", del: "CASCADE" },
  { a: "matrix", b: "row", name: "CONTAINS", cardA: "1", cardB: "N", x: 1100, y: 570, fk: "exam_matrix_rows.matrix_id", del: "CASCADE" },
  { a: "matrix", b: "level", name: "DEFINES", cardA: "1", cardB: "N", x: 1370, y: 700, fk: "custom_learning_levels.matrix_id", del: "CASCADE" },
  { a: "chapter", b: "row", name: "SCOPES", cardA: "1", cardB: "N", x: 680, y: 742, fk: "exam_matrix_rows.chapter_id", del: "SET NULL", optional: true },
  { a: "outcome", b: "row", name: "SELECTS", cardA: "N", cardB: "N", x: 1510, y: 742,
    fk: "exam_matrix_row_learning_outcomes · is_selected", del: "CASCADE", reverse: true },
  { a: "outcome", b: "level", name: "CLASSIFIES", cardA: "N", cardB: "N", x: 1810, y: 930,
    fk: "custom_learning_level_learning_outcomes · is_selected", del: "CASCADE", reverse: true },
  { a: "group", b: "outcome", name: "GROUPS", cardA: "1", cardB: "N", x: 1895, y: 575, fk: "learning_outcomes.(group_id, course_id)", del: "SET NULL", optional: true },

  // Course-owned dimensions.
  { a: "course", b: "chapter", name: "HAS", cardA: "1", cardB: "N", x: 430, y: 355, fk: "chapters.course_id", del: "CASCADE" },
  { a: "course", b: "group", name: "DEFINES", cardA: "1", cardB: "N", x: 1575, y: 250, fk: "group_learning_outcomes.course_id", del: "CASCADE" },
  { a: "course", b: "outcome", name: "DEFINES", cardA: "1", cardB: "N", x: 1810, y: 285, fk: "learning_outcomes.course_id", del: "CASCADE" },

  // Conceptual N-N relationship implemented by the schema junction table.
  { a: "chapter", b: "outcome", name: "MAPS", cardA: "N", cardB: "N", x: 1100, y: 955,
    fk: "implemented by chapter_learning_outcomes", del: "CASCADE" },

  // Durable generation-job relationships.
  { a: "matrix", b: "job", name: "USED BY", cardA: "1", cardB: "N", x: 1375, y: 1105,
    fk: "quiz_generation_jobs.exam_matrix_id", del: "RESTRICT" },
  { a: "course", b: "job", name: "QUEUES", cardA: "1", cardB: "N", x: 660, y: 1110,
    fk: "quiz_generation_jobs.course_id", del: "CASCADE" },
  { a: "user", b: "job", name: "REQUESTS", cardA: "1", cardB: "N", x: 675, y: 1322,
    fk: "quiz_generation_jobs.requested_by", del: "CASCADE" },
  { a: "quiz", b: "job", name: "RESULTS IN", cardA: "1", cardB: "N", x: 1515, y: 1322,
    fk: "quiz_generation_jobs.quiz_id", del: "SET NULL", optional: true, reverse: true },
];

const esc = text => String(text).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const edgeColor = action => ({
  "CASCADE": "#dc2626",
  "SET NULL": "#7c3aed",
  "RESTRICT": "#ea580c",
})[action] ?? "#475569";

const svg = [];
svg.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="title desc">`);
svg.push(`<title id="title">Course and exam matrix ERD in Chen notation</title>`);
svg.push(`<desc id="desc">Chen ERD derived from schema.sql using 1-N and N-N relationship cardinality notation.</desc>`);
svg.push(`<defs><style>
  .bg{fill:#f8fafc}.title{font:700 34px Inter,Arial,sans-serif;fill:#0f172a}.subtitle{font:17px Inter,Arial,sans-serif;fill:#475569}
  .entity{fill:#dbeafe;stroke:#2563eb;stroke-width:3}.entity-name{font:700 18px Inter,Arial,sans-serif;fill:#1e3a8a;text-anchor:middle;dominant-baseline:middle}
  .attribute{fill:#fff;stroke:#64748b;stroke-width:2}.key{fill:#dcfce7;stroke:#15803d;stroke-width:2.5}
  .attr-text{font:13px Inter,Arial,sans-serif;fill:#334155;text-anchor:middle;dominant-baseline:middle}.key-text{font:700 13px Inter,Arial,sans-serif;fill:#14532d;text-anchor:middle;dominant-baseline:middle;text-decoration:underline}
  .attr-line{stroke:#94a3b8;stroke-width:1.8}.edge{fill:none;stroke-width:3}.diamond{fill:#fef3c7;stroke:#b45309;stroke-width:3}
  .relation{font:700 13px Inter,Arial,sans-serif;fill:#78350f;text-anchor:middle;dominant-baseline:middle}
  .end-card{font:800 18px Inter,Arial,sans-serif;fill:#be123c;paint-order:stroke;stroke:#fff;stroke-width:6px}
  .card-badge{fill:#ffe4e6;stroke:#be123c;stroke-width:2}.card-badge-text{font:800 15px Inter,Arial,sans-serif;fill:#9f1239;text-anchor:middle;dominant-baseline:middle}
  .fk{font:12px Inter,Arial,sans-serif;fill:#334155;text-anchor:middle;paint-order:stroke;stroke:#f8fafc;stroke-width:5px}
  .note-box{fill:#fff;stroke:#cbd5e1;stroke-width:2}.note-title{font:700 15px Inter,Arial,sans-serif;fill:#0f172a}.note{font:13px Inter,Arial,sans-serif;fill:#334155}
</style></defs>`);
svg.push(`<rect class="bg" width="${W}" height="${H}"/>`);
svg.push(`<text class="title" x="55" y="52">Course and Exam Matrix ERD</text>`);
svg.push(`<text class="subtitle" x="55" y="82">Chen notation • relationship types use 1-N and N-N • derived from src/main/resources/schema.sql</text>`);

// Draw relationship connectors first so entities remain visually prominent.
for (const r of relations) {
  const a = entities[r.a];
  const b = entities[r.b];
  const color = edgeColor(r.del);
  svg.push(`<path class="edge" stroke="${color}" d="M${a.cx},${a.cy} Q${r.x},${r.y} ${r.x},${r.y} Q${r.x},${r.y} ${b.cx},${b.cy}"/>`);
}

// Relationship symbols and explicit 1-N/N-N notation.
for (const r of relations) {
  const a = entities[r.a];
  const b = entities[r.b];
  const dw = Math.max(55, r.name.length * 6);
  const type = `${r.cardA}-${r.cardB}`;
  svg.push(`<polygon class="diamond" points="${r.x},${r.y - 31} ${r.x + dw},${r.y} ${r.x},${r.y + 31} ${r.x - dw},${r.y}"/>`);
  svg.push(`<text class="relation" x="${r.x}" y="${r.y}">${esc(r.name)}</text>`);

  const ax = a.cx + (r.x - a.cx) * 0.32;
  const ay = a.cy + (r.y - a.cy) * 0.32 - 12;
  const bx = r.x + (b.cx - r.x) * 0.68;
  const by = r.y + (b.cy - r.y) * 0.68 - 12;
  svg.push(`<text class="end-card" x="${ax}" y="${ay}">${r.cardA}</text>`);
  svg.push(`<text class="end-card" x="${bx}" y="${by}">${r.cardB}</text>`);

  svg.push(`<rect class="card-badge" x="${r.x - 31}" y="${r.y - 67}" width="62" height="25" rx="12"/>`);
  svg.push(`<text class="card-badge-text" x="${r.x}" y="${r.y - 54}">${type}</text>`);
  const optional = r.optional ? " • optional FK" : "";
  svg.push(`<text class="fk" x="${r.x}" y="${r.y + 50}">${esc(r.fk)} • ${esc(r.del)}${optional}</text>`);
}

// Entities with key and selected schema attributes.
for (const e of Object.values(entities)) {
  const keyX = e.keyPos?.[0] ?? e.cx;
  const keyY = e.keyPos?.[1] ?? e.y + e.h + 53;
  const keyRx = Math.max(70, Math.min(150, e.key.length * 6.2));
  const attrX = e.attrPos?.[0] ?? e.cx;
  const attrY = e.attrPos?.[1] ?? e.y - 43;
  const attrRx = Math.max(100, Math.min(230, e.attrs.length * 5.2));
  svg.push(`<line class="attr-line" x1="${e.cx}" y1="${e.cy}" x2="${attrX}" y2="${attrY}"/>`);
  svg.push(`<line class="attr-line" x1="${e.cx}" y1="${e.cy}" x2="${keyX}" y2="${keyY}"/>`);
  svg.push(`<rect class="entity" x="${e.x}" y="${e.y}" width="${e.w}" height="${e.h}" rx="10"/>`);
  svg.push(`<text class="entity-name" x="${e.cx}" y="${e.cy}">${e.name}</text>`);
  svg.push(`<ellipse class="attribute" cx="${attrX}" cy="${attrY}" rx="${attrRx}" ry="25"/>`);
  svg.push(`<text class="attr-text" x="${attrX}" y="${attrY}">${esc(e.attrs)}</text>`);
  svg.push(`<ellipse class="key" cx="${keyX}" cy="${keyY}" rx="${keyRx}" ry="25"/>`);
  svg.push(`<text class="key-text" x="${keyX}" y="${keyY}">${esc(e.key)}</text>`);
}

// Legend and interpretation notes.
svg.push(`<rect class="note-box" x="55" y="1515" width="2090" height="145" rx="12"/>`);
svg.push(`<text class="note-title" x="80" y="1545">How to read cardinality</text>`);
svg.push(`<text class="note" x="80" y="1572">1-N: one parent can relate to many children; each child references at most one parent. N-N: many entities on both sides, implemented here by a junction table.</text>`);
svg.push(`<text class="note" x="80" y="1598">Nullable FKs are still classified by relationship type, with “optional FK” shown separately. There is no declared 1-1 relationship in this exam-matrix subgraph.</text>`);
svg.push(`<text class="note" x="80" y="1624">Row/outcome and custom-level/outcome junctions materialize every eligible pair and store the checkbox state in is_selected.</text>`);
svg.push(`<text class="note" x="80" y="1650">Line colors: red = CASCADE, purple = SET NULL, orange = RESTRICT.</text>`);
svg.push(`</svg>`);

mkdirSync("docs/erd", { recursive: true });
writeFileSync(output, svg.join("\n"));
console.log(`Wrote ${output}: ${Object.keys(entities).length} entities and ${relations.length} relationships`);
