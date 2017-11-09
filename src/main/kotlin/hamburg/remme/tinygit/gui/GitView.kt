package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.PushRejectedException
import hamburg.remme.tinygit.gui.FontAwesome.CLOUD_DOWNLOAD
import hamburg.remme.tinygit.gui.FontAwesome.CLOUD_UPLOAD
import hamburg.remme.tinygit.gui.FontAwesome.CODE_FORK
import hamburg.remme.tinygit.gui.FontAwesome.COG
import hamburg.remme.tinygit.gui.FontAwesome.DATABASE
import hamburg.remme.tinygit.gui.FontAwesome.DOWNLOAD
import hamburg.remme.tinygit.gui.FontAwesome.PLUS
import hamburg.remme.tinygit.gui.FontAwesome.REFRESH
import hamburg.remme.tinygit.gui.FontAwesome.TAG
import hamburg.remme.tinygit.gui.FontAwesome.UNDO
import hamburg.remme.tinygit.gui.FontAwesome.UPLOAD
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Separator
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import java.io.File

class GitView : VBox() {

    private val repositories = State.getRepositories()
    private val localRepositories = RepositoryView(repositories)

    init {
        styleClass += "git-view"

        val addCopy = EventHandler<ActionEvent> { addCopy() }
        val commit = EventHandler<ActionEvent> { commit(State.getSelectedRepository()) }
        val push = EventHandler<ActionEvent> { push(State.getSelectedRepository(), false) }
        val pushForce = EventHandler<ActionEvent> { push(State.getSelectedRepository(), true) }
        val pull = EventHandler<ActionEvent> { pull(State.getSelectedRepository()) }
        val fetch = EventHandler<ActionEvent> { fetch(State.getSelectedRepository()) }
        val createBranch = EventHandler<ActionEvent> { createBranch(State.getSelectedRepository()) }
        val stash = EventHandler<ActionEvent> { stash(State.getSelectedRepository()) }
        val stashApply = EventHandler<ActionEvent> { stashApply(State.getSelectedRepository()) }

        //<editor-fold desc="MenuBar">
        val menuBar = MenuBar(
                Menu("File", null,
                        menuItem("Add Working Copy",
                                DATABASE(),
                                shortcut = "Shortcut+O",
                                action = addCopy),
                        SeparatorMenuItem(),
                        menuItem("Quit TinyGit",
                                action = EventHandler { Platform.exit() })),
                Menu("Repository", null,
                        menuItem("Settings",
                                COG(),
                                action = EventHandler { SettingsDialog(State.getSelectedRepository(), scene.window).show() })),
                Menu("Actions", null,
                        menuItem("Commit",
                                PLUS(),
                                shortcut = "Shortcut+Plus",
                                disable = State.canCommit.not(),
                                action = commit),
                        SeparatorMenuItem(),
                        menuItem("Push",
                                CLOUD_UPLOAD(),
                                shortcut = "Shortcut+P",
                                disable = State.canPush.not(),
                                action = push),
                        menuItem("Force Push",
                                CLOUD_UPLOAD(),
                                shortcut = "Shortcut+Shift+P",
                                disable = State.canPush.not(),
                                action = pushForce),
                        menuItem("Pull",
                                CLOUD_DOWNLOAD(),
                                disable = State.canPull.not(),
                                shortcut = "Shortcut+L",
                                action = pull),
                        menuItem("Fetch",
                                REFRESH(),
                                shortcut = "Shortcut+F",
                                disable = State.canFetch.not(),
                                action = fetch),
                        menuItem("Tag",
                                TAG(),
                                shortcut = "Shortcut+T",
                                disable = State.canTag.not(),
                                action = EventHandler { }),
                        SeparatorMenuItem(),
                        menuItem("Branch",
                                CODE_FORK(),
                                shortcut = "Shortcut+B",
                                disable = State.canBranch.not(),
                                action = createBranch),
                        menuItem("Merge",
                                CODE_FORK(),
                                shortcut = "Shortcut+M",
                                disable = State.canMerge.not(),
                                action = EventHandler { }),
                        SeparatorMenuItem(),
                        menuItem("Stash",
                                DOWNLOAD(),
                                shortcut = "Shortcut+S",
                                disable = State.canStash.not(),
                                action = stash),
                        menuItem("Apply Stash",
                                UPLOAD(),
                                shortcut = "Shortcut+Shift+S",
                                disable = State.canApplyStash.not(),
                                action = stashApply),
                        SeparatorMenuItem(),
                        menuItem("Reset",
                                UNDO(),
                                shortcut = "Shortcut+R",
                                disable = State.canReset.not(),
                                action = EventHandler { })),
                Menu("?", null,
                        menuItem("About TinyGit",
                                action = EventHandler { })))
        menuBar.isUseSystemMenuBar = true
        //</editor-fold>

        //<editor-fold desc="ToolBar">
        val toolBar = ToolBar(
                button("Add Working Copy",
                        DATABASE(),
                        addCopy),
                Separator(),
                button("Commit",
                        PLUS(),
                        disable = State.canCommit.not(),
                        action = commit),
                Separator(),
                button("Push",
                        CLOUD_UPLOAD(),
                        disable = State.canPush.not(),
                        action = push),
                button("Pull",
                        CLOUD_DOWNLOAD(),
                        disable = State.canPull.not(),
                        action = pull),
                button("Fetch",
                        REFRESH(),
                        disable = State.canFetch.not(),
                        action = fetch),
                button("Tag",
                        TAG(),
                        disable = State.canTag.not(),
                        action = EventHandler { }),
                Separator(),
                button("Branch",
                        CODE_FORK(),
                        disable = State.canBranch.not(),
                        action = createBranch),
                button("Merge",
                        CODE_FORK().also { it.scaleY = -1.0 },
                        disable = State.canMerge.not(),
                        action = EventHandler { }),
                Separator(),
                button("Stash",
                        DOWNLOAD(),
                        disable = State.canStash.not(),
                        action = stash),
                button("Apply Stash",
                        UPLOAD(),
                        disable = State.canApplyStash.not(),
                        action = stashApply),
                Separator(),
                button("Reset",
                        UNDO(),
                        disable = State.canReset.not(),
                        action = EventHandler { }))
        //</editor-fold>

        val tabs = TabPane(LogView(), WorkingCopyView())
        val content = SplitPane(localRepositories, tabs)
        Platform.runLater { content.setDividerPosition(0, 0.20) }

        val info = StackPane(HBox(
                Label("Click "),
                Label("", DATABASE()),
                Label(" to add a working copy."))
                .also { it.styleClass += "box" })
        info.styleClass += "overlay"
        info.visibleProperty().bind(State.showGlobalInfo)

        val overlay = StackPane(
                ProgressIndicator(-1.0),
                Label().also { it.textProperty().bind(State.processTextProperty()) })
        overlay.styleClass += "progress-overlay"
        overlay.visibleProperty().bind(State.showGlobalOverlay)

        children.addAll(
                menuBar,
                toolBar,
                StackPane(content, info, overlay).also { VBox.setVgrow(it, Priority.ALWAYS) })
    }

    private fun addCopy() {
        val chooser = DirectoryChooser()
        chooser.title = "Add Working Copy"
        chooser.showDialog(this.scene.window)?.let {
            if (File("${it.absolutePath}/.git").exists()) {
                val repository = LocalRepository(it.absolutePath)
                if (State.getRepositories().none { it.path == repository.path }) {
                    State.addRepository(LocalRepository(it.absolutePath))
                    localRepositories.selectionModel.selectLast()
                }
            } else {
                errorAlert(scene.window,
                        "Invalid Working Copy",
                        "${it.absolutePath}\ndoes not contain a valid '.git' directory.")
            }
        }
    }

    private fun commit(repository: LocalRepository) {
        CommitDialog(repository, scene.window).show()
    }

    private fun fetch(repository: LocalRepository) {
        State.addProcess("Fetching...")
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() = LocalGit.fetchPrune(repository)

            override fun succeeded() {
                State.removeProcess()
                State.fireRefresh()
            }

            override fun failed() {
                State.removeProcess()
                exception.printStackTrace()
            }
        })
    }

    private fun pull(repository: LocalRepository) {
        State.addProcess("Pulling commits...")
        State.cachedThreadPool.execute(object : Task<Boolean>() {
            override fun call() = LocalGit.pull(repository)

            override fun succeeded() {
                State.removeProcess()
                if (value) State.fireRefresh()
            }

            override fun failed() {
                State.removeProcess()
                exception.printStackTrace()
                // TODO: make more specific to exception
                errorAlert(scene.window,
                        "Cannot Pull From Remote Branch",
                        "${exception.message}\n\nPlease commit or stash them before pulling.")
            }
        })
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        if (force && !confirmWarningAlert(scene.window,
                "Force Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) return

        State.addProcess("Pushing commits...")
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() {
                if (force) LocalGit.pushForce(repository)
                LocalGit.push(repository)
            }

            override fun succeeded() {
                State.removeProcess()
                State.fireRefresh()
            }

            override fun failed() {
                State.removeProcess()
                when (exception) {
                    is PushRejectedException -> errorAlert(scene.window,
                            "Cannot Push to Remote Branch",
                            "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun createBranch(repository: LocalRepository) {
        textInputDialog(scene.window, CODE_FORK())?.let { name ->
            State.addProcess("Branching...")
            State.cachedThreadPool.execute(object : Task<Unit>() {
                override fun call() {
                    LocalGit.branchCreate(repository, name)
                }

                override fun succeeded() {
                    State.removeProcess()
                    State.fireRefresh()
                }

                override fun failed() {
                    State.removeProcess()
                    when (exception) {
                        is RefAlreadyExistsException -> errorAlert(scene.window,
                                "Cannot Create Branch",
                                "Branch '$name' does already exist in the working copy.")
                        else -> exception.printStackTrace()
                    }
                }
            })
        }
    }

    private fun stash(repository: LocalRepository) {
        State.addProcess("Stashing files...")
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() = LocalGit.stash(repository)

            override fun succeeded() {
                State.removeProcess()
                State.fireRefresh()
            }

            override fun failed() {
                State.removeProcess()
                exception.printStackTrace()
            }
        })
    }

    private fun stashApply(repository: LocalRepository) {
        State.addProcess("Applying stash...")
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() = LocalGit.stashPop(repository)

            override fun succeeded() {
                State.removeProcess()
                State.fireRefresh()
            }

            override fun failed() {
                State.removeProcess()
                exception.printStackTrace()
            }
        })
    }

}
