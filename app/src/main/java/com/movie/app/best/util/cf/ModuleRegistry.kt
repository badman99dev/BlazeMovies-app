package com.movie.app.best.util.cf

import com.movie.app.best.util.cf.modules.FilmyzillaModule
import java.net.URL

class ModuleRegistry {

    private val modules: List<HostBypassModule> = listOf(
        FilmyzillaModule()
    )

    fun findMatch(url: String): HostBypassModule? {
        val host = runCatching { URL(url).host }.getOrNull() ?: return null
        return modules.firstOrNull { it.matches(host) }
    }
}
