import { mkdirSync, writeFileSync } from "node:fs";

const output = "docs/erd/full-schema-chen.svg";
const width = 5200;
const height = 3800;

const panels = [
  { x: 60, y: 140, w: 1640, h: 1520, title: "1. Identity and course membership" },
  { x: 1730, y: 140, w: 1680, h: 1520, title: "2. Curriculum, materials and exam matrices" },
  { x: 3440, y: 140, w: 1700, h: 2250, title: "3. Quiz generation and assessment" },
  { x: 60, y: 1700, w: 3350, h: 1990, title: "4. Activity, notifications, chat and infrastructure" },
  { x: 3440, y: 2430, w: 1700, h: 1260, title: "Schema notes and legend" },
];

const nodes = new Map();
function entity(id, label, x, y, pk = "id", options = {}) {
  nodes.set(id, { id, label, x, y, w: options.w ?? 330, h: 78, pk, ref: options.ref ?? false, note: options.note ?? "" });
}

// Panel 1
entity("user", "USERS", 730, 270, "id");
entity("student", "STUDENTS", 230, 560, "user_id (PK/FK)");
entity("teacher", "TEACHERS", 1230, 560, "user_id (PK/FK)");
entity("course", "COURSES", 730, 820, "id");
entity("enrollment", "ENROLLMENTS", 230, 1160, "course_id + user_id", { w: 370 });
entity("announcement", "COURSE_ANNOUNCEMENTS", 1190, 1160, "id", { w: 410 });

// Panel 2
entity("course-curriculum", "COURSES", 2410, 245, "id", { ref: true });
entity("chapter", "CHAPTERS", 1840, 510, "chapter_id");
entity("material", "MATERIALS", 2390, 510, "id");
entity("group-lo", "GROUP_LEARNING_OUTCOMES", 2920, 510, "id + course_id", { w: 430 });
entity("chapter-material", "CHAPTER_MATERIALS", 1810, 850, "material_id + chapter_id", { w: 390 });
entity("learning-outcome", "LEARNING_OUTCOMES", 2860, 850, "id + course_id", { w: 400 });
entity("chapter-lo", "CHAPTER_LEARNING_OUTCOMES", 2250, 1110, "chapter_id + learning_outcome_id", { w: 470 });
entity("exam-matrix", "EXAM_MATRICES", 2930, 1170, "id");
entity("exam-row", "EXAM_MATRIX_ROWS", 2620, 1470, "id", { w: 380 });
entity("material-job", "MATERIAL_INDEX_JOBS", 1800, 1430, "id", { w: 400, note: "No declared foreign keys" });

// Panel 3
entity("course-quiz", "COURSES", 4160, 245, "id", { ref: true });
entity("student-quiz", "STUDENTS", 3520, 245, "user_id", { ref: true });
entity("teacher-quiz", "TEACHERS", 4800, 245, "user_id", { ref: true });
entity("matrix-quiz", "EXAM_MATRICES", 4800, 520, "id", { ref: true });
entity("user-quiz", "USERS", 4800, 790, "id", { ref: true });
entity("quiz", "QUIZZES", 4160, 610, "id");
entity("generation-job", "QUIZ_GENERATION_JOBS", 4630, 1030, "id", { w: 450 });
entity("question", "QUESTIONS", 3790, 1030, "id");
entity("question-image", "QUESTION_IMAGES", 3480, 1370, "id", { w: 380 });
entity("answer", "ANSWERS", 4020, 1370, "answer_number + question_id", { w: 360 });
entity("attempt", "ATTEMPTS", 3480, 1780, "id");
entity("quiz-chapter", "QUIZ_CHAPTERS", 4100, 1780, "quiz_id + chapter_id", { w: 370 });
entity("quiz-lo", "QUIZ_LEARNING_OUTCOMES", 4590, 1450, "quiz_id + learning_outcome_id", { w: 440 });
entity("question-lo", "QUESTION_LEARNING_OUTCOMES", 4590, 2040, "question_id + learning_outcome_id", { w: 490 });
entity("chapter-quiz", "CHAPTERS", 4100, 2190, "chapter_id", { ref: true });
entity("lo-quiz", "LEARNING_OUTCOMES", 4790, 1780, "id", { ref: true, w: 400 });

