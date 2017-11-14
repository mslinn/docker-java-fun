import de.vandermeer.asciitable.AsciiTable

object AsciiWidgets {
  def asciiTable(title: String, contents: List[String]*): String = {
    assert(contents.nonEmpty)
    val table = new AsciiTable
    val rows = contents.length
    table.addRule()
    0 until rows foreach { i =>
      val rowContents: List[String] = contents(i)
      table.addRow(rowContents:_*)
    }
    table.addRule()
    s"\n$title\n" + table.render
  }
}
