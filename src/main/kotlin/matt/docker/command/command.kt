package matt.docker.command

import matt.lang.If
import matt.lang.anno.SeeURL
import matt.lang.optArray
import matt.model.data.file.FilePath
import matt.shell.Commandable
import matt.shell.ControlledShellProgram
import matt.shell.Shell

val <R> Shell<R>.docker get() = DockerCommand(this)

class DockerCommand<R>(shell: Shell<R>) : ControlledShellProgram<R>(
    shell = shell,
    program = "/usr/local/bin/docker"
) {


    fun build(vararg args: String): R = sendCommand(
        this::build.name,
        * args
    )

    enum class DockerPlatforms(internal val label: String) {
        amd64("linux/amd64");
    }


    val buildx get() = DockerBuildX(this)

    class DockerBuildX<R>(docker: DockerCommand<R>) :
        ControlledShellProgram<R>(program = "buildx", shell = object : Commandable<R> {
            override fun sendCommand(vararg args: String): R {
                return docker.sendCommand(*args)
            }
        }) {

        /*https://stackoverflow.com/questions/66982720/keep-running-into-the-same-deployment-error-exec-format-error-when-pushing-nod*/
        @SeeURL("https://docs.docker.com/build/building/multi-platform/#building-multi-platform-images")
        fun build(
            path: String,
            platform: DockerPlatforms? = null,
            tag: String? = null,
            quiet: Boolean = false,
            /*squash: Boolean = false*/
        ): R =
            sendCommand(
                this::build.name,
                *optArray(platform) { arrayOf("--platform", label) },
                *optArray(tag) { arrayOf("-t", this) },
                *If(quiet).then("-q"),
                /**If(squash).then("--squash"),*/
                path
            )
    }

    @SeeURL("https://docs.docker.com/engine/reference/commandline/tag/")
    fun tag(sourceImage: String, targetImage: String): R = sendCommand(
        this::tag.name, sourceImage, targetImage
    )


    fun push(vararg args: String): R = sendCommand(
        this::push.name, *args
    )

    fun ps(vararg args: String): R = sendCommand(
        this::ps.name, *args
    )

    fun stop(vararg args: String): R = sendCommand(
        this::stop.name, *args
    )

    fun run(vararg args: String): R = sendCommand(
        this::run.name, *args
    )

    fun system(vararg args: String): R = sendCommand(
        this::system.name, *args
    )

    fun images(vararg args: String): R = sendCommand(
        this::images.name, *args
    )

    fun rmi(vararg args: String): R = sendCommand(
        this::rmi.name, *args
    )

    fun rm(vararg args: String): R = sendCommand(
        this::rm.name, *args
    )


    fun save(
        image: String,
        outputFile: FilePath? = null
    ): R = sendCommand(
        this::save.name,
        *optArray(outputFile) { arrayOf("-o", filePath) },
        image
    )


}


/*

 TODO: need to eventually start managing the images and containers that will accumulate. But for now keeping them is ok. If anything, it will make things faster. Unless there really is a completely clear separation of those from caches. By the way, system prune deletes caches which is bad!

	*/

fun DockerCommand<String>.cleanup() {
    system("prune", "-a")
    images("-a").lines().forEach {
        rmi(it.trim())
    }
}

fun DockerCommand<String>.stopAll() {
    ps("-a", "-q").lines().forEach {
        stop(it.trim())
        /*docker("rm", it.trim())*/
    }
}