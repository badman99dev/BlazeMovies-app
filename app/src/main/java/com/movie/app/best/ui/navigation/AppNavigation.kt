package com.movie.app.best.ui.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.movie.app.best.ui.screens.categories.CategoriesScreen
import com.movie.app.best.ui.screens.categories.CategoryPageScreen
import com.movie.app.best.ui.screens.downloads.DownloadsScreen
import com.movie.app.best.ui.screens.downloads.ExtractedSeriesScreen
import com.movie.app.best.ui.screens.downloads.LocalVideoScreen
import com.movie.app.best.ui.screens.home.HomeScreen
import com.movie.app.best.ui.screens.library.LibraryScreen
import com.movie.app.best.ui.screens.moviedetail.MovieDetailScreen
import com.movie.app.best.ui.screens.search.SearchScreen
import com.movie.app.best.ui.screens.tvshowdetail.TVShowDetailScreen
import com.movie.app.best.ui.screens.tvshows.TVShowsScreen
import com.movie.app.best.ui.screens.livetv.LiveTvScreen
import com.movie.app.best.ui.screens.serieswatch.SeriesWatchScreen
import com.movie.app.best.ui.screens.moviewatch.MovieWatchScreen
import java.net.URLEncoder
import com.movie.app.best.ui.screens.player.VideoPlayerScreen
import com.movie.app.best.ui.screens.movies.MoviesScreen
import com.movie.app.best.ui.screens.settings.SettingsScreen
import com.movie.app.best.ui.screens.trending.TrendingScreen
import com.movie.app.best.ui.screens.latestupload.LatestUploadScreen
import com.movie.app.best.ui.screens.myfeed.MyFeedScreen
import com.movie.app.best.ui.screens.auth.LoginScreen
import com.movie.app.best.ui.screens.profile.ProfileScreen
import com.movie.app.best.ui.screens.notification.NotificationScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Movies : Screen("movies")
    object Trending : Screen("trending")
    object Zee5 : Screen("zee5")
    object LatestUploads : Screen("latest-uploads")
    object MyFeed : Screen("my-feed")
    object Categories : Screen("categories")
    object CategoryPage : Screen("category/{categorySlug}/{categoryName}") {
        fun createRoute(slug: String, name: String): String {
            return "category/${Uri.encode(slug)}/${Uri.encode(name)}"
        }
    }
    object MovieDetail : Screen("movie/{slug}") {
        fun createRoute(slug: String) = "movie/${Uri.encode(slug)}"
    }
    object Search : Screen("search")
    object TVShows : Screen("tv-shows")
    object LiveTv : Screen("live-tv")
    object Downloads : Screen("downloads")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object LocalVideos : Screen("localVideos")
    object Notifications : Screen("notifications")
    object ExtractedSeries : Screen("extractedSeries/{extractPath}/{slug}/{posterPath}") {
        fun createRoute(extractPath: String, slug: String, posterPath: String): String {
            return "extractedSeries/${Uri.encode(extractPath)}/${Uri.encode(slug)}/${Uri.encode(posterPath)}"
        }
    }
    object SeriesDetail : Screen("series/{slug}") {
        fun createRoute(slug: String) = "series/${Uri.encode(slug)}"
    }
    object SeriesWatch : Screen("seriesWatch/{imdbId}/{title}/{movieId}/{slug}/{targetSeason}?cast={cast}&director={director}&description={description}") {
        fun createRoute(imdbId: String, title: String, movieId: String, slug: String, targetSeason: Int = -1, cast: String = "", director: String = "", description: String = ""): String {
            return "seriesWatch/${Uri.encode(imdbId)}/${Uri.encode(title)}/${Uri.encode(movieId)}/${Uri.encode(slug)}/${targetSeason}?cast=${Uri.encode(cast)}&director=${Uri.encode(director)}&description=${Uri.encode(description)}"
        }
    }
    object MovieWatch : Screen("movieWatch/{slug}?imdbId={imdbId}&title={title}&movieId={movieId}&hasStream={hasStream}&playerUrl={playerUrl}&posterUrl={posterUrl}&cast={cast}&director={director}&description={description}") {
        fun createRoute(slug: String, imdbId: String, title: String, movieId: String, hasStream: Boolean, playerUrl: String, posterUrl: String, cast: String = "", director: String = "", description: String = ""): String {
            return "movieWatch/${Uri.encode(slug)}?imdbId=${URLEncoder.encode(imdbId, "UTF-8")}&title=${Uri.encode(title)}&movieId=${URLEncoder.encode(movieId, "UTF-8")}&hasStream=$hasStream&playerUrl=${URLEncoder.encode(playerUrl, "UTF-8")}&posterUrl=${URLEncoder.encode(posterUrl, "UTF-8")}&cast=${Uri.encode(cast)}&director=${Uri.encode(director)}&description=${Uri.encode(description)}"
        }
    }
    object VideoPlayer : Screen("videoPlayer?playerUrl={playerUrl}&streamUrl={streamUrl}&title={title}&youtubeId={youtubeId}&movieId={movieId}&slug={slug}&isLive={isLive}&contentSource={contentSource}") {
        fun createRoute(playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String = "", slug: String = "", isLive: Boolean = false, contentSource: String = "wasmer"): String {
            return "videoPlayer?playerUrl=${URLEncoder.encode(playerUrl, "UTF-8")}&streamUrl=${URLEncoder.encode(streamUrl, "UTF-8")}&title=${Uri.encode(title)}&youtubeId=${URLEncoder.encode(youtubeId, "UTF-8")}&movieId=${URLEncoder.encode(movieId, "UTF-8")}&slug=${URLEncoder.encode(slug, "UTF-8")}&isLive=$isLive&contentSource=${URLEncoder.encode(contentSource, "UTF-8")}"
        }
    }
    object YoutubeTrailer : Screen("youtubeTrailer/{youtubeId}?title={title}&imdbId={imdbId}") {
        fun createRoute(youtubeId: String, title: String, imdbId: String = ""): String {
            return "youtubeTrailer/${Uri.encode(youtubeId)}?title=${Uri.encode(title)}&imdbId=${URLEncoder.encode(imdbId, "UTF-8")}"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    isOnline: Boolean = true,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun navigateToContent(slug: String, isSeries: Boolean) {
        if (isSeries) {
            navController.navigate(Screen.SeriesDetail.createRoute(slug))
        } else {
            navController.navigate(Screen.MovieDetail.createRoute(slug))
        }
    }

    val tabRoutes = listOf(
        Screen.Home.route,
        Screen.Zee5.route,
        Screen.Library.route,
        Screen.Downloads.route,
        Screen.Profile.route
    )

    fun slideDirection(from: String?, to: String?): Int {
        val fromIdx = tabRoutes.indexOf(from)
        val toIdx = tabRoutes.indexOf(to)
        if (fromIdx < 0 || toIdx < 0) return 0
        return if (toIdx > fromIdx) 1 else -1
    }

    val tabEnter: (from: String?, to: String?, Int) -> EnterTransition = { from, to, dir ->
        if (dir != 0) {
            slideInHorizontally(
                initialOffsetX = { w -> dir * w / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(200))
        } else {
            slideInHorizontally(
                initialOffsetX = { w -> w },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(200))
        }
    }
    val tabExit: (from: String?, to: String?, Int) -> ExitTransition = { from, to, dir ->
        if (dir != 0) {
            slideOutHorizontally(
                targetOffsetX = { w -> -dir * w / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(150))
        } else {
            slideOutHorizontally(
                targetOffsetX = { w -> -w / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(150))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            if (!isOnline) EnterTransition.None
            else tabEnter(initialState.destination.route, targetState.destination.route, slideDirection(initialState.destination.route, targetState.destination.route))
        },
        exitTransition = {
            if (!isOnline) ExitTransition.None
            else tabExit(initialState.destination.route, targetState.destination.route, slideDirection(initialState.destination.route, targetState.destination.route))
        },
        popEnterTransition = {
            if (!isOnline) EnterTransition.None
            else tabEnter(targetState.destination.route, initialState.destination.route, slideDirection(targetState.destination.route, initialState.destination.route))
        },
        popExitTransition = {
            if (!isOnline) ExitTransition.None
            else tabExit(targetState.destination.route, initialState.destination.route, slideDirection(targetState.destination.route, initialState.destination.route))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onDownloadClick = { navController.navigate(Screen.Downloads.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            MovieDetailScreen(
                slug = slug,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { playerUrl, streamUrl, title, youtubeId, movieId, slug ->
                    navController.navigate(Screen.VideoPlayer.createRoute(playerUrl, streamUrl, title, youtubeId, movieId, slug))
                },
                onTrailerClick = { youtubeId, title, imdbId ->
                    navController.navigate(Screen.YoutubeTrailer.createRoute(youtubeId, title, imdbId))
                },
                onWatchClick = { imdbId, mTitle, mId, mSlug, mHasStream, mPlayerUrl, mPosterUrl, mCast, mDirector, mDescription ->
                    navController.navigate(Screen.MovieWatch.createRoute(mSlug, imdbId, mTitle, mId, mHasStream, mPlayerUrl, mPosterUrl, mCast, mDirector, mDescription))
                },                onSeriesClick = { seriesSlug ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesSlug)) {
                        popUpTo(Screen.MovieDetail.route) { inclusive = true }
                    }
                },
                onMovieClick = { movieSlug ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieSlug)) {
                        popUpTo(Screen.MovieDetail.route) { inclusive = true }
                    }
                },
                onDownloadClick = { },
                onGoToDownloads = {
                    navController.navigate(Screen.Downloads.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenExtractedSeries = { extractPath, slug, posterPath ->
                    navController.navigate(Screen.ExtractedSeries.createRoute(extractPath, slug, posterPath))
                }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            TVShowDetailScreen(
                slug = slug,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { playerUrl, streamUrl, title, youtubeId, movieId, slug ->
                    navController.navigate(Screen.VideoPlayer.createRoute(playerUrl, streamUrl, title, youtubeId, movieId, slug))
                },
                onTrailerClick = { youtubeId, title, imdbId ->
                    navController.navigate(Screen.YoutubeTrailer.createRoute(youtubeId, title, imdbId))
                },
                onWatchNow = { imdbId, title, movieId, slug, targetSeason, cast, director, description ->
                    navController.navigate(Screen.SeriesWatch.createRoute(imdbId, title, movieId, slug, targetSeason, cast, director, description))
                },
                onMovieClick = { movieSlug ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieSlug))
                },
                onSeriesClick = { seriesSlug ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesSlug)) {
                        popUpTo(Screen.SeriesDetail.route) { inclusive = true }
                    }
                },
                onGoToDownloads = {
                    navController.navigate(Screen.Downloads.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenExtractedSeries = { extractPath, slug, posterPath ->
                    navController.navigate(Screen.ExtractedSeries.createRoute(extractPath, slug, posterPath))
                }
            )
        }

        composable(
            route = Screen.SeriesWatch.route,
            arguments = listOf(
                navArgument("imdbId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("movieId") { type = NavType.StringType },
                navArgument("slug") { type = NavType.StringType },
                navArgument("targetSeason") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("cast") { type = NavType.StringType; defaultValue = "" },
                navArgument("director") { type = NavType.StringType; defaultValue = "" },
                navArgument("description") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            SeriesWatchScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MovieWatch.route,
            arguments = listOf(
                navArgument("slug") { type = NavType.StringType },
                navArgument("imdbId") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("movieId") { type = NavType.StringType; defaultValue = "" },
                navArgument("hasStream") { type = NavType.BoolType; defaultValue = false },
                navArgument("playerUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("posterUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("cast") { type = NavType.StringType; defaultValue = "" },
                navArgument("director") { type = NavType.StringType; defaultValue = "" },
                navArgument("description") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            MovieWatchScreen(
                onBackClick = { navController.popBackStack() },
                onMovieClick = { movieSlug ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieSlug)) {
                        popUpTo(Screen.MovieWatch.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                onZee5Click = { id -> navController.navigate("zee5_detail/$id") },
                onTvChannelClick = { channel ->
                    navController.navigate(
                        Screen.VideoPlayer.createRoute(
                            playerUrl = channel.streamUrl,
                            streamUrl = "",
                            title = channel.name,
                            youtubeId = "",
                            isLive = true,
                            contentSource = "live_tv"
                        )
                    )
                }
            )
        }

        composable(Screen.TVShows.route) {
            TVShowsScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(Screen.LiveTv.route) {
            LiveTvScreen(
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channel ->
                    navController.navigate(
                        Screen.VideoPlayer.createRoute(
                            playerUrl = channel.streamUrl,
                            streamUrl = "",
                            title = channel.name,
                            youtubeId = "",
                            isLive = true,
                            contentSource = "live_tv"
                        )
                    )
                },
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onCategoryClick = { slug, name ->
                    navController.navigate(Screen.CategoryPage.createRoute(slug, name))
                },
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(
            route = Screen.CategoryPage.route,
            arguments = listOf(
                navArgument("categorySlug") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("categorySlug") ?: ""
            val name = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryPageScreen(
                categorySlug = slug,
                categoryName = name,
                onContentClick = { s, isSeries -> navigateToContent(s, isSeries) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("playerUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("streamUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("youtubeId") { type = NavType.StringType; defaultValue = "" },
                navArgument("movieId") { type = NavType.StringType; defaultValue = "" },
                navArgument("slug") { type = NavType.StringType; defaultValue = "" },
                navArgument("isLive") { type = NavType.BoolType; defaultValue = false },
                navArgument("contentSource") { type = NavType.StringType; defaultValue = "wasmer" }
            )
        ) { backStackEntry ->
            val playerUrl = backStackEntry.arguments?.getString("playerUrl") ?: ""
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val youtubeId = backStackEntry.arguments?.getString("youtubeId") ?: ""
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            val isLive = backStackEntry.arguments?.getBoolean("isLive") ?: false
            val contentSource = backStackEntry.arguments?.getString("contentSource") ?: "wasmer"

            VideoPlayerScreen(
                onBackClick = { navController.popBackStack() },
                playerUrl = playerUrl,
                streamUrl = streamUrl,
                title = title,
                youtubeId = youtubeId,
                movieId = movieId,
                slug = slug,
                isLive = isLive,
                contentSource = contentSource
            )
        }

        composable(
            route = Screen.YoutubeTrailer.route,
            arguments = listOf(
                navArgument("youtubeId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("imdbId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val ytId = backStackEntry.arguments?.getString("youtubeId") ?: ""
            val ytTitle = backStackEntry.arguments?.getString("title") ?: ""
            val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
            com.movie.app.best.ui.screens.player.YoutubeTrailerScreen(
                youtubeId = ytId,
                title = ytTitle,
                imdbId = imdbId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Movies.route) {
            MoviesScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onMenuClick = onMenuClick
            )
        }

        composable(Screen.Trending.route) {
            TrendingScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController
            )
        }

        composable(Screen.Zee5.route) {
            com.movie.app.best.ui.screens.zee5.Zee5Screen(
                navController = navController,
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )
        }

        composable(
            route = "zee5_detail/{contentId}",
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
            com.movie.app.best.ui.screens.zee5.Zee5DetailScreen(
                contentId = contentId,
                navController = navController
            )
        }

        composable(
            route = "zee5_collection/{collectionId}?title={title}",
            arguments = listOf(
                navArgument("collectionId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            com.movie.app.best.ui.screens.zee5.Zee5CollectionScreen(
                navController = navController
            )
        }

        composable(
            route = "zee5_watch/{contentId}?epId={epId}&epNum={epNum}",
            arguments = listOf(
                navArgument("contentId") { type = NavType.StringType },
                navArgument("epId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("epNum") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            com.movie.app.best.ui.screens.zee5.Zee5WatchScreen(
                onBackClick = { navController.popBackStack() },
                onRelatedItemClick = { id ->
                    navController.navigate("zee5_detail/$id")
                }
            )
        }

        composable(Screen.LatestUploads.route) {
            LatestUploadScreen(
                onBackClick = { navController.popBackStack() },
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) }
            )
        }

        composable(Screen.MyFeed.route) {
            MyFeedScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                navController = navController
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onLocalVideosClick = { navController.navigate(Screen.LocalVideos.route) },
                onPlayFile = { path, name ->
                    navController.navigate(Screen.VideoPlayer.createRoute("", path, name, "", ""))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onOpenExtractedSeries = { extractPath, slug, posterPath ->
                    navController.navigate(Screen.ExtractedSeries.createRoute(extractPath, slug, posterPath))
                }
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onContentClick = { slug, isSeries -> navigateToContent(slug, isSeries) },
                onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onBookmarksClick = { navController.navigate(Screen.Library.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.LocalVideos.route) {
            LocalVideoScreen(
                onBackClick = { navController.popBackStack() },
                onPlayVideo = { path, title ->
                    navController.navigate(Screen.VideoPlayer.createRoute("", "file://$path", title, "", ""))
                }
            )
        }

        composable(
            route = Screen.ExtractedSeries.route,
            arguments = listOf(
                navArgument("extractPath") { type = NavType.StringType },
                navArgument("slug") { type = NavType.StringType },
                navArgument("posterPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val extractPath = backStackEntry.arguments?.getString("extractPath") ?: ""
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            val posterPath = backStackEntry.arguments?.getString("posterPath") ?: ""
            ExtractedSeriesScreen(
                extractPath = extractPath,
                slug = slug,
                posterPath = posterPath,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
