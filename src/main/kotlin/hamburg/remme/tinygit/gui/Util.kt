package hamburg.remme.tinygit.gui

import javafx.beans.binding.BooleanBinding
import javafx.beans.value.ObservableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.control.TextInputDialog
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Window
import javafx.util.Callback
import javafx.util.converter.IntegerStringConverter
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.fontawesome.FontAwesome
import org.kordamp.ikonli.javafx.FontIcon
import java.time.format.DateTimeFormatter

val shortDate = DateTimeFormatter.ofPattern("d. MMM yyyy HH:mm")!!
val fullDate = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss")!!

fun label(text: String = "",
          icon: Node? = null,
          color: String? = null,
          tooltip: String? = null): Label {
    val label = Label(text)
    icon?.let { label.graphic = it }
    color?.let { label.style = "-fx-text-fill:$it;" }
    tooltip?.let { label.tooltip = Tooltip(it) }
    return label
}

fun icon(ikon: Ikon, color: String = "#fff"): FontIcon {
    val icon = FontIcon(ikon)
    icon.iconColor = Color.web(color)
    return icon
}

fun button(label: String = "",
           icon: Node? = null,
           action: EventHandler<ActionEvent>,
           tooltip: String? = null,
           disable: BooleanBinding? = null,
           vararg styleClass: String): Button {
    val button = Button(label)
    button.onAction = action
    button.styleClass += styleClass
    icon?.let { button.graphic = it }
    tooltip?.let { button.tooltip = Tooltip(it) }
    disable?.let { button.disableProperty().bind(it) }
    return button
}

fun <S, T> tableColumn(title: String,
                       sortable: Boolean = false,
                       cellValue: Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>>,
                       cellFactory: Callback<TableColumn<S, T>, TableCell<S, T>>? = null): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = cellValue
    column.isSortable = sortable
    cellFactory?.let { column.cellFactory = it }
    return column
}

fun textField(value: String = "",
              editable: Boolean = true): TextField {
    val textField = TextField(value)
    textField.isEditable = editable
    return textField
}

fun intTextField(value: Int = 0,
                 editable: Boolean = true): TextField {
    val textField = textField(value.toString(), editable)
    textField.textFormatter = TextFormatter<Int>(IntegerStringConverter(), value)
    return textField
}

fun textArea(value: String = "",
             placeholder: String = "",
             editable: Boolean = true): TextArea {
    val textArea = TextArea(value)
    textArea.promptText = placeholder
    textArea.isEditable = editable
    textArea.isWrapText = true
    return textArea
}

fun spacer() = Pane().also { HBox.setHgrow(it, Priority.ALWAYS) }

fun menuItem(label: String,
             icon: Node? = null,
             shortcut: String? = null,
             action: EventHandler<ActionEvent>,
             disable: BooleanBinding? = null): MenuItem {
    val menuItem = MenuItem(label)
    menuItem.onAction = action
    shortcut?.let { menuItem.accelerator = KeyCombination.valueOf(it) }
    icon?.let { menuItem.graphic = it }
    disable?.let { menuItem.disableProperty().bind(it) }
    return menuItem
}

fun confirmAlert(window: Window,
                 header: String,
                 text: String): Boolean {
    val alert = alert(window, Alert.AlertType.CONFIRMATION, header, text, icon(FontAwesome.QUESTION_CIRCLE, "#5bc0de"))
    return alert.showAndWait().get() == ButtonType.OK
}

fun confirmWarningAlert(window: Window,
                        header: String,
                        text: String): Boolean {
    val alert = alert(window, Alert.AlertType.CONFIRMATION, header, text, icon(FontAwesome.EXCLAMATION_TRIANGLE, "#f0ad4e"))
    return alert.showAndWait().get() == ButtonType.OK
}

fun errorAlert(window: Window,
               header: String,
               text: String) {
    val alert = alert(window, Alert.AlertType.ERROR, header, text, icon(FontAwesome.EXCLAMATION_TRIANGLE, "#d9534f"))
    alert.showAndWait()
}

private fun alert(window: Window,
                  type: Alert.AlertType,
                  header: String,
                  text: String,
                  icon: Node): Alert {
    val alert = Alert(type, text)
    alert.initModality(Modality.WINDOW_MODAL)
    alert.initOwner(window)
    alert.headerText = header
    alert.graphic = icon
    return alert
}

fun textInputDialog(window: Window,
                    icon: Node? = null): String? {
    val dialog = TextInputDialog()
    dialog.initModality(Modality.WINDOW_MODAL)
    dialog.initOwner(window)
    dialog.title = "Input"
    dialog.headerText = "Enter a New Branch Name"
    dialog.graphic = icon
    return dialog.showAndWait().get()
}