package com.beust.kash

import com.beust.kash.api.CommandResult
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object Background {
    private val log = LoggerFactory.getLogger(Background::class.java)

    fun launchBackgroundCommand(
            onFinish: ((BackgroundCommandResult) -> Unit)? = null,
            f: () -> CommandResult): CommandResult {
        log.debug("Launching in background")
        val future = backgroundProcessesExecutor.submit<Void> {
            val commandResult = f()
            if (onFinish != null) {
                onFinish(BackgroundCommandResult(42, commandResult.returnCode))
            }
            commandResult.display()
            null
        }
        return CommandResult(0)
    }

    private val backgroundProcessesExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    private val backgroundProcesses = hashMapOf<Int, BackgroundCommand>()
}