package com.movie.app.best.util.cf

import android.app.Activity
import com.ead.lib.cloudflare_bypass.solver.CloudFlareSolver
import com.ead.lib.cloudflare_bypass.solver.SolverConfig
import com.ead.lib.cloudflare_bypass.solver.SolverEvent
import com.ead.lib.cloudflare_bypass.solver.SolverResult
import com.movie.app.best.data.repository.DOWNLOAD_USER_AGENT
import java.net.URL

class CfBypassController(private val activity: Activity) {

    private val moduleRegistry = ModuleRegistry()

    suspend fun solveAndExtractDirectUrl(
        jackpotUrl: String,
        onLog: (String) -> Unit
    ): ModuleResult? {
        val cookieHeader = solve(jackpotUrl, onLog) ?: run {
            onLog("❌ Cloudflare solve failed — cannot extract direct URL")
            return null
        }
        onLog("🍪 cf_clearance stored in CookieManager")

        val module = moduleRegistry.findMatch(jackpotUrl)
        return if (module != null) {
            onLog("🎭 Module matched: ${module.tag}")
            module.scrape(activity, jackpotUrl, onLog)
        } else {
            onLog("🔗 No host module — generic redirect follower")
            RedirectFollower().follow(activity, jackpotUrl, onLog)
        }
    }

    suspend fun solve(
        jackpotUrl: String,
        onLog: (String) -> Unit
    ): String? {
        val host = runCatching { URL(jackpotUrl).host }.getOrNull() ?: return null
        val rootUrl = "https://$host/"

        onLog("Target host: $host")

        val header = solveUrl(jackpotUrl, onLog)
        if (header != null) return header

        onLog("⚠️ Direct solve failed — retrying with host root")
        return solveUrl(rootUrl, onLog)
    }

    private suspend fun solveUrl(url: String, onLog: (String) -> Unit): String? {
        val solver = CloudFlareSolver(
            activity,
            SolverConfig(
                timeoutMs = 40_000,
                backgroundSolveMs = 20_000,
                userAgent = DOWNLOAD_USER_AGENT,
            ),
        )

        solver.onEvent = { ev ->
            val icon = when (ev.level) {
                SolverEvent.OK -> "✅"
                SolverEvent.ERR -> "❌"
                SolverEvent.TAP -> "👆"
                else -> "ℹ️"
            }
            onLog("$icon ${ev.message}")
        }

        return try {
            when (val r = solver.solve(url)) {
                is SolverResult.Solved -> {
                    onLog("cf_clearance acquired for ${r.domain}")
                    r.cookieHeader
                }
                is SolverResult.InteractionNeeded -> {
                    onLog("⚠️ Interaction needed — silent mode cannot proceed")
                    null
                }
                is SolverResult.Failed -> {
                    onLog("❌ Failed: ${r.reason}")
                    null
                }
                is SolverResult.Cancelled -> {
                    onLog("Cancelled")
                    null
                }
            }
        } catch (e: Exception) {
            onLog("❌ Solver error: ${e.message ?: e::class.simpleName}")
            null
        } finally {
            solver.close()
        }
    }
}
