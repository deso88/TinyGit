package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.greater0
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority

fun button(action: Action) = button {
    text = action.text
    graphic = action.icon?.invoke()
    action.shortcut?.let { tooltip(KeyCombination.valueOf(it).displayText) }
    action.disabled?.let { disabledWhen(it) }
    setOnAction { action.handler() }
}

inline fun button(block: Button.() -> Unit): Button {
    val button = Button()
    block(button)
    return button
}

inline fun link(block: Hyperlink.() -> Unit): Hyperlink {
    val link = Hyperlink()
    block(link)
    return link
}

inline fun toolBar(block: ToolBarBuilder.() -> Unit): ToolBar {
    val toolBar = ToolBarBuilder()
    block(toolBar)
    return toolBar
}

class ToolBarBuilder : ToolBar() {

    fun addSpacer() {
        +Pane().hgrow(Priority.ALWAYS)
    }

    operator fun Node.unaryPlus() {
        items += this
    }

    operator fun Action.unaryPlus() {
        +button(this)
    }

    operator fun ActionGroup.unaryPlus() {
        if (items.isNotEmpty()) +Separator()
        action.forEach { action ->
            +stackPane {
                +button(action)
                action.count?.let {
                    +label {
                        addClass("count")
                        alignment(Pos.TOP_RIGHT)
                        visibleWhen(it.greater0()) // .run { action.disabled?.let { and(it.not()) } ?: this })
                        isMouseTransparent = true
                        textProperty().bind(it.asString())
                    }
                }
            }
        }
    }

}
