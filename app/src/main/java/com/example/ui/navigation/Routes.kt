package com.example.ui.navigation

object Routes {
    // 4 Bottom Navigation Tabs (matching screenshot)
    const val ROUTE_HOME = "home"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_ALERTS = "alerts"
    const val ROUTE_SETTINGS = "settings"

    // 14 Module Destinations + NOC Monitoring
    const val ROUTE_MONITORING = "monitoring"
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_PPPOE = "pppoe"
    const val ROUTE_TAGIHAN = "tagihan"
    const val ROUTE_TRANSAKSI = "transaksi"
    const val ROUTE_ROUTERS = "routers"
    const val ROUTE_PRODUK = "produk"
    const val ROUTE_AREA_MAPS = "area_maps"
    const val ROUTE_FTTH_LOKASI = "ftth_lokasi"
    const val ROUTE_FTTH_TOPOLOGI = "ftth_topologi"
    const val ROUTE_FTTH_ZONA = "ftth_zona"
    const val ROUTE_MAPS_FTTH = "maps_ftth"
    const val ROUTE_GENIEACS = "genieacs"
    const val ROUTE_RADIUS = "radius"
    const val ROUTE_LAPORAN_PAJAK = "laporan_pajak"
    const val ROUTE_SALES = "sales"
    const val ROUTE_LOG_AKTIVITAS = "log_aktivitas"
    const val ROUTE_TIKET = "tiket"

    const val ROUTE_ROUTER_DETAIL = "router_detail/{routerId}"

    fun routerDetail(routerId: Long): String = "router_detail/$routerId"
}
