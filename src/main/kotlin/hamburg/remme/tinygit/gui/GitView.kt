package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.git.api.PushRejectedException
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.StashApplyFailureException
import java.io.File

class GitView(application: Application) : VBox() {

    private val repositoryView = RepositoryView()
    private val commitLog = CommitLogView()
    private val workingCopy = WorkingCopyView()
    private val tabs = TabPane(commitLog, workingCopy)

    init {
        addClass("git-view")

        // File
        val addCopy = Action("Add Repository", { FontAwesome.database() }, "Shortcut+O",
                action = EventHandler { addRepo() })
        val quit = Action("Quit TinyGit",
                action = EventHandler { Platform.exit() })
        // View
        val showCommits = Action("Show Commits", shortcut = "F1",
                action = EventHandler { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action("Show Working Copy", shortcut = "F2",
                action = EventHandler { tabs.selectionModel.select(workingCopy) })
        // Repository
        val commit = Action("Commit", { FontAwesome.plus() }, "Shortcut+Plus", State.canCommit.not(),
                EventHandler { commit(State.selectedRepository) })
        val push = Action("Push", { FontAwesome.cloudUpload() }, "Shortcut+P", State.canPush.not(),
                EventHandler { push(State.selectedRepository, false) }, State.aheadProperty())
        val pushForce = Action("Force Push", { FontAwesome.cloudUpload() }, "Shortcut+Shift+P", State.canPush.not(),
                EventHandler { push(State.selectedRepository, true) })
        val pull = Action("Pull", { FontAwesome.cloudDownload() }, "Shortcut+L", State.canPull.not(),
                EventHandler { pull(State.selectedRepository) }, State.behindProperty())
        val fetch = Action("Fetch", { FontAwesome.refresh() }, "Shortcut+F", State.canFetch.not(),
                EventHandler { fetch(State.selectedRepository) })
        val tag = Action("Tag", { FontAwesome.tag() }, "Shortcut+T", State.canTag.not(),
                action = EventHandler { /* TODO */ })
        val branch = Action("Branch", { FontAwesome.codeFork() }, "Shortcut+B", State.canBranch.not(),
                EventHandler { createBranch(State.selectedRepository) })
        val merge = Action("Merge", { FontAwesome.codeFork().flipY() }, "Shortcut+M", State.canMerge.not(),
                action = EventHandler { /* TODO */ })
        val stash = Action("Stash", { FontAwesome.cube() }, "Shortcut+S", State.canStash.not(),
                EventHandler { stash(State.selectedRepository) })
        val stashPop = Action("Pop Stash", { FontAwesome.cube().flipXY() }, "Shortcut+Shift+S", State.canApplyStash.not(),
                EventHandler { stashPop(State.selectedRepository) })
        val reset = Action("Auto Reset", { FontAwesome.undo() }, disable = State.canReset.not(),
                action = EventHandler { /* TODO */ })
        val squash = Action("Auto Squash", { FontAwesome.gavel() }, disable = State.canSquash.not(),
                action = EventHandler { /* TODO */ })
        val settings = Action("Settings", { FontAwesome.cog() }, disable = State.canSettings.not(),
                action = EventHandler { SettingsDialog(State.selectedRepository, scene.window).show() })
        // ?
        val github = Action("Star TinyGit on GitHub", { FontAwesome.githubAlt() },
                action = EventHandler { application.hostServices.showDocument("https://github.com/deso88/TinyGit") })
        val about = Action("About",
                action = EventHandler { /*AboutDialog(scene.window).show() TODO */ })

        val info = StackPane(HBox(
                Text("Click "),
                FontAwesome.database(),
                Text(" to add a repository."))
                .addClass("box"))
                .addClass("overlay")
        info.visibleProperty().bind(State.showGlobalInfo)

        val overlay = StackPane(
                ProgressIndicator(-1.0),
                Label().also { it.textProperty().bind(State.processTextProperty()) })
                .addClass("progress-overlay")
        overlay.visibleProperty().bind(State.showGlobalOverlay)

        val content = SplitPane(repositoryView, tabs)
        Platform.runLater { content.setDividerPosition(0, 0.20) }

        children.addAll(
                menuBar(ActionCollection("File", ActionGroup(addCopy), ActionGroup(quit)),
                        ActionCollection("View", ActionGroup(showCommits, showWorkingCopy)),
                        ActionCollection("Repository",
                                ActionGroup(commit),
                                ActionGroup(push, pushForce, pull, fetch, tag),
                                ActionGroup(branch, merge),
                                ActionGroup(stash, stashPop),
                                ActionGroup(reset, squash),
                                ActionGroup(settings)),
                        ActionCollection("Actions", *workingCopy.actions),
                        ActionCollection("?", ActionGroup(github, about))),
                toolBar(ActionGroup(addCopy),
                        ActionGroup(commit, push, pull, fetch, tag),
                        ActionGroup(branch, merge),
                        ActionGroup(stash, stashPop),
                        ActionGroup(reset, squash)),
                StackPane(content, info, overlay).also { VBox.setVgrow(it, Priority.ALWAYS) })
    }

    private fun addRepo() {
        directoryChooser(scene.window, "Add Repository")?.let {
            if (File("${it.absolutePath}/.git").exists()) {
                val repository = LocalRepository(it.absolutePath)
                if (State.repositories.none { it.path == repository.path }) {
                    State.repositories += LocalRepository(it.absolutePath)
                }
            } else {
                errorAlert(scene.window,
                        "Invalid Repository",
                        "${it.absolutePath}\ndoes not contain a valid '.git' directory.")
            }
        }
    }

    private fun commit(repository: LocalRepository) {
        CommitDialog(repository, scene.window).show()
    }

    private fun fetch(repository: LocalRepository) {
        State.addProcess("Fetching...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.fetchPrune(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun pull(repository: LocalRepository) {
        State.addProcess("Pulling commits...")
        State.execute(object : Task<Boolean>() {
            override fun call() = Git.pull(repository)

            override fun succeeded() {
                if (value) State.fireRefresh()
            }

            override fun failed() {
                exception.printStackTrace()
                // TODO: make more specific to exception
                errorAlert(scene.window,
                        "Cannot Pull From Remote Branch",
                        "${exception.message}\n\nPlease commit or stash them before pulling.")
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        if (force && !confirmWarningAlert(scene.window,
                "Force Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) return

        State.addProcess("Pushing commits...")
        State.execute(object : Task<Unit>() {
            override fun call() {
                if (force) Git.pushForce(repository)
                else Git.push(repository)
            }

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is PushRejectedException -> errorAlert(scene.window,
                            "Cannot Push to Remote Branch",
                            "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.")
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun createBranch(repository: LocalRepository) {
        textInputDialog(scene.window, FontAwesome.codeFork())?.let { name ->
            State.addProcess("Branching...")
            State.execute(object : Task<Unit>() {
                override fun call() = Git.branchCreate(repository, name)

                override fun succeeded() = State.fireRefresh()

                override fun failed() {
                    when (exception) {
                        is RefAlreadyExistsException -> errorAlert(scene.window,
                                "Cannot Create Branch",
                                "Branch '$name' does already exist in the working copy.")
                        else -> exception.printStackTrace()
                    }
                }

                override fun done() = Platform.runLater { State.removeProcess() }
            })
        }
    }

    private fun stash(repository: LocalRepository) {
        State.addProcess("Stashing files...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.stash(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun stashPop(repository: LocalRepository) {
        State.addProcess("Applying stash...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.stashPop(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is StashApplyFailureException -> {
                        State.fireRefresh()
                        errorAlert(scene.window,
                                "Cannot Pop Stash",
                                "Applying stashed changes resulted in a conflict.\nTherefore the stash entry has been preserved.")
                    }
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

}
