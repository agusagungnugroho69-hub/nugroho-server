package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class WilayahItem(
    val id: String,
    val name: String
)

object IndonesiaWilayahRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val regencyCache = ConcurrentHashMap<String, List<WilayahItem>>()
    private val districtCache = ConcurrentHashMap<String, List<WilayahItem>>()
    private val villageCache = ConcurrentHashMap<String, List<WilayahItem>>()
    @Volatile
    private var liveProvincesMap: Map<String, String>? = null

    val provinces: List<WilayahItem> = listOf(
        WilayahItem("11", "ACEH"),
        WilayahItem("12", "SUMATERA UTARA"),
        WilayahItem("13", "SUMATERA BARAT"),
        WilayahItem("14", "RIAU"),
        WilayahItem("15", "JAMBI"),
        WilayahItem("16", "SUMATERA SELATAN"),
        WilayahItem("17", "BENGKULU"),
        WilayahItem("18", "LAMPUNG"),
        WilayahItem("19", "KEPULAUAN BANGKA BELITUNG"),
        WilayahItem("21", "KEPULAUAN RIAU"),
        WilayahItem("31", "DAERAH KHUSUS IBUKOTA JAKARTA"),
        WilayahItem("32", "JAWA BARAT"),
        WilayahItem("33", "JAWA TENGAH"),
        WilayahItem("34", "DAERAH ISTIMEWA YOGYAKARTA"),
        WilayahItem("35", "JAWA TIMUR"),
        WilayahItem("36", "BANTEN"),
        WilayahItem("51", "BALI"),
        WilayahItem("52", "NUSA TENGGARA BARAT"),
        WilayahItem("53", "NUSA TENGGARA TIMUR"),
        WilayahItem("61", "KALIMANTAN BARAT"),
        WilayahItem("62", "KALIMANTAN TENGAH"),
        WilayahItem("63", "KALIMANTAN SELATAN"),
        WilayahItem("64", "KALIMANTAN TIMUR"),
        WilayahItem("65", "KALIMANTAN UTARA"),
        WilayahItem("71", "SULAWESI UTARA"),
        WilayahItem("72", "SULAWESI TENGAH"),
        WilayahItem("73", "SULAWESI SELATAN"),
        WilayahItem("74", "SULAWESI TENGGARA"),
        WilayahItem("75", "GORONTALO"),
        WilayahItem("76", "SULAWESI BARAT"),
        WilayahItem("81", "MALUKU"),
        WilayahItem("82", "MALUKU UTARA"),
        WilayahItem("91", "PAPUA BARAT"),
        WilayahItem("92", "PAPUA BARAT DAYA"),
        WilayahItem("93", "PAPUA SELATAN"),
        WilayahItem("94", "PAPUA"),
        WilayahItem("95", "PAPUA TENGAH"),
        WilayahItem("96", "PAPUA PEGUNUNGAN")
    )

    private suspend fun ensureLiveProvinceIds() {
        if (liveProvincesMap != null) return
        val remote = fetchJsonList("https://www.emsifa.com/api-wilayah-indonesia/api/provinces.json")
        if (remote.isNotEmpty()) {
            liveProvincesMap = remote.associate { item ->
                item.name.uppercase().trim() to item.id
            }
        }
    }

    suspend fun getRegencies(province: WilayahItem): List<WilayahItem> {
        regencyCache[province.id]?.let { return it }
        ensureLiveProvinceIds()
        val liveId = liveProvincesMap?.get(province.name.uppercase().trim())
            ?: liveProvincesMap?.entries?.firstOrNull { (k, _) ->
                k.contains(province.name.uppercase().removePrefix("DAERAH KHUSUS IBUKOTA ").removePrefix("DAERAH ISTIMEWA "))
            }?.value
            ?: province.id

        val remote = fetchJsonList("https://www.emsifa.com/api-wilayah-indonesia/api/regencies/$liveId.json")
        val result = if (remote.isNotEmpty()) remote else fallbackRegencies[province.id].orEmpty()
        if (result.isNotEmpty()) {
            regencyCache[province.id] = result
        }
        return result
    }

    suspend fun getDistricts(regency: WilayahItem): List<WilayahItem> {
        districtCache[regency.id]?.let { return it }
        val remote = fetchJsonList("https://www.emsifa.com/api-wilayah-indonesia/api/districts/${regency.id}.json")
        val result = if (remote.isNotEmpty()) remote else fallbackDistricts[regency.id].orEmpty()
        if (result.isNotEmpty()) {
            districtCache[regency.id] = result
        }
        return result
    }

    suspend fun getVillages(district: WilayahItem): List<WilayahItem> {
        villageCache[district.id]?.let { return it }
        val remote = fetchJsonList("https://www.emsifa.com/api-wilayah-indonesia/api/villages/${district.id}.json")
        val result = if (remote.isNotEmpty()) remote else fallbackVillages[district.id].orEmpty()
        if (result.isNotEmpty()) {
            villageCache[district.id] = result
        }
        return result
    }

    private suspend fun fetchJsonList(url: String): List<WilayahItem> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext emptyList()
                val arr = JSONArray(body)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val name = obj.optString("name")
                        if (id.isNotBlank() && name.isNotBlank()) {
                            add(WilayahItem(id, name.uppercase()))
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private val fallbackRegencies: Map<String, List<WilayahItem>> = mapOf(
        "11" to listOf(
            WilayahItem("1101", "KABUPATEN SIMEULUE"),
            WilayahItem("1108", "KABUPATEN ACEH BESAR"),
            WilayahItem("1111", "KABUPATEN BIREUEN"),
            WilayahItem("1171", "KOTA BANDA ACEH"),
            WilayahItem("1174", "KOTA LHOKSEUMAWE")
        ),
        "12" to listOf(
            WilayahItem("1207", "KABUPATEN DELI SERDANG"),
            WilayahItem("1213", "KABUPATEN LANGKAT"),
            WilayahItem("1271", "KOTA MEDAN"),
            WilayahItem("1273", "KOTA PEMATANGSIANTAR"),
            WilayahItem("1275", "KOTA BINJAI")
        ),
        "13" to listOf(
            WilayahItem("1306", "KABUPATEN AGAM"),
            WilayahItem("1308", "KABUPATEN LIMA PULUH KOTA"),
            WilayahItem("1371", "KOTA PADANG"),
            WilayahItem("1375", "KOTA BUKITTINGGI")
        ),
        "14" to listOf(
            WilayahItem("1401", "KABUPATEN KUANTAN SINGINGI"),
            WilayahItem("1406", "KABUPATEN KAMPAR"),
            WilayahItem("1471", "KOTA PEKANBARU"),
            WilayahItem("1473", "KOTA DUMAI")
        ),
        "15" to listOf(
            WilayahItem("1505", "KABUPATEN MUARO JAMBI"),
            WilayahItem("1571", "KOTA JAMBI")
        ),
        "16" to listOf(
            WilayahItem("1603", "KABUPATEN MUARA ENIM"),
            WilayahItem("1607", "KABUPATEN BANYUASIN"),
            WilayahItem("1671", "KOTA PALEMBANG")
        ),
        "17" to listOf(
            WilayahItem("1702", "KABUPATEN REJANG LEBONG"),
            WilayahItem("1771", "KOTA BENGKULU")
        ),
        "18" to listOf(
            WilayahItem("1801", "KABUPATEN LAMPUNG BARAT"),
            WilayahItem("1803", "KABUPATEN LAMPUNG SELATAN"),
            WilayahItem("1805", "KABUPATEN LAMPUNG TENGAH"),
            WilayahItem("1871", "KOTA BANDAR LAMPUNG"),
            WilayahItem("1872", "KOTA METRO")
        ),
        "19" to listOf(
            WilayahItem("1901", "KABUPATEN BANGKA"),
            WilayahItem("1971", "KOTA PANGKAL PINANG")
        ),
        "21" to listOf(
            WilayahItem("2101", "KABUPATEN BINTAN"),
            WilayahItem("2171", "KOTA BATAM"),
            WilayahItem("2172", "KOTA TANJUNG PINANG")
        ),
        "31" to listOf(
            WilayahItem("3101", "KABUPATEN KEPULAUAN SERIBU"),
            WilayahItem("3171", "KOTA JAKARTA SELATAN"),
            WilayahItem("3172", "KOTA JAKARTA TIMUR"),
            WilayahItem("3173", "KOTA JAKARTA PUSAT"),
            WilayahItem("3174", "KOTA JAKARTA BARAT"),
            WilayahItem("3175", "KOTA JAKARTA UTARA")
        ),
        "32" to listOf(
            WilayahItem("3201", "KABUPATEN BOGOR"),
            WilayahItem("3204", "KABUPATEN BANDUNG"),
            WilayahItem("3209", "KABUPATEN CIREBON"),
            WilayahItem("3215", "KABUPATEN KARAWANG"),
            WilayahItem("3216", "KABUPATEN BEKASI"),
            WilayahItem("3271", "KOTA BOGOR"),
            WilayahItem("3273", "KOTA BANDUNG"),
            WilayahItem("3275", "KOTA BEKASI"),
            WilayahItem("3276", "KOTA DEPOK")
        ),
        "33" to listOf(
            WilayahItem("3301", "KABUPATEN CILACAP"),
            WilayahItem("3302", "KABUPATEN BANYUMAS"),
            WilayahItem("3303", "KABUPATEN PURBALINGGA"),
            WilayahItem("3304", "KABUPATEN BANJARNEGARA"),
            WilayahItem("3305", "KABUPATEN KEBUMEN"),
            WilayahItem("3306", "KABUPATEN PURWOREJO"),
            WilayahItem("3307", "KABUPATEN WONOSOBO"),
            WilayahItem("3308", "KABUPATEN MAGELANG"),
            WilayahItem("3309", "KABUPATEN BOYOLALI"),
            WilayahItem("3310", "KABUPATEN KLATEN"),
            WilayahItem("3311", "KABUPATEN SUKOHARJO"),
            WilayahItem("3312", "KABUPATEN WONOGIRI"),
            WilayahItem("3313", "KABUPATEN KARANGANYAR"),
            WilayahItem("3314", "KABUPATEN SRAGEN"),
            WilayahItem("3315", "KABUPATEN GROBOGAN"),
            WilayahItem("3316", "KABUPATEN BLORA"),
            WilayahItem("3317", "KABUPATEN REMBANG"),
            WilayahItem("3318", "KABUPATEN PATI"),
            WilayahItem("3319", "KABUPATEN KUDUS"),
            WilayahItem("3320", "KABUPATEN JEPARA"),
            WilayahItem("3321", "KABUPATEN DEMAK"),
            WilayahItem("3322", "KABUPATEN SEMARANG"),
            WilayahItem("3323", "KABUPATEN TEMANGGUNG"),
            WilayahItem("3324", "KABUPATEN KENDAL"),
            WilayahItem("3325", "KABUPATEN BATANG"),
            WilayahItem("3326", "KABUPATEN PEKALONGAN"),
            WilayahItem("3327", "KABUPATEN PEMALANG"),
            WilayahItem("3328", "KABUPATEN TEGAL"),
            WilayahItem("3329", "KABUPATEN BREBES"),
            WilayahItem("3371", "KOTA MAGELANG"),
            WilayahItem("3372", "KOTA SURAKARTA"),
            WilayahItem("3373", "KOTA SALATIGA"),
            WilayahItem("3374", "KOTA SEMARANG"),
            WilayahItem("3375", "KOTA PEKALONGAN"),
            WilayahItem("3376", "KOTA TEGAL")
        ),
        "34" to listOf(
            WilayahItem("3401", "KABUPATEN KULON PROGO"),
            WilayahItem("3402", "KABUPATEN BANTUL"),
            WilayahItem("3403", "KABUPATEN GUNUNG KIDUL"),
            WilayahItem("3404", "KABUPATEN SLEMAN"),
            WilayahItem("3471", "KOTA YOGYAKARTA")
        ),
        "35" to listOf(
            WilayahItem("3507", "KABUPATEN MALANG"),
            WilayahItem("3515", "KABUPATEN SIDOARJO"),
            WilayahItem("3522", "KABUPATEN BOJONEGORO"),
            WilayahItem("3523", "KABUPATEN TUBAN"),
            WilayahItem("3524", "KABUPATEN LAMONGAN"),
            WilayahItem("3525", "KABUPATEN GRESIK"),
            WilayahItem("3573", "KOTA MALANG"),
            WilayahItem("3578", "KOTA SURABAYA")
        ),
        "36" to listOf(
            WilayahItem("3603", "KABUPATEN TANGERANG"),
            WilayahItem("3604", "KABUPATEN SERANG"),
            WilayahItem("3671", "KOTA TANGERANG"),
            WilayahItem("3673", "KOTA SERANG"),
            WilayahItem("3674", "KOTA TANGERANG SELATAN")
        ),
        "51" to listOf(
            WilayahItem("5103", "KABUPATEN BADUNG"),
            WilayahItem("5104", "KABUPATEN GIANYAR"),
            WilayahItem("5171", "KOTA DENPASAR")
        ),
        "52" to listOf(
            WilayahItem("5201", "KABUPATEN LOMBOK BARAT"),
            WilayahItem("5271", "KOTA MATARAM")
        ),
        "53" to listOf(
            WilayahItem("5303", "KABUPATEN KUPANG"),
            WilayahItem("5371", "KOTA KUPANG")
        ),
        "61" to listOf(
            WilayahItem("6112", "KABUPATEN KUBU RAYA"),
            WilayahItem("6171", "KOTA PONTIANAK"),
            WilayahItem("6172", "KOTA SINGKAWANG")
        ),
        "62" to listOf(
            WilayahItem("6202", "KABUPATEN KOTAWARINGIN TIMUR"),
            WilayahItem("6271", "KOTA PALANGKA RAYA")
        ),
        "63" to listOf(
            WilayahItem("6303", "KABUPATEN BANJAR"),
            WilayahItem("6371", "KOTA BANJARMASIN"),
            WilayahItem("6372", "KOTA BANJARBARU")
        ),
        "64" to listOf(
            WilayahItem("6403", "KABUPATEN KUTAI KARTANEGARA"),
            WilayahItem("6471", "KOTA BALIKPAPAN"),
            WilayahItem("6472", "KOTA SAMARINDA")
        ),
        "65" to listOf(
            WilayahItem("6502", "KABUPATEN BULUNGAN"),
            WilayahItem("6571", "KOTA TARAKAN")
        ),
        "71" to listOf(
            WilayahItem("7102", "KABUPATEN MINAHASA"),
            WilayahItem("7171", "KOTA MANADO")
        ),
        "72" to listOf(
            WilayahItem("7210", "KABUPATEN SIGI"),
            WilayahItem("7271", "KOTA PALU")
        ),
        "73" to listOf(
            WilayahItem("7306", "KABUPATEN GOWA"),
            WilayahItem("7371", "KOTA MAKASSAR")
        ),
        "74" to listOf(
            WilayahItem("7405", "KABUPATEN KONAWE SELATAN"),
            WilayahItem("7471", "KOTA KENDARI")
        ),
        "75" to listOf(
            WilayahItem("7502", "KABUPATEN GORONTALO"),
            WilayahItem("7571", "KOTA GORONTALO")
        ),
        "76" to listOf(
            WilayahItem("7604", "KABUPATEN MAMUJU")
        ),
        "81" to listOf(
            WilayahItem("8101", "KABUPATEN MALUKU TENGAH"),
            WilayahItem("8171", "KOTA AMBON")
        ),
        "82" to listOf(
            WilayahItem("8271", "KOTA TERNATE")
        ),
        "91" to listOf(
            WilayahItem("9105", "KABUPATEN MANOKWARI")
        ),
        "92" to listOf(
            WilayahItem("9201", "KABUPATEN SORONG"),
            WilayahItem("9271", "KOTA SORONG")
        ),
        "93" to listOf(
            WilayahItem("9301", "KABUPATEN MERAUKE"),
            WilayahItem("9302", "KABUPATEN BOVEN DIGOEL")
        ),
        "94" to listOf(
            WilayahItem("9403", "KABUPATEN JAYAPURA"),
            WilayahItem("9471", "KOTA JAYAPURA")
        ),
        "95" to listOf(
            WilayahItem("9501", "KABUPATEN NABIRE"),
            WilayahItem("9504", "KABUPATEN MIMIKA")
        ),
        "96" to listOf(
            WilayahItem("9601", "KABUPATEN JAYAWIJAYA")
        )
    )

    private val fallbackDistricts: Map<String, List<WilayahItem>> = mapOf(
        "3316" to listOf(
            WilayahItem("3316010", "JATI"),
            WilayahItem("3316020", "RANDUBLATUNG"),
            WilayahItem("3316030", "KRADENAN"),
            WilayahItem("3316040", "KEDUNGTUBAN"),
            WilayahItem("3316050", "CEPU"),
            WilayahItem("3316060", "SAMBONG"),
            WilayahItem("3316070", "JIKEN"),
            WilayahItem("3316080", "BOGOREJO"),
            WilayahItem("3316090", "JEPON"),
            WilayahItem("3316100", "BLORA"),
            WilayahItem("3316110", "BANJAREJO"),
            WilayahItem("3316120", "TUNJUNGAN"),
            WilayahItem("3316130", "JAPAH"),
            WilayahItem("3316140", "NGAWEN"),
            WilayahItem("3316150", "KUNDURAN"),
            WilayahItem("3316160", "TODANAN")
        )
    )

    private val fallbackVillages: Map<String, List<WilayahItem>> = mapOf(
        "3316080" to listOf(
            WilayahItem("3316080001", "PRANTAAN"),
            WilayahItem("3316080002", "GANDU"),
            WilayahItem("3316080003", "SARIREJO"),
            WilayahItem("3316080004", "GAYAM"),
            WilayahItem("3316080005", "GEMPOLREJO"),
            WilayahItem("3316080006", "JERUK"),
            WilayahItem("3316080007", "KARANG"),
            WilayahItem("3316080008", "KARANGANYAR"),
            WilayahItem("3316080009", "NGAMPEL"),
            WilayahItem("3316080010", "NGLOBO"),
            WilayahItem("3316080011", "SENDANGREJO"),
            WilayahItem("3316080012", "TEMPURAN"),
            WilayahItem("3316080013", "TEMPUREJO"),
            WilayahItem("3316080014", "JURANGJERO")
        )
    )
}