// Panel 4
entity("user-engagement", "USERS", 700, 1860, "id", { ref: true });
entity("course-engagement", "COURSES", 2470, 1860, "id", { ref: true });
entity("activity", "ACTIVITY_LOGS", 160, 2220, "id", { w: 360 });
entity("notification", "NOTIFICATIONS", 620, 2220, "id");
entity("direct-message", "USER_CHAT_MESSAGES", 1120, 2220, "id", { w: 410 });
entity("ai-session", "AI_CHAT_SESSIONS", 2180, 2260, "id", { w: 390 });
entity("ai-message", "AI_COURSE_CHAT_MESSAGES", 2500, 2750, "id", { w: 490 });
entity("event-publication", "EVENT_PUBLICATION", 300, 3090, "id", { w: 410, note: "No declared foreign keys" });

const relationships = [];
function relation(from, to, label, fromCard, toCard, fk, onDelete = "NO ACTION", options = {}) {
  relationships.push({ from, to, label, fromCard, toCard, fk, onDelete, ...options });
}

// Identity/course
relation("user", "student", "HAS PROFILE", "(0,1)", "(1,1)", "students.user_id", "CASCADE");
relation("user", "teacher", "HAS PROFILE", "(0,1)", "(1,1)", "teachers.user_id", "CASCADE");
relation("user", "enrollment", "ENROLLS", "(0,N)", "(1,1)", "enrollments.user_id", "CASCADE", { dx: -80 });
relation("course", "enrollment", "ACCEPTS", "(0,N)", "(1,1)", "enrollments.course_id", "CASCADE", { dx: 75 });
relation("course", "announcement", "HAS", "(0,N)", "(1,1)", "course_announcements.course_id", "CASCADE", { dx: -80 });
relation("user", "announcement", "AUTHORS", "(0,N)", "(1,1)", "course_announcements.teacher_id", "NO ACTION", { dx: 85 });

// Curriculum/materials/matrix
relation("course-curriculum", "chapter", "HAS", "(0,N)", "(1,1)", "chapters.course_id", "CASCADE", { dx: -40 });
relation("course-curriculum", "material", "HAS", "(0,N)", "(0,1)", "materials.course_id", "CASCADE");
relation("course-curriculum", "group-lo", "DEFINES", "(0,N)", "(1,1)", "group_learning_outcomes.course_id", "CASCADE", { dx: 45 });
relation("course-curriculum", "learning-outcome", "DEFINES", "(0,N)", "(1,1)", "learning_outcomes.course_id", "CASCADE", { dx: 80 });
relation("course-curriculum", "exam-matrix", "OWNS", "(0,N)", "(1,1)", "exam_matrices.course_id", "CASCADE", { dx: 125 });
relation("chapter", "chapter-material", "LINKS", "(0,N)", "(1,1)", "chapter_materials.chapter_id", "CASCADE", { dx: -55 });
relation("material", "chapter-material", "LINKS", "(0,N)", "(1,1)", "chapter_materials.material_id", "CASCADE", { dx: 55 });
relation("group-lo", "learning-outcome", "GROUPS", "(0,N)", "(0,1)", "learning_outcomes.(group_id, course_id)", "SET NULL", { dx: 75 });
relation("chapter", "chapter-lo", "MAPS", "(0,N)", "(1,1)", "chapter_learning_outcomes.chapter_id", "CASCADE", { dx: -100 });
relation("learning-outcome", "chapter-lo", "MAPS", "(0,N)", "(1,1)", "chapter_learning_outcomes.learning_outcome_id", "CASCADE", { dx: 80 });
relation("exam-matrix", "exam-row", "CONTAINS", "(0,N)", "(1,1)", "exam_matrix_rows.matrix_id", "CASCADE", { dx: 75 });
relation("chapter", "exam-row", "SCOPES", "(0,N)", "(0,1)", "exam_matrix_rows.chapter_id", "SET NULL", { dx: -130 });
relation("learning-outcome", "exam-row", "TARGETS", "(0,N)", "(1,1)", "exam_matrix_rows.learning_outcome_id", "CASCADE", { dx: 80 });

