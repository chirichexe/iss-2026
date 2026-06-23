// ════════════════════════════════════════════════════════════════════════════
//  template.typ
//  Dipendenza: @preview/ilm:1.4.1  (spacing e tipografia LaTeX-style)
//
//  Esporta: iss-template, iss-table, nota, domanda
// ════════════════════════════════════════════════════════════════════════════

#import "@preview/ilm:1.4.1": ilm

// ── Utilità ──────────────────────────────────────────────────────────────────

#let iss-table(..args) = table(
  stroke: 0.5pt + luma(180),
  fill: (_, row) =>
    if row == 0    { luma(220) }
    else if calc.odd(row) { luma(247) }
    else           { white },
  ..args,
)

#let nota(body) = block(
  width: 100%,
  fill: luma(240),
  stroke: (left: 2pt + luma(150)),
  inset: (left: 10pt, right: 6pt, top: 5pt, bottom: 5pt),
)[#text(style: "italic")[Nota:] #h(4pt) #body]

#let domanda(body) = block(
  width: 100%,
  fill: luma(245),
  stroke: (left: 2pt + luma(80)),
  inset: (left: 10pt, right: 6pt, top: 5pt, bottom: 5pt),
)[#text(weight: "semibold")[Domanda al committente.] \ #body]

// ── Template ─────────────────────────────────────────────────────────────────

#let iss-template(
  title:         "Titolo",
  subtitle:      "Sprint 0",
  course:        "Ingegneria dei Sistemi Software",
  university:    "Alma Mater Studiorum · Università di Bologna",
  academic-year: "2025/2026",
  authors:       (),
  body,
) = {

  // ilm gestisce: margini, spaziatura, ritmo tipografico, header, footer
  show: ilm.with(
    title:             [#title: #subtitle],
    author:            authors.join(", "),
    date:              datetime.today(),
    abstract:          [#course, A.A. #academic-year \ #university],
    table-of-contents: none,
    chapter-pagebreak: false,
    bibliography:      none,
    paper-size:        "a4",
  )

  // Font LaTeX-style (New Computer Modern è built-in in Typst)
  set text(font: "New Computer Modern")

  // Numbering sezioni
  set heading(numbering: "1.")

  // Font e stile blocchi di codice
  show raw: set text(
    font: ("JetBrains Mono", "Fira Code", "Source Code Pro", "DejaVu Sans Mono"),
    size: 9pt,
  )
  show raw.where(block: true): it => block(
    width: 100%,
    fill: luma(248),
    stroke: 0.5pt + luma(205),
    inset: (x: 12pt, y: 9pt),
    radius: 2pt,
  )[#it]
  show raw.where(block: false): it => box(
    fill: luma(238),
    inset: (x: 3pt, y: 1pt),
    radius: 1pt,
  )[#it]

  // Link azzurri
  show link: it => text(fill: rgb("#0077CC"))[#it]

  body
}
