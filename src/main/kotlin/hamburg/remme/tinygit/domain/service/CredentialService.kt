package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.Service
import hamburg.remme.tinygit.domain.Credentials
import hamburg.remme.tinygit.git.gitCredentialKeychainGet
import hamburg.remme.tinygit.git.gitCredentialKeychainStore
import hamburg.remme.tinygit.git.gitCredentialWincredGet
import hamburg.remme.tinygit.git.gitCredentialWincredStore
import hamburg.remme.tinygit.isMac
import hamburg.remme.tinygit.isWindows

@Service
class CredentialService {

    lateinit var credentialHandler: (String) -> Credentials?

    // TODO: ssh / ssh-add functionality
    // TODO: refactor this to beauty
    fun applyCredentials(remote: String) {
        if (remote.isNotBlank() && remote.startsWith("http")) {
            val remoteMatch = "(https?)://(.+@)?(.+\\..+?)/.+".toRegex().matchEntire(remote)!!.groupValues
            val dummy = getCredentials(remoteMatch[3], remoteMatch[1])
            if (dummy.isEmpty) credentialHandler(dummy.host)?.let {
                setCredentials(Credentials(it.username, it.password, dummy.host, dummy.protocol))
            }
        }
    }

    private fun getCredentials(host: String, protocol: String) = when {
        isWindows -> gitCredentialWincredGet(host, protocol)
        isMac -> gitCredentialKeychainGet(host, protocol)
        else -> throw RuntimeException("Credentials currently not supported for your OS.")
    }

    private fun setCredentials(credentials: Credentials) = when {
        isWindows -> gitCredentialWincredStore(credentials)
        isMac -> gitCredentialKeychainStore(credentials)
        else -> throw RuntimeException("Credentials currently not supported for your OS.")
    }

}