// Quiz/assessment
relation("course-quiz", "quiz", "HAS", "(0,N)", "(1,1)", "quizzes.course_id", "NO ACTION");
relation("student-quiz", "quiz", "CREATES", "(0,N)", "(0,1)", "quizzes.student_id", "NO ACTION", { dx: -70 });
relation("teacher-quiz", "quiz", "CREATES", "(0,N)", "(0,1)", "quizzes.teacher_id", "NO ACTION", { dx: 70 });
relation("course-quiz", "generation-job", "QUEUES", "(0,N)", "(1,1)", "quiz_generation_jobs.course_id", "CASCADE", { dx: 95 });
relation("matrix-quiz", "generation-job", "USES", "(0,N)", "(1,1)", "quiz_generation_jobs.exam_matrix_id", "RESTRICT", { dx: 70 });
relation("user-quiz", "generation-job", "REQUESTS", "(0,N)", "(1,1)", "quiz_generation_jobs.requested_by", "CASCADE", { dx: 95 });
relation("quiz", "generation-job", "PRODUCES", "(0,N)", "(0,1)", "quiz_generation_jobs.quiz_id", "SET NULL", { dx: -60 });
relation("quiz", "question", "CONTAINS", "(0,N)", "(0,1)", "questions.quiz_id", "CASCADE", { dx: -45 });
relation("question", "question-image", "HAS", "(0,N)", "(1,1)", "question_images.question_id", "CASCADE", { dx: -55 });
relation("question", "answer", "HAS", "(0,N)", "(1,1)", "answers.question_id", "NO ACTION", { dx: 55 });
relation("student-quiz", "attempt", "MAKES", "(0,N)", "(1,1)", "attempts.student_id", "NO ACTION", { dx: -115 });
relation("quiz", "attempt", "RECEIVES", "(0,N)", "(1,1)", "attempts.quiz_id", "NO ACTION", { dx: -70 });
relation("quiz", "quiz-chapter", "MAPS", "(0,N)", "(1,1)", "quiz_chapters.quiz_id", "CASCADE", { dx: -45 });
relation("chapter-quiz", "quiz-chapter", "MAPS", "(0,N)", "(1,1)", "quiz_chapters.chapter_id", "CASCADE", { dx: 45 });
relation("quiz", "quiz-lo", "ASSESSES", "(0,N)", "(1,1)", "quiz_learning_outcomes.quiz_id", "CASCADE", { dx: 95 });
relation("lo-quiz", "quiz-lo", "ASSESSED BY", "(0,N)", "(1,1)", "quiz_learning_outcomes.learning_outcome_id", "CASCADE", { dx: 55 });
relation("question", "question-lo", "ASSESSES", "(0,N)", "(1,1)", "question_learning_outcomes.question_id", "CASCADE", { dx: 120 });
relation("lo-quiz", "question-lo", "ASSESSED BY", "(0,N)", "(1,1)", "question_learning_outcomes.learning_outcome_id", "CASCADE", { dx: -55 });

