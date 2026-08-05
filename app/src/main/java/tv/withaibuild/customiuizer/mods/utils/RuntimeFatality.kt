package tv.withaibuild.customiuizer.mods.utils

/**
 * Shared fatal-error propagation helper.
 *
 * Traverses the cause chain (up to 8 levels) and rethrows the original fatal
 * instance when one of the following is found:
 * - OutOfMemoryError
 * - ThreadDeath
 * - VirtualMachineError
 *
 * Ordinary throwables are swallowed by normal return.
 */
internal object RuntimeFatality {

    @JvmStatic
    fun throwIfFatal(throwable: Throwable?) {
        var current = throwable

        for (depth in 0 until 8) {
            if (current == null) return

            when (current) {
                is OutOfMemoryError -> throw current
                is ThreadDeath -> throw current
                is VirtualMachineError -> throw current
            }

            val next = current.cause
            if (next === current) return
            current = next
        }
    }
}
