# -*- coding: utf-8 -*-
"""Convert thesis markdown to a Word report (.docx)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


INLINE_RE = re.compile(
    r"(\*\*[^*]+\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\))"
)


def set_run_font(run, name="Times New Roman", size=13, bold=None, italic=None, code=False):
    run.font.name = "Courier New" if code else name
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman" if not code else "Courier New")
    run.font.size = Pt(11 if code else size)
    if bold is not None:
        run.bold = bold
    if italic is not None:
        run.italic = italic
    if code:
        run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)


def add_inline(paragraph, text, base_size=13):
    if not text:
        return
    pos = 0
    for m in INLINE_RE.finditer(text):
        if m.start() > pos:
            run = paragraph.add_run(text[pos : m.start()])
            set_run_font(run, size=base_size)
        token = m.group(0)
        if token.startswith("**") and token.endswith("**"):
            run = paragraph.add_run(token[2:-2])
            set_run_font(run, size=base_size, bold=True)
        elif token.startswith("`") and token.endswith("`"):
            run = paragraph.add_run(token[1:-1])
            set_run_font(run, size=base_size, code=True)
        elif token.startswith("["):
            label = re.match(r"\[([^\]]+)\]", token).group(1)
            run = paragraph.add_run(label)
            set_run_font(run, size=base_size)
        pos = m.end()
    if pos < len(text):
        run = paragraph.add_run(text[pos:])
        set_run_font(run, size=base_size)


def style_paragraph(p, space_after=6, first_line=False, align=None):
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(space_after)
    pf.line_spacing_rule = WD_LINE_SPACING.ONE_POINT_FIVE
    if first_line:
        pf.first_line_indent = Cm(1)
    if align is not None:
        p.alignment = align


def add_heading(doc, text, level):
    # Strip markdown heading markers already done by caller
    p = doc.add_paragraph()
    run = p.add_run(text.strip())
    if level == 0:
        set_run_font(run, size=16, bold=True)
        style_paragraph(p, space_after=10, align=WD_ALIGN_PARAGRAPH.CENTER)
    elif level == 1:
        set_run_font(run, size=14, bold=True)
        style_paragraph(p, space_after=10, align=WD_ALIGN_PARAGRAPH.CENTER)
        p.paragraph_format.space_before = Pt(18)
    elif level == 2:
        set_run_font(run, size=13, bold=True)
        style_paragraph(p, space_after=8)
        p.paragraph_format.space_before = Pt(12)
    else:
        set_run_font(run, size=13, bold=True)
        style_paragraph(p, space_after=6)
        p.paragraph_format.space_before = Pt(8)
    return p


def add_body(doc, text, first_line=True):
    p = doc.add_paragraph()
    add_inline(p, text.strip())
    style_paragraph(p, first_line=first_line and not text.strip().startswith("**"))
    return p


def add_list_item(doc, text, ordered=False, number=None):
    p = doc.add_paragraph()
    prefix = f"{number}. " if ordered else "• "
    run = p.add_run(prefix)
    set_run_font(run, size=13)
    add_inline(p, text.strip())
    style_paragraph(p, space_after=4)
    p.paragraph_format.left_indent = Cm(0.75)
    return p


def parse_table(lines):
    rows = []
    for line in lines:
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if all(re.fullmatch(r":?-{3,}:?", c.replace(" ", "")) for c in cells):
            continue
        rows.append(cells)
    return rows


def add_table(doc, rows):
    if not rows:
        return
    cols = max(len(r) for r in rows)
    table = doc.add_table(rows=len(rows), cols=cols)
    table.style = "Table Grid"
    for i, row in enumerate(rows):
        for j in range(cols):
            cell = table.cell(i, j)
            cell.text = ""
            p = cell.paragraphs[0]
            text = row[j] if j < len(row) else ""
            add_inline(p, text, base_size=11)
            for run in p.runs:
                if i == 0:
                    run.bold = True
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.space_before = Pt(2)
    doc.add_paragraph()


def add_code_block(doc, lines, lang=""):
    if lang.lower() == "mermaid":
        p = doc.add_paragraph()
        run = p.add_run("[Sơ đồ Mermaid — xem bản Markdown hoặc vẽ lại trong Word]")
        set_run_font(run, size=12, italic=True)
        style_paragraph(p, space_after=4)
    p = doc.add_paragraph()
    run = p.add_run("\n".join(lines))
    set_run_font(run, size=10, code=True)
    style_paragraph(p, space_after=8)
    p.paragraph_format.left_indent = Cm(0.5)
    p.paragraph_format.line_spacing = 1.0


def convert(md_path: Path, docx_path: Path):
    lines = md_path.read_text(encoding="utf-8").splitlines()
    doc = Document()

    section = doc.sections[0]
    section.top_margin = Cm(2)
    section.bottom_margin = Cm(2)
    section.left_margin = Cm(3)
    section.right_margin = Cm(2)

    style = doc.styles["Normal"]
    style.font.name = "Times New Roman"
    style.font.size = Pt(13)
    style._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman")

    i = 0
    ordered_num = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if not stripped:
            i += 1
            ordered_num = 0
            continue

        if stripped == "---":
            i += 1
            continue

        if stripped.startswith("```"):
            lang = stripped[3:].strip()
            i += 1
            block = []
            while i < len(lines) and not lines[i].strip().startswith("```"):
                block.append(lines[i])
                i += 1
            if i < len(lines):
                i += 1
            add_code_block(doc, block, lang)
            continue

        if stripped.startswith("|"):
            table_lines = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_lines.append(lines[i])
                i += 1
            add_table(doc, parse_table(table_lines))
            continue

        m = re.match(r"^(#{1,4})\s+(.*)$", stripped)
        if m:
            level = len(m.group(1))
            # Map: # -> chapter/title (1), ## -> section (2), etc.
            # First school titles use # / ## — treat level 1 as centered title style when short school header
            add_heading(doc, m.group(2), level)
            ordered_num = 0
            i += 1
            continue

        m = re.match(r"^(\d+)\.\s+(.*)$", stripped)
        if m:
            ordered_num = int(m.group(1))
            add_list_item(doc, m.group(2), ordered=True, number=ordered_num)
            i += 1
            continue

        if stripped.startswith("- "):
            add_list_item(doc, stripped[2:], ordered=False)
            i += 1
            continue

        # Continuations / plain paragraphs
        add_body(doc, stripped, first_line=True)
        i += 1

    docx_path.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(docx_path))
    print(f"Created: {docx_path}")


if __name__ == "__main__":
    base = Path(__file__).resolve().parent
    md = base / "DOC-SYS-01-bao-cao-do-an-chatbot-ai.md"
    out = base / "DOC-SYS-01-bao-cao-do-an-chatbot-ai.docx"
    if len(sys.argv) >= 2:
        md = Path(sys.argv[1])
    if len(sys.argv) >= 3:
        out = Path(sys.argv[2])
    convert(md, out)