// Activity/chat
relation("user-engagement", "activity", "GENERATES", "(0,N)", "(1,1)", "activity_logs.user_id", "CASCADE", { dx: -120 });
relation("user-engagement", "notification", "RECEIVES", "(0,N)", "(1,1)", "notifications.user_id", "CASCADE", { dx: -35 });
relation("user-engagement", "direct-message", "SENDS", "(0,N)", "(1,1)", "user_chat_messages.sender_id", "CASCADE", { dx: 35 });
relation("user-engagement", "direct-message", "RECEIVES", "(0,N)", "(1,1)", "user_chat_messages.recipient_id", "CASCADE", { dx: 145, dy: 70 });
relation("user-engagement", "ai-session", "OWNS", "(0,N)", "(1,1)", "ai_chat_sessions.user_id", "CASCADE", { dx: 70 });
relation("course-engagement", "ai-session", "CONTEXT", "(0,N)", "(1,1)", "ai_chat_sessions.course_id", "CASCADE", { dx: -55 });
relation("ai-session", "ai-message", "CONTAINS", "(0,N)", "(1,1)", "ai_course_chat_messages.session_id", "CASCADE", { dx: -65 });
relation("user-engagement", "ai-message", "WRITES", "(0,N)", "(1,1)", "ai_course_chat_messages.user_id", "CASCADE", { dx: 125 });
relation("course-engagement", "ai-message", "CONTEXT", "(0,N)", "(1,1)", "ai_course_chat_messages.course_id", "CASCADE", { dx: 65 });

const esc = value => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
const node = id => {
  const value = nodes.get(id);
  if (!value) throw new Error(`Unknown node ${id}`);
  return value;
};
const center = n => ({ x: n.x + n.w / 2, y: n.y + n.h / 2 });
const edgeColor = action => ({
  "CASCADE": "#dc2626",
  "SET NULL": "#7c3aed",
  "RESTRICT": "#ea580c",
  "NO ACTION": "#475569",
})[action] ?? "#475569";

const parts = [];
parts.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-labelledby="title desc">`);
parts.push(`<title id="title">Complete LMS schema ERD in Chen notation</title>`);
parts.push(`<desc id="desc">All 30 tables and all declared foreign key relationships from schema.sql, arranged into four domain panels.</desc>`);
parts.push(`<defs><style>
  .bg{fill:#f8fafc}.panel{fill:#fff;stroke:#cbd5e1;stroke-width:3}.panel-title{font:700 25px Inter,Arial,sans-serif;fill:#0f172a}
  .title{font:700 40px Inter,Arial,sans-serif;fill:#0f172a}.subtitle{font:19px Inter,Arial,sans-serif;fill:#475569}
  .entity{fill:#dbeafe;stroke:#2563eb;stroke-width:4}.reference{fill:#eff6ff;stroke:#2563eb;stroke-width:4;stroke-dasharray:12 8}
  .entity-name{font:700 18px Inter,Arial,sans-serif;fill:#1e3a8a;text-anchor:middle;dominant-baseline:middle}
  .ref-label{font:italic 13px Inter,Arial,sans-serif;fill:#475569;text-anchor:middle}
  .attribute{fill:#dcfce7;stroke:#15803d;stroke-width:3}.attribute-text{font:700 13px Inter,Arial,sans-serif;fill:#14532d;text-anchor:middle;dominant-baseline:middle;text-decoration:underline}
  .attribute-line{stroke:#94a3b8;stroke-width:2}.relationship{fill:#fef3c7;stroke:#b45309;stroke-width:3}
  .relationship-name{font:700 13px Inter,Arial,sans-serif;fill:#78350f;text-anchor:middle;dominant-baseline:middle}
  .edge{fill:none;stroke-width:3}.card{font:700 13px Inter,Arial,sans-serif;fill:#be123c;paint-order:stroke;stroke:#fff;stroke-width:5px}
  .fk{font:12px Inter,Arial,sans-serif;fill:#334155;text-anchor:middle;paint-order:stroke;stroke:#fff;stroke-width:5px}
  .isolated{font:italic 13px Inter,Arial,sans-serif;fill:#64748b;text-anchor:middle}
  .note-title{font:700 20px Inter,Arial,sans-serif;fill:#0f172a}.note{font:16px Inter,Arial,sans-serif;fill:#334155}
</style></defs>`);
parts.push(`<rect class="bg" width="${width}" height="${height}"/>`);
parts.push(`<text class="title" x="70" y="65">Complete LMS Database ERD</text>`);
parts.push(`<text class="subtitle" x="70" y="102">Chen notation • all tables and declared foreign keys in src/main/resources/schema.sql • non-key attributes omitted for readability</text>`);

for (const p of panels) {
  parts.push(`<rect class="panel" x="${p.x}" y="${p.y}" width="${p.w}" height="${p.h}" rx="18"/>`);
  parts.push(`<text class="panel-title" x="${p.x + 25}" y="${p.y + 42}">${esc(p.title)}</text>`);
}

// Relationship connectors.
for (const r of relationships) {
  const a = center(node(r.from));
  const b = center(node(r.to));
  const mx = (a.x + b.x) / 2 + (r.dx ?? 0);
  const my = (a.y + b.y) / 2 + (r.dy ?? 0);
  const color = edgeColor(r.onDelete);
  parts.push(`<path class="edge" stroke="${color}" d="M${a.x},${a.y} Q${mx},${my} ${mx},${my} Q${mx},${my} ${b.x},${b.y}"/>`);
}

// Relationship diamonds, labels, cardinalities and FK annotations.
for (const r of relationships) {
  const a = center(node(r.from));
  const b = center(node(r.to));
  const mx = (a.x + b.x) / 2 + (r.dx ?? 0);
  const my = (a.y + b.y) / 2 + (r.dy ?? 0);
  const diamondWidth = Math.max(58, Math.min(105, r.label.length * 5.2));
  parts.push(`<polygon class="relationship" points="${mx},${my - 31} ${mx + diamondWidth},${my} ${mx},${my + 31} ${mx - diamondWidth},${my}"/>`);
  parts.push(`<text class="relationship-name" x="${mx}" y="${my}">${esc(r.label)}</text>`);
  const firstX = a.x + (mx - a.x) * 0.28;
  const firstY = a.y + (my - a.y) * 0.28 - 12;
  const secondX = mx + (b.x - mx) * 0.72;
  const secondY = my + (b.y - my) * 0.72 - 12;
  parts.push(`<text class="card" x="${firstX}" y="${firstY}">${esc(r.fromCard)}</text>`);
  parts.push(`<text class="card" x="${secondX}" y="${secondY}">${esc(r.toCard)}</text>`);
  parts.push(`<text class="fk" x="${mx}" y="${my + 50}">${esc(r.fk)} • ${esc(r.onDelete)}</text>`);
}

// Entities and their key attributes.
for (const n of nodes.values()) {
  const cx = n.x + n.w / 2;
  const keyY = n.y + n.h + 68;
  const keyRx = Math.max(72, Math.min(190, n.pk.length * 6.4));
  parts.push(`<line class="attribute-line" x1="${cx}" y1="${n.y + n.h}" x2="${cx}" y2="${keyY - 27}"/>`);
  parts.push(`<rect class="${n.ref ? "reference" : "entity"}" x="${n.x}" y="${n.y}" width="${n.w}" height="${n.h}" rx="10"/>`);
  parts.push(`<text class="entity-name" x="${cx}" y="${n.y + n.h / 2}">${esc(n.label)}</text>`);
  if (n.ref) parts.push(`<text class="ref-label" x="${cx}" y="${n.y + n.h - 10}">reference copy</text>`);
  parts.push(`<ellipse class="attribute" cx="${cx}" cy="${keyY}" rx="${keyRx}" ry="28"/>`);
  parts.push(`<text class="attribute-text" x="${cx}" y="${keyY}">${esc(n.pk)} · PK</text>`);
  if (n.note) parts.push(`<text class="isolated" x="${cx}" y="${keyY + 52}">${esc(n.note)}</text>`);
}

// Notes / legend panel.
parts.push(`<text class="note-title" x="3485" y="2515">Reading the diagram</text>`);
parts.push(`<rect class="entity" x="3485" y="2550" width="190" height="58" rx="8"/><text class="entity-name" x="3580" y="2579">ENTITY</text>`);
parts.push(`<polygon class="relationship" points="3800,2550 3865,2579 3800,2608 3735,2579"/><text class="relationship-name" x="3800" y="2579">RELATION</text>`);
parts.push(`<ellipse class="attribute" cx="4075" cy="2579" rx="120" ry="28"/><text class="attribute-text" x="4075" y="2579">primary key</text>`);
parts.push(`<text class="note" x="3485" y="2670">Cardinality uses (minimum, maximum). For example, (0,N) means zero or many.</text>`);
parts.push(`<text class="note-title" x="3485" y="2735">Delete behavior</text>`);
parts.push(`<line x1="3485" y1="2780" x2="3605" y2="2780" stroke="#dc2626" stroke-width="6"/><text class="note" x="3630" y="2787">CASCADE</text>`);
parts.push(`<line x1="3900" y1="2780" x2="4020" y2="2780" stroke="#7c3aed" stroke-width="6"/><text class="note" x="4045" y="2787">SET NULL</text>`);
parts.push(`<line x1="4320" y1="2780" x2="4440" y2="2780" stroke="#ea580c" stroke-width="6"/><text class="note" x="4465" y="2787">RESTRICT</text>`);
parts.push(`<line x1="4750" y1="2780" x2="4870" y2="2780" stroke="#475569" stroke-width="6"/><text class="note" x="4895" y="2787">NO ACTION</text>`);
parts.push(`<text class="note-title" x="3485" y="2870">Important schema observations</text>`);
parts.push(`<text class="note" x="3485" y="2920">• MATERIAL_INDEX_JOBS has course_id and material_id columns, but schema.sql declares no FKs.</text>`);
parts.push(`<text class="note" x="3485" y="2965">• EVENT_PUBLICATION is intentionally standalone Spring Modulith infrastructure.</text>`);
parts.push(`<text class="note" x="3485" y="3010">• QUIZZES.student_id and teacher_id are nullable, so either creator relationship is optional.</text>`);
parts.push(`<text class="note" x="3485" y="3055">• QUESTIONS.quiz_id and MATERIALS.course_id are nullable in the current DDL.</text>`);
parts.push(`<text class="note" x="3485" y="3100">• A generation job’s quiz_id is nullable and not unique: the DDL permits many jobs per quiz.</text>`);
parts.push(`<text class="note" x="3485" y="3145">• No composite FK enforces that matrix rows, quizzes, chapters and outcomes share one course.</text>`);
parts.push(`<text class="note" x="3485" y="3210">Reference copies are dashed versions of the same USERS, COURSES, STUDENTS, TEACHERS,</text>`);
parts.push(`<text class="note" x="3485" y="3250">CHAPTERS, LEARNING_OUTCOMES and EXAM_MATRICES entities, repeated only to prevent</text>`);
parts.push(`<text class="note" x="3485" y="3290">cross-panel connector lines. They do not represent additional database tables.</text>`);
parts.push(`<text class="note-title" x="3485" y="3380">Coverage</text>`);
parts.push(`<text class="note" x="3485" y="3425">30 tables • ${relationships.length} foreign-key roles • PostgreSQL schema: edu</text>`);
parts.push(`<text class="note" x="3485" y="3470">Generated from the checked-in schema structure; indexes and non-key columns are omitted.</text>`);

parts.push(`</svg>`);

mkdirSync("docs/erd", { recursive: true });
writeFileSync(output, parts.join("\n"));
console.log(`Wrote ${output}: ${nodes.size} displayed entities/reference copies, ${relationships.length} relationships`);
