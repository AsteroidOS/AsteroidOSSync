package org.asteroidos.sync.connectivity

/**
 * Every Service of Sync has to implement the [IService] interface.
 * A service is a module that either processes information that is exchanged
 * with the watch or is interested in knowing when the watch is connected and
 * when the watch is disconnected,
 * ([IService.sync]) is called when the watch is connected.
 * ([IService.unsync]) is called when the watch is disconnected.
 */
interface IService {
    fun sync()
    fun unsync()
}