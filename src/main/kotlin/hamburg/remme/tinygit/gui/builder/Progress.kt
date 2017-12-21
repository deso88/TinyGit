package hamburg.remme.tinygit.gui.builder

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.gui.component.Icons
import javafx.animation.Interpolator
import javafx.animation.Transition
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ProgressBar
import javafx.scene.layout.StackPane
import javafx.util.Duration

private val spinAnimationStep = 8.0

inline fun progressBar(block: ProgressBar.() -> Unit): ProgressBar {
    val bar = ProgressBar(-1.0)
    block.invoke(bar)
    return bar
}

inline fun progressSpinner(block: Node.() -> Unit): Node {
    val indicator = Icons.spinner()
    block.invoke(indicator)
    SpinAnimation(indicator, 2.0).play()
    return indicator
}

inline fun progressPane(block: ProgressPaneBuilder.() -> Unit): ProgressPaneBuilder {
    val pane = ProgressPaneBuilder()
    block.invoke(pane)
    return pane
}

class SpinAnimation(private val node: Node, rate: Double = 1.0) : Transition(spinAnimationStep / rate) {

    init {
        cycleCount = -1
        cycleDuration = Duration(spinAnimationStep * rate * 1000.0)
        interpolator = Interpolator.DISCRETE
    }

    override fun interpolate(frac: Double) {
        node.rotate += 360.0 / spinAnimationStep
    }

}

class ProgressPaneBuilder : StackPane() {

    private val progress = progressBar {
        addClass("progress-pane-bar")
        alignment(Pos.TOP_CENTER)
        maxWidth = Double.MAX_VALUE
        isVisible = false
    }

    init {
        children.add(progress)
    }

    operator fun Node.unaryPlus() {
        children.add(children.size - 1, this)
    }

    fun execute(task: Task<*>) {
        task.setOnSucceeded { hideProgress() }
        task.setOnCancelled { hideProgress() }
        task.setOnFailed { hideProgress() }
        showProgress()
        State.execute(task)
    }

    private fun showProgress() {
        progress.isVisible = true
    }

    private fun hideProgress() {
        progress.isVisible = false
    }

}
