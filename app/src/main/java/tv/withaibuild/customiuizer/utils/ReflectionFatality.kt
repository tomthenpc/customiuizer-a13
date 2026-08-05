package tv.withaibuild.customiuizer.utils

import java.lang.reflect.InvocationTargetException

internal object ReflectionFatality {

    fun rethrowIfFatal(error: Throwable) {
        val candidate =
            if (error is InvocationTargetException) {
                error.cause ?: error
            } else {
                error
            }

        if (
            candidate is OutOfMemoryError ||
            candidate is ThreadDeath ||
            candidate is VirtualMachineError
        ) throw candidate
    }
}
