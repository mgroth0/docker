package matt.docker.recipe

import kotlinx.serialization.Serializable
import matt.json.jser.oser.JavaIoSerializable
import matt.lang.anno.SeeURL
import matt.prim.str.joinWithNewLines
import matt.shell.Command
import matt.shell.CommandReturner
import matt.shell.Shell

@Serializable
sealed interface DockerRecipe

@Serializable
class DockerRecipeText(val dockerRecipe: String) : DockerRecipe, JavaIoSerializable

@Serializable
class DockerStageText(val dockerStageRecipe: String)

@Serializable
object ExtractDockerRecipe : DockerRecipe

const val DockerfileName = "Dockerfile"

fun openJDKDocker(
    openJDKVersion: String,
    op: DockerfileStageDSL.() -> Unit
): DockerRecipeText {
    return DockerfileDSL().apply {
        stage(OpenJdk(openJDKVersion), op)
    }.text()
}

fun dockerFile(
    op: DockerfileDSL.() -> Unit
): DockerRecipeText {
    return DockerfileDSL().apply(op).text()
}

interface DockerFrom {
    val image: String
}

class OpenJdk(version: String) : DockerFrom {
    override val image = "openjdk:$version"
}

@SeeURL("https://github.com/gradle/gradle/issues/19682#issuecomment-1256202437")
@SeeURL("https://stackoverflow.com/q/73516116/6596010")
class AmazonCorretto(version: String) : DockerFrom {
    override val image = "amazoncorretto:$version"
}

@DslMarker
annotation class DockerDsl


@DockerDsl
class DockerfileDSL {
    private val stages = mutableListOf<DockerfileStageDSL>()
    fun stage(from: DockerFrom, op: DockerfileStageDSL.() -> Unit) {
        stages += DockerfileStageDSL(from).apply(op)
    }

    fun text() = DockerRecipeText(stages.joinToString(separator = "\n\n") { it.text().dockerStageRecipe })
}

@DockerDsl
@SeeURL("https://docs.docker.com/build/building/multi-stage/")
@SeeURL("https://hub.docker.com/r/karthequian/helloworld/dockerfile")
class DockerfileStageDSL(from: DockerFrom) {
    private val lines = mutableListOf<String>()
    fun text() = DockerStageText(lines.joinWithNewLines())


    /*most recent heroku stack is Heroku-22 (Ubuntu 22.04	)*/
    /*mgroth0/jdk:17*/
    private fun from(arg: String) {
        lines += "FROM $arg"
    }

    init {
        from(from.image)
    }

    @SeeURL("https://docs.docker.com/engine/reference/builder/#copy")
    fun copy(from: String, to: String) {
        lines += "COPY $from $to"
    }

    fun add(arg: String) {
        lines += "ADD $arg"
    }


    fun run(command: Command) {
        /*not sure if rawWithNoEscaping is best here...*/
        lines += "RUN ${command.rawWithNoEscaping()}"
    }

    val runCommand get() = DockerCommander()

    @SeeURL("https://stackoverflow.com/a/35294506/6596010")
    private var previousWorkDirs = mutableListOf<String>()


    @DockerDsl
    inner class DockerCommander : Shell<Unit> {
        override fun sendCommand(vararg args: String) {
            val command = CommandReturner.sendCommand(*args)
            this@DockerfileStageDSL.run(command)
        }

        fun withWorkDir(
            workdir: String? = null,
            op: DockerCommander.() -> Unit
        ) = this@DockerfileStageDSL.runCommands(workdir = workdir, op = op)
    }

    fun runCommands(
        workdir: String? = null,
        op: DockerCommander.() -> Unit
    ) {
        if (workdir != null) {
            previousWorkDirs += workdir
            workdir(workdir)
        }
        runCommand.apply(op)
        if (workdir != null) {
            require(previousWorkDirs.removeLast() == workdir)
            if (previousWorkDirs.isEmpty()) {
                resetWorkdir()
            } else {
                workdir(previousWorkDirs.last())
            }

        }
    }

    @SeeURL("https://stackoverflow.com/a/35294506/6596010")
    fun resetWorkdir() = workdir("/")

    fun workdir(arg: String) {
        lines += "WORKDIR $arg"
    }


    fun user(arg: String) {
        lines += "USER $arg"
    }


    fun cmd(arg: String) {
        lines += "CMD $arg"
    }

    fun arg(arg: String) {
        lines += "ARG $arg"
    }

    fun dockerEnv(arg: String) {
        lines += "ENV $arg"
    }


}