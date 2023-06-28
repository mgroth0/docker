package matt.docker.recipe.jprofiler

import matt.docker.command.EXTRA_DOCKER_CTX_NAME
import matt.docker.recipe.DockerfileStageDSL
import matt.lang.anno.SeeURL
import matt.shell.commands.rm.rm

private const val J_PROFILER_RPM = "jprofiler_linux_13_0_6.rpm"

fun DockerfileStageDSL.copyAndInstallJProfiler() {
    copy("--from=$EXTRA_DOCKER_CTX_NAME $J_PROFILER_RPM", J_PROFILER_RPM)

    runCommands {
        @SeeURL("https://github.com/rpm-software-management/microdnf/issues/20#issuecomment-1098784347")
        sendCommand("rpm", "-i", J_PROFILER_RPM)
        /*microdnf.install(jprofilerRpm, autoConfirm = true)*/
        rm(J_PROFILER_RPM)
    }

}
