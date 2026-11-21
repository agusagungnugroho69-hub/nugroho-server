package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.example.data.model.CableRouteEntity
import com.example.data.model.FtthCustomerPinEntity
import com.example.data.model.FtthNodeEntity
import com.example.data.model.FtthNodeType
import com.example.ui.theme.NocAmber
import com.example.ui.theme.NocBlueDownload
import com.example.ui.theme.NocCyanTelemetry
import com.example.ui.theme.NocEmeraldOnline
import com.example.ui.theme.NocPurpleAccent
import com.example.ui.theme.NocRedOffline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

enum class FtthBasemap(val label: String) {
    SATELIT("Satelit"),
    SATELIT_POLOS("Satelit polos"),
    OSM("OSM"),
    GELAP("Gelap")
}

enum class PinLinkStatus { ONLINE, OFFLINE, ISOLIR }

data class LatLngPoint(val lat: Double, val lng: Double)

data class CustomerPinRenderItem(
    val pin: FtthCustomerPinEntity,
    val status: PinLinkStatus,
    val speedLabel: String
)

data class NearestRoutePreview(
    val from: LatLngPoint,
    val targetNode: FtthNodeEntity,
    val distanceMeters: Int
)

private object TileCacheEngine {
    private val cache = object : LruCache<String, ImageBitmap>(180) {}
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun getCached(key: String): ImageBitmap? = cache.get(key)

    suspend fun fetchTile(basemap: FtthBasemap, z: Int, x: Int, y: Int): ImageBitmap? {
        val key = "${basemap.name}/$z/$x/$y"
        cache.get(key)?.let { return it }
        if (!inFlight.add(key)) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = when (basemap) {
                    FtthBasemap.SATELIT -> "https://mt1.google.com/vt/lyrs=y&x=$x&y=$y&z=$z"
                    FtthBasemap.SATELIT_POLOS -> "https://mt1.google.com/vt/lyrs=s&x=$x&y=$y&z=$z"
                    FtthBasemap.OSM -> "https://tile.openstreetmap.org/$z/$x/$y.png"
                    FtthBasemap.GELAP -> "https://a.basemaps.cartocdn.com/dark_all/$z/$x/$y.png"
                }
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MikroMon-Android/1.0 (FTTH-NOC-Map)")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val bytes = resp.body?.bytes() ?: return@withContext null
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
                    val img = bmp.asImageBitmap()
                    cache.put(key, img)
                    img
                }
            } catch (_: Throwable) {
                null
            } finally {
                inFlight.remove(key)
            }
        }
    }
}

object GeoProjection {
    private const val TILE_SIZE = 256.0

    fun latLngToWorldPx(lat: Double, lng: Double, zoom: Float): Pair<Double, Double> {
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val scale = TILE_SIZE * 2.0.pow(zoom.toDouble())
        val x = (lng + 180.0) / 360.0 * scale
        val latRad = Math.toRadians(clampedLat)
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * scale
        return x to y
    }

    fun worldPxToLatLng(worldX: Double, worldY: Double, zoom: Float): LatLngPoint {
        val scale = TILE_SIZE * 2.0.pow(zoom.toDouble())
        val lng = (worldX / scale) * 360.0 - 180.0
        val n = PI - 2.0 * PI * (worldY / scale)
        val lat = Math.toDegrees(atan(sinh(n)))
        return LatLngPoint(lat.coerceIn(-85.0, 85.0), lng.coerceIn(-180.0, 180.0))
    }

    fun latLngToScreen(
        lat: Double,
        lng: Double,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        canvasSize: IntSize
    ): Offset {
        val (wx, wy) = latLngToWorldPx(lat, lng, zoom)
        val (cx, cy) = latLngToWorldPx(centerLat, centerLng, zoom)
        return Offset(
            x = (canvasSize.width / 2f + (wx - cx)).toFloat(),
            y = (canvasSize.height / 2f + (wy - cy)).toFloat()
        )
    }

    fun screenToLatLng(
        offset: Offset,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        canvasSize: IntSize
    ): LatLngPoint {
        val (cx, cy) = latLngToWorldPx(centerLat, centerLng, zoom)
        val wx = cx + (offset.x - canvasSize.width / 2f)
        val wy = cy + (offset.y - canvasSize.height / 2f)
        return worldPxToLatLng(wx, wy, zoom)
    }

    fun haversineMeters(a: LatLngPoint, b: LatLngPoint): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val s1 = sin(dLat / 2)
        val s2 = sin(dLng / 2)
        val h = s1 * s1 + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * s2 * s2
        return (2.0 * r * atan2(sqrt(h), sqrt((1.0 - h).coerceAtLeast(0.0)))).roundToInt()
    }

    fun parseRoutePoints(encoded: String): List<LatLngPoint> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size < 2) null
            else {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null) LatLngPoint(lat, lng) else null
            }
        }
    }

    fun encodeRoutePoints(points: List<LatLngPoint>): String {
        return points.joinToString(";") { "${it.lat},${it.lng}" }
    }

    fun totalRouteMeters(points: List<LatLngPoint>): Int {
        if (points.size < 2) return 0
        var total = 0
        for (i in 0 until points.lastIndex) {
            total += haversineMeters(points[i], points[i + 1])
        }
        return total
    }
}

@Composable
fun FtthInteractiveMapCanvas(
    basemap: FtthBasemap,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    nodes: List<FtthNodeEntity>,
    customerPins: List<CustomerPinRenderItem>,
    cableRoutes: List<CableRouteEntity>,
    draftCablePoints: List<LatLngPoint>,
    nearestRoute: NearestRoutePreview?,
    isDrawingCable: Boolean,
    showCustomerLayer: Boolean,
    showDistributionLayer: Boolean,
    showCustomerLineLayer: Boolean,
    showDistributionLineLayer: Boolean,
    showCableRouteLayer: Boolean,
    dragMarkerMode: Boolean,
    onCameraMove: (newLat: Double, newLng: Double, newZoom: Float) -> Unit,
    onMapTap: (LatLngPoint) -> Unit,
    onMapLongPress: (LatLngPoint) -> Unit,
    onNodeTap: (FtthNodeEntity) -> Unit,
    onCustomerPinTap: (CustomerPinRenderItem) -> Unit,
    onNodeDragged: (FtthNodeEntity, LatLngPoint) -> Unit,
    onCustomerPinDragged: (FtthCustomerPinEntity, LatLngPoint) -> Unit,
    pickerLatLng: LatLngPoint? = null,
    onPickerDragged: ((LatLngPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tileTrigger by remember { mutableIntStateOf(0) }

    // Keep fresh callbacks & camera state inside pointerInput blocks
    val currentLat by rememberUpdatedState(centerLat)
    val currentLng by rememberUpdatedState(centerLng)
    val currentZoom by rememberUpdatedState(zoom)
    val currentNodes by rememberUpdatedState(nodes)
    val currentPins by rememberUpdatedState(customerPins)
    val currentDragMode by rememberUpdatedState(dragMarkerMode)
    val currentDrawCable by rememberUpdatedState(isDrawingCable)
    val currentPickerLatLng by rememberUpdatedState(pickerLatLng)
    val currentOnPickerDragged by rememberUpdatedState(onPickerDragged)

    var draggingNode by remember { mutableStateOf<FtthNodeEntity?>(null) }
    var draggingPin by remember { mutableStateOf<FtthCustomerPinEntity?>(null) }
    var draggingPicker by remember { mutableStateOf(false) }
    var dragTempLatLng by remember { mutableStateOf<LatLngPoint?>(null) }

    // Request visible tiles whenever camera or basemap changes
    val intZoom = zoom.roundToInt().coerceIn(3, 19)
    LaunchedEffect(basemap, centerLat, centerLng, intZoom, canvasSize) {
        if (canvasSize.width == 0 || canvasSize.height == 0) return@LaunchedEffect
        val maxTile = (1 shl intZoom)
        val (cx, cy) = GeoProjection.latLngToWorldPx(centerLat, centerLng, intZoom.toFloat())
        val tileSizePx = 256.0
        val halfW = canvasSize.width / 2.0 + tileSizePx
        val halfH = canvasSize.height / 2.0 + tileSizePx
        val minTx = floor((cx - halfW) / tileSizePx).toInt().coerceIn(0, maxTile - 1)
        val maxTx = floor((cx + halfW) / tileSizePx).toInt().coerceIn(0, maxTile - 1)
        val minTy = floor((cy - halfH) / tileSizePx).toInt().coerceIn(0, maxTile - 1)
        val maxTy = floor((cy + halfH) / tileSizePx).toInt().coerceIn(0, maxTile - 1)

        for (tx in minTx..maxTx) {
            for (ty in minTy..maxTy) {
                val key = "${basemap.name}/$intZoom/$tx/$ty"
                if (TileCacheEngine.getCached(key) == null) {
                    scope.launch {
                        val loaded = TileCacheEngine.fetchTile(basemap, intZoom, tx, ty)
                        if (loaded != null) tileTrigger++
                    }
                }
            }
        }
    }

    val fallbackBg = when (basemap) {
        FtthBasemap.OSM -> Color(0xFFDFDFD6)
        FtthBasemap.GELAP -> Color(0xFF0E131B)
        else -> Color(0xFF1A2820)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(fallbackBg)
            .onSizeChanged { canvasSize = it }
            // Mouse right-click (secondary button) + scroll wheel zoom handler
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val pos = event.changes.firstOrNull()?.position
                            if (pos != null && canvasSize.width > 0) {
                                event.changes.forEach { it.consume() }
                                val geo = GeoProjection.screenToLatLng(pos, currentLat, currentLng, currentZoom, canvasSize)
                                onMapLongPress(geo)
                            }
                        } else if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.firstOrNull()
                            val scrollY = change?.scrollDelta?.y ?: 0f
                            if (scrollY != 0f && canvasSize.width > 0) {
                                val newZoom = (currentZoom - scrollY * 0.6f).coerceIn(4f, 19f)
                                onCameraMove(currentLat, currentLng, newZoom)
                                change?.consume()
                            }
                        }
                    }
                }
            }
            // Pan / Zoom / Marker Drag gesture handler (respects touchSlop so long-press works)
            .pointerInput(dragMarkerMode) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var activeNodeDrag: FtthNodeEntity? = null
                    var activePinDrag: FtthCustomerPinEntity? = null
                    var activePickerDrag = false
                    var panStarted = false
                    var totalPanDistance = 0f

                    if (canvasSize.width > 0) {
                        val hitRadius = 52f
                        val pickerPoint = currentPickerLatLng
                        if (pickerPoint != null && currentOnPickerDragged != null) {
                            val pickerPos = GeoProjection.latLngToScreen(
                                pickerPoint.lat, pickerPoint.lng,
                                currentLat, currentLng, currentZoom, canvasSize
                            )
                            if ((pickerPos - down.position).getDistance() <= hitRadius) {
                                activePickerDrag = true
                                draggingPicker = true
                                dragTempLatLng = pickerPoint
                            }
                        }
                    }

                    if (!activePickerDrag && currentDragMode && canvasSize.width > 0) {
                        val hitRadius = 46f
                        val nodeHit = currentNodes.firstOrNull { node ->
                            val pos = GeoProjection.latLngToScreen(
                                node.latitude, node.longitude,
                                currentLat, currentLng, currentZoom, canvasSize
                            )
                            (pos - down.position).getDistance() <= hitRadius
                        }
                        if (nodeHit != null) {
                            activeNodeDrag = nodeHit
                            draggingNode = nodeHit
                            dragTempLatLng = LatLngPoint(nodeHit.latitude, nodeHit.longitude)
                        } else {
                            val pinHit = currentPins.firstOrNull { item ->
                                val pos = GeoProjection.latLngToScreen(
                                    item.pin.latitude, item.pin.longitude,
                                    currentLat, currentLng, currentZoom, canvasSize
                                )
                                (pos - down.position).getDistance() <= hitRadius
                            }
                            if (pinHit != null) {
                                activePinDrag = pinHit.pin
                                draggingPin = pinHit.pin
                                dragTempLatLng = LatLngPoint(pinHit.pin.latitude, pinHit.pin.longitude)
                            }
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        if (event.buttons.isSecondaryPressed) break
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        if (activePickerDrag || activeNodeDrag != null || activePinDrag != null) {
                            val change = event.changes.firstOrNull()
                            if (change != null && change.positionChanged()) {
                                val nextGeo = GeoProjection.screenToLatLng(
                                    change.position, currentLat, currentLng, currentZoom, canvasSize
                                )
                                dragTempLatLng = nextGeo
                                if (activePickerDrag) {
                                    currentOnPickerDragged?.invoke(nextGeo)
                                }
                                change.consume()
                            }
                        } else {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val pressedCount = event.changes.count { it.pressed }
                            totalPanDistance += panChange.getDistance()
                            val isZooming = pressedCount >= 2 && (kotlin.math.abs(zoomChange - 1f) > 0.002f || panChange.getDistance() > 1f)
                            if (!panStarted && (totalPanDistance > touchSlop || isZooming)) {
                                panStarted = true
                            }
                            if (panStarted) {
                                val newZoom = (currentZoom + ln(zoomChange.toDouble()).toFloat() / ln(2.0).toFloat())
                                    .coerceIn(4f, 19f)
                                val (cx, cy) = GeoProjection.latLngToWorldPx(currentLat, currentLng, currentZoom)
                                val scaleRatio = 2.0.pow((newZoom - currentZoom).toDouble())
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val nextCx = if (centroid != Offset.Unspecified && zoomChange != 1f) {
                                    val dx = centroid.x - canvasSize.width / 2f
                                    val dy = centroid.y - canvasSize.height / 2f
                                    (cx + dx) * scaleRatio - dx - panChange.x * scaleRatio
                                } else {
                                    (cx - panChange.x) * scaleRatio
                                }
                                val nextCy = if (centroid != Offset.Unspecified && zoomChange != 1f) {
                                    val dx = centroid.x - canvasSize.width / 2f
                                    val dy = centroid.y - canvasSize.height / 2f
                                    (cy + dy) * scaleRatio - dy - panChange.y * scaleRatio
                                } else {
                                    (cy - panChange.y) * scaleRatio
                                }
                                val nextLatLng = GeoProjection.worldPxToLatLng(nextCx, nextCy, newZoom)
                                onCameraMove(nextLatLng.lat, nextLatLng.lng, newZoom)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val finalGeo = dragTempLatLng
                    if (finalGeo != null) {
                        if (activePickerDrag) currentOnPickerDragged?.invoke(finalGeo)
                        activeNodeDrag?.let { onNodeDragged(it, finalGeo) }
                        activePinDrag?.let { onCustomerPinDragged(it, finalGeo) }
                    }
                    draggingPicker = false
                    draggingNode = null
                    draggingPin = null
                    dragTempLatLng = null
                }
            }
            // Tap & Long-press detector
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (canvasSize.width == 0) return@detectTapGestures
                        if (currentDrawCable) {
                            onMapTap(GeoProjection.screenToLatLng(tapOffset, currentLat, currentLng, currentZoom, canvasSize))
                            return@detectTapGestures
                        }
                        val hitRadius = 42f
                        val hitNode = currentNodes.firstOrNull { node ->
                            val pos = GeoProjection.latLngToScreen(
                                node.latitude, node.longitude,
                                currentLat, currentLng, currentZoom, canvasSize
                            )
                            (pos - tapOffset).getDistance() <= hitRadius
                        }
                        if (hitNode != null) {
                            onNodeTap(hitNode)
                            return@detectTapGestures
                        }
                        val hitPin = currentPins.firstOrNull { item ->
                            val pos = GeoProjection.latLngToScreen(
                                item.pin.latitude, item.pin.longitude,
                                currentLat, currentLng, currentZoom, canvasSize
                            )
                            (pos - tapOffset).getDistance() <= hitRadius
                        }
                        if (hitPin != null) {
                            onCustomerPinTap(hitPin)
                            return@detectTapGestures
                        }
                        onMapTap(GeoProjection.screenToLatLng(tapOffset, currentLat, currentLng, currentZoom, canvasSize))
                    },
                    onLongPress = { pressOffset ->
                        if (canvasSize.width == 0) return@detectTapGestures
                        val geo = GeoProjection.screenToLatLng(pressOffset, currentLat, currentLng, currentZoom, canvasSize)
                        onMapLongPress(geo)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
            // Read trigger so Canvas redraws when async tiles finish loading
            val _redraw = tileTrigger
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val intZ = zoom.roundToInt().coerceIn(3, 19)
            val zoomDiffScale = 2.0.pow((zoom - intZ).toDouble()).toFloat()
            val tilePx = 256f * zoomDiffScale
            val (centerWx, centerWy) = GeoProjection.latLngToWorldPx(centerLat, centerLng, zoom)
            val baseTilePx = 256.0
            val (baseCx, baseCy) = GeoProjection.latLngToWorldPx(centerLat, centerLng, intZ.toFloat())
            val maxTile = 1 shl intZ

            val minTx = floor((baseCx - size.width / (2f * zoomDiffScale)) / baseTilePx).toInt() - 1
            val maxTx = floor((baseCx + size.width / (2f * zoomDiffScale)) / baseTilePx).toInt() + 1
            val minTy = floor((baseCy - size.height / (2f * zoomDiffScale)) / baseTilePx).toInt() - 1
            val maxTy = floor((baseCy + size.height / (2f * zoomDiffScale)) / baseTilePx).toInt() + 1

            // Subtle offline grid behind tiles
            val gridColor = if (basemap == FtthBasemap.OSM) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)
            for (tx in minTx..maxTx) {
                for (ty in minTy..maxTy) {
                    val wrappedX = ((tx % maxTile) + maxTile) % maxTile
                    if (ty < 0 || ty >= maxTile) continue
                    val screenX = (size.width / 2f + (tx * tilePx - centerWx)).toFloat()
                    val screenY = (size.height / 2f + (ty * tilePx - centerWy)).toFloat()
                    val bmp = TileCacheEngine.getCached("${basemap.name}/$intZ/$wrappedX/$ty")
                    if (bmp != null) {
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
                            dstSize = IntSize(tilePx.roundToInt() + 1, tilePx.roundToInt() + 1)
                        )
                    } else {
                        drawRect(
                            color = gridColor,
                            topLeft = Offset(screenX, screenY),
                            size = Size(tilePx, tilePx),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // Slight readability tint on satellite
            if (basemap == FtthBasemap.SATELIT || basemap == FtthBasemap.SATELIT_POLOS) {
                drawRect(Color.Black.copy(alpha = 0.08f))
            }

            val cSize = IntSize(size.width.roundToInt(), size.height.roundToInt())

            fun effectiveNodePos(node: FtthNodeEntity): Offset {
                val lat = if (draggingNode?.id == node.id && dragTempLatLng != null) dragTempLatLng!!.lat else node.latitude
                val lng = if (draggingNode?.id == node.id && dragTempLatLng != null) dragTempLatLng!!.lng else node.longitude
                return GeoProjection.latLngToScreen(lat, lng, centerLat, centerLng, zoom, cSize)
            }

            fun effectivePinPos(pin: FtthCustomerPinEntity): Offset {
                val lat = if (draggingPin?.id == pin.id && dragTempLatLng != null) dragTempLatLng!!.lat else pin.latitude
                val lng = if (draggingPin?.id == pin.id && dragTempLatLng != null) dragTempLatLng!!.lng else pin.longitude
                return GeoProjection.latLngToScreen(lat, lng, centerLat, centerLng, zoom, cSize)
            }

            // 1. Saved Cable Routes layer
            if (showCableRouteLayer) {
                cableRoutes.forEach { route ->
                    val pts = GeoProjection.parseRoutePoints(route.pointsEncoded)
                    if (pts.size >= 2) {
                        val path = Path()
                        pts.forEachIndexed { idx, p ->
                            val o = GeoProjection.latLngToScreen(p.lat, p.lng, centerLat, centerLng, zoom, cSize)
                            if (idx == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                        }
                        drawPath(
                            path = path,
                            color = NocCyanTelemetry,
                            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // 2. Distribution Lines (Node -> Parent)
            if (showDistributionLineLayer) {
                nodes.forEach { node ->
                    val parent = node.parentId?.let { pid -> nodes.firstOrNull { it.id == pid } }
                    if (parent != null) {
                        val p1 = effectiveNodePos(parent)
                        val p2 = effectiveNodePos(node)
                        drawLine(
                            color = NocAmber.copy(alpha = 0.85f),
                            start = p1,
                            end = p2,
                            strokeWidth = 4.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    }
                }
            }

            // 3. Customer Lines (Customer -> Parent ODP)
            if (showCustomerLineLayer) {
                customerPins.forEach { item ->
                    val parent = item.pin.parentNodeId?.let { pid -> nodes.firstOrNull { it.id == pid } }
                    if (parent != null) {
                        val p1 = effectiveNodePos(parent)
                        val p2 = effectivePinPos(item.pin)
                        val lineColor = when (item.status) {
                            PinLinkStatus.ONLINE -> NocEmeraldOnline
                            PinLinkStatus.OFFLINE -> NocRedOffline
                            PinLinkStatus.ISOLIR -> NocAmber
                        }
                        drawLine(
                            color = lineColor.copy(alpha = 0.85f),
                            start = p1,
                            end = p2,
                            strokeWidth = 3.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    }
                }
            }

            // 4. Nearest Route Preview (Cari rute terdekat)
            nearestRoute?.let { preview ->
                val pStart = GeoProjection.latLngToScreen(preview.from.lat, preview.from.lng, centerLat, centerLng, zoom, cSize)
                val pEnd = effectiveNodePos(preview.targetNode)
                drawLine(
                    color = NocCyanTelemetry,
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                )
                drawCircle(color = NocCyanTelemetry, radius = 12f, center = pStart)
                drawCircle(color = Color.White, radius = 6f, center = pStart)
            }

            // 5. Draft Cable Drawing Route
            if (draftCablePoints.isNotEmpty()) {
                val path = Path()
                draftCablePoints.forEachIndexed { idx, p ->
                    val o = GeoProjection.latLngToScreen(p.lat, p.lng, centerLat, centerLng, zoom, cSize)
                    if (idx == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                if (draftCablePoints.size >= 2) {
                    drawPath(
                        path = path,
                        color = NocAmber,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                draftCablePoints.forEach { p ->
                    val o = GeoProjection.latLngToScreen(p.lat, p.lng, centerLat, centerLng, zoom, cSize)
                    drawCircle(color = NocAmber, radius = 10f, center = o)
                    drawCircle(color = Color.White, radius = 4f, center = o)
                }
            }

            // 6. Distribution Markers (ODP, ODC, POP, BTS, HTB)
            if (showDistributionLayer) {
                nodes.forEach { node ->
                    val pos = effectiveNodePos(node)
                    val color = when (node.type) {
                        FtthNodeType.POP -> NocPurpleAccent
                        FtthNodeType.ODC -> NocBlueDownload
                        FtthNodeType.ODP -> NocAmber
                        FtthNodeType.BTS -> NocCyanTelemetry
                        FtthNodeType.HTB -> NocEmeraldOnline
                        FtthNodeType.OTHER -> Color(0xFF94A3B8)
                    }
                    drawDistributionMarker(
                        pos = pos,
                        label = node.code.ifBlank { node.name },
                        color = color,
                        textMeasurer = textMeasurer
                    )
                }
            }

            // 7. Customer Markers (Nia, etc. with live green/red/amber + speed tag)
            if (showCustomerLayer) {
                customerPins.forEach { item ->
                    val pos = effectivePinPos(item.pin)
                    val color = when (item.status) {
                        PinLinkStatus.ONLINE -> NocEmeraldOnline
                        PinLinkStatus.OFFLINE -> NocRedOffline
                        PinLinkStatus.ISOLIR -> NocAmber
                    }
                    drawCustomerMarker(
                        pos = pos,
                        name = item.pin.name,
                        speedLabel = item.speedLabel,
                        color = color,
                        textMeasurer = textMeasurer
                    )
                }
            }

            // 8. Target Location Picker Pin (Pilih Lokasi wizard step)
            val targetPoint = if (draggingPicker && dragTempLatLng != null) dragTempLatLng else pickerLatLng
            if (targetPoint != null) {
                val pPos = GeoProjection.latLngToScreen(targetPoint.lat, targetPoint.lng, centerLat, centerLng, zoom, cSize)
                drawCircle(color = NocEmeraldOnline.copy(alpha = 0.28f), radius = 34f, center = pPos)
                drawCircle(color = NocEmeraldOnline, radius = 18f, center = pPos)
                drawCircle(color = Color.White, radius = 18f, center = pPos, style = Stroke(width = 3.5f))
                drawCircle(color = Color.White, radius = 6.5f, center = pPos)
            }
        }
    }
}

private fun DrawScope.drawDistributionMarker(
    pos: Offset,
    label: String,
    color: Color,
    textMeasurer: TextMeasurer
) {
    // Outer soft halo & rounded badge marker
    drawCircle(color = color.copy(alpha = 0.25f), radius = 26f, center = pos)
    drawRoundRect(
        color = color,
        topLeft = Offset(pos.x - 16f, pos.y - 16f),
        size = Size(32f, 32f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(pos.x - 16f, pos.y - 16f),
        size = Size(32f, 32f),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 2.5f)
    )
    drawCircle(color = Color.White, radius = 5.5f, center = pos)

    val measured = textMeasurer.measure(
        text = label,
        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )
    val padX = 12f
    val boxW = measured.size.width + padX
    val boxH = measured.size.height + 6f
    val topLeft = Offset(pos.x - boxW / 2f, pos.y - 16f - boxH - 6f)
    drawRoundRect(
        color = Color(0xE60B0E14),
        topLeft = topLeft,
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(topLeft.x + padX / 2f, topLeft.y + 3f)
    )
}

private fun DrawScope.drawCustomerMarker(
    pos: Offset,
    name: String,
    speedLabel: String,
    color: Color,
    textMeasurer: TextMeasurer
) {
    drawCircle(color = color.copy(alpha = 0.25f), radius = 22f, center = pos)
    drawCircle(color = color, radius = 14f, center = pos)
    drawCircle(color = Color.White, radius = 14f, center = pos, style = Stroke(width = 2.5f))
    drawCircle(color = Color.White, radius = 4.5f, center = pos)

    // Customer name pill on the right (matching "Nia" in screenshot 6)
    val nameMeasured = textMeasurer.measure(
        text = name,
        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    )
    val nameBoxW = nameMeasured.size.width + 14f
    val nameBoxH = nameMeasured.size.height + 6f
    val nameTL = Offset(pos.x + 18f, pos.y - nameBoxH / 2f)
    drawRoundRect(
        color = Color(0xE60B0E14),
        topLeft = nameTL,
        size = Size(nameBoxW, nameBoxH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawText(nameMeasured, topLeft = Offset(nameTL.x + 7f, nameTL.y + 3f))

    // Speed badge above marker if online
    if (speedLabel.isNotBlank()) {
        val speedMeasured = textMeasurer.measure(
            text = speedLabel,
            style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
        val sBoxW = speedMeasured.size.width + 10f
        val sBoxH = speedMeasured.size.height + 4f
        val sTL = Offset(pos.x - sBoxW / 2f, pos.y - 16f - sBoxH - 4f)
        drawRoundRect(
            color = color.copy(alpha = 0.90f),
            topLeft = sTL,
            size = Size(sBoxW, sBoxH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawText(speedMeasured, topLeft = Offset(sTL.x + 5f, sTL.y + 2f))
    }
}
