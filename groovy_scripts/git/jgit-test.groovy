@Grapes(
    @Grab(group='org.eclipse.jgit', module='org.eclipse.jgit', version='3.2.0.201312181205-r')
)
import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.*
// See https://wiki.eclipse.org/JGit/User_Guide
def repository = "/path/to/git/repo"
def logs = new Git(repository).log()
                .all()
                .call();