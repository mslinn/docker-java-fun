import de.vandermeer.asciitable.{AT_Context, AsciiTable}

object AsciiWidgets {
  def asciiTable(title: String, rows: List[String]*): String = {
    assert(rows.nonEmpty)
    val table = new AsciiTable(new AT_Context().setWidth(120))
    table.addRule()
    rows foreach { row =>
      table.addRow(row:_*)
    }
    table.addRule()
    s"\n$title\n" + table.render
  }
}
