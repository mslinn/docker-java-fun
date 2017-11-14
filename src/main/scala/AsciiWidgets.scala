import de.vandermeer.asciitable.{AT_Context, AsciiTable}
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

object AsciiWidgets {
  def asciiTable(title: String, rows: List[String]*): String = {
    assert(rows.nonEmpty)
    val table = new AsciiTable(new AT_Context().setWidth(120))
    table.addRule()
    rows foreach { row =>
      table.addRow(row:_*)
      table.addRule()
    }
    table.setTextAlignment(TextAlignment.LEFT)
    s"\n$title\n" + table.render
  }
}
