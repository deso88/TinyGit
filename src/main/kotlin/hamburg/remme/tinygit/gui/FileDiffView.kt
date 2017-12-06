package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.webView
import hamburg.remme.tinygit.htmlEncodeAll
import javafx.scene.web.WebEngine

class FileDiffView : StackPaneBuilder() {

    private val fileDiff: WebEngine
    //language=HTML
    private val emptyDiff = """
        <tr>
            <td class="line-number header">&nbsp;</td>
            <td class="line-number header">&nbsp;</td>
            <td class="code header">&nbsp;@@ No changes detected or binary file @@</td>
        </tr>
    """

    init {
        val webView = webView {
            isContextMenuEnabled = false
            prefWidth = 400.0
            prefHeight = 300.0
        }
        fileDiff = webView.engine
        +webView
        clearContent()
    }

    fun update(repository: LocalRepository, file: LocalFile) {
        setContent(Git.diff(repository, file), repository.resolve(file))
    }

    fun update(repository: LocalRepository, file: LocalFile, commit: LocalCommit) {
        setContent(Git.diff(repository, file, commit))
    }

    fun clear() {
        clearContent()
    }

    private fun clearContent() {
        //language=HTML
        fileDiff.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        background-color: #3c3f41;
                    }
                </style>
            </head>
            </html>
        """)
    }

    private fun setContent(diff: String, file: String? = null) {
        //language=HTML
        fileDiff.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        font: 12px "Liberation Mono", monospace;
                        color: #ccc;
                        background-color: #3c3f41;
                    }
                    hr {
                        height: 1px;
                        background-color: #aaa;
                        border: none;
                    }
                    table {
                        position: absolute;
                        min-width: 100%;
                        font-size: 13px;
                    }
                    .image-box {
                        padding: 20px;
                    }
                    .image-box img {
                        width: 100%;
                        box-shadow: 0 2px 10px 2px rgba(0, 0, 0, 0.5);
                    }
                    .line-number {
                        padding: 3px 6px;
                        text-align: right;
                        color: rgba(255,255,255,0.6);
                        background-color: #535759;
                    }
                    .line-number.header {
                        padding: 6px 0;
                        background-color: #4e6e80;
                    }
                    .line-number.added {
                        background-color: #4e8054;
                    }
                    .line-number.removed {
                        background-color: #804e4e;
                    }
                    .code {
                        width: 100%;
                        white-space: nowrap;
                    }
                    .code.header {
                        color: #aaa;
                        background-color: #354b57;
                    }
                    .code.added {
                        background-color: #36593b;
                    }
                    .code.removed {
                        background-color: #593636;
                    }
                    .code.eof {
                        color: rgba(255,255,255,0.6);
                    }
                    .marker {
                        -webkit-user-select: none;
                        user-select: none;
                        margin-left: 4px;
                        padding: 0 2px;
                        color: rgba(255,255,255,0.45);
                        background-color: rgba(255,255,255,0.15);
                        border-radius: 2px;
                        font-size: 11px;
                    }
                </style>
            </head>
            <body>
                <table cellpadding="0" cellspacing="0">
                    ${format(diff, file)}
                </table>
            </body>
            </html>
        """)
    }

    private fun format(diff: String, file: String? = null): String {
        if (diff.isBlank() || diff.matches(".*Binary files differ\\r?\\n?$".toRegex(RegexOption.DOT_MATCHES_ALL))) {
            //language=HTML
            val image = if (file?.toLowerCase()?.matches(".*\\.(png|jpe?g|gif)$".toRegex()) == true) """
                <tr>
                    <td colspan="3"><div class="image-box"><img src="file://$file"></div></td>
                </tr>
            """ else ""
            //language=HTML
            return """
                $emptyDiff
                $image
            """
        }
        val blocks = mutableListOf<DiffBlock>()
        var blockNumber = -1
        val numbers = arrayOf(0, 0)
        return diff.split("\\r?\\n".toRegex())
                .dropLast(1)
                .dropWhile { !it.isBlockHeader() }
                .onEach { if (it.isBlockHeader()) blocks += it.parseBlockHeader() }
                .map { it.htmlEncodeAll() }
                .map {
                    if (it.isBlockHeader()) {
                        blockNumber++
                        numbers[0] = blocks[blockNumber].number1
                        numbers[1] = blocks[blockNumber].number2
                    }
                    formatLine(it, numbers, blocks[blockNumber])
                }
                .joinToString("")
                .takeIf { it.isNotBlank() } ?: emptyDiff
    }

    private fun formatLine(line: String, numbers: Array<Int>, block: DiffBlock): String {
        if (line.isBlockHeader()) {
            //language=HTML
            return """
                <tr>
                    <td class="line-number header">&nbsp;</td>
                    <td class="line-number header">&nbsp;</td>
                    <td class="code header">&nbsp;@@ -${block.number1},${block.length1} +${block.number2},${block.length2} @@</td>
                </tr>
            """
        }
        val codeClass: String
        val oldLineNumber: String
        val newLineNumber: String
        when {
            line.startsWith('+') -> {
                newLineNumber = numbers[1]++.toString()
                oldLineNumber = "&nbsp;"
                codeClass = "added"
            }
            line.startsWith('-') -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = numbers[0]++.toString()
                codeClass = "removed"
            }
            line.startsWith('\\') -> {
                newLineNumber = "&nbsp;"
                oldLineNumber = "&nbsp;"
                codeClass = "eof"
            }
            else -> {
                oldLineNumber = numbers[0]++.toString()
                newLineNumber = numbers[1]++.toString()
                codeClass = "&nbsp;"
            }
        }
        //language=HTML
        return """
            <tr>
                <td class="line-number $codeClass">$oldLineNumber</td>
                <td class="line-number $codeClass">$newLineNumber</td>
                <td class="code $codeClass">$line</td>
            </tr>
        """
    }

    private fun String.isBlockHeader() = startsWith("@@")

    private fun String.parseBlockHeader(): DiffBlock {
        val match = ".*?(\\d+)(,\\d+)?.*?(\\d+)(,\\d+)?.*".toRegex().matchEntire(this)!!.groups
        return DiffBlock(
                match[1]?.value?.toInt() ?: 1,
                match[2]?.value?.substring(1)?.toInt() ?: 1,
                match[3]?.value?.toInt() ?: 1,
                match[4]?.value?.substring(1)?.toInt() ?: 1)
    }

    private class DiffBlock(val number1: Int, val length1: Int, val number2: Int, val length2: Int)

}
