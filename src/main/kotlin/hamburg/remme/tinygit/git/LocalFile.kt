package hamburg.remme.tinygit.git

class LocalFile(val path: String, val status: Status) {

    override fun toString() = path

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalFile

        if (path != other.path) return false

        return true
    }

    override fun hashCode() = path.hashCode()

    enum class Status { CONFLICT, ADDED, CHANGED, MODIFIED, REMOVED, MISSING, UNTRACKED }

}