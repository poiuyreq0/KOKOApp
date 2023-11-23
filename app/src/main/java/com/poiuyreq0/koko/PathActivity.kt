package com.poiuyreq0.koko

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.FusedLocationSource
import com.poiuyreq0.koko.databinding.ActivityPathBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class PathActivity : AppCompatActivity(), OnMapReadyCallback, OnItemClickListener, AdapterView.OnItemSelectedListener {

    private lateinit var binding: ActivityPathBinding

    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap

    private lateinit var locationOverlay: LocationOverlay

    private var path = PathOverlay()

    private lateinit var cafes: List<Cafe>
    private lateinit var markers: MutableList<Marker>

    private var items: MutableMap<String, Item> = mutableMapOf()

    private val apiKeyID = BuildConfig.API_KEY_ID
    private val apiKey = BuildConfig.API_KEY

    private var spinnerFlag = false
    private var posFlag = 0

    private lateinit var locationChangeListener: NaverMap.OnLocationChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPathBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(binding.mapFragment.id) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(binding.mapFragment.id, it).commit()
            }
        mapFragment.getMapAsync(this)

        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        ArrayAdapter.createFromResource(
            this,
            R.array.spinner_item,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinner.adapter = adapter
        }
        binding.spinner.onItemSelectedListener = this

        binding.refreshButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                listRefresh()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions,
                grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        naverMap.locationSource = locationSource

        locationOverlay = naverMap.locationOverlay
        locationOverlay.isVisible = true
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        naverMap.mapType = NaverMap.MapType.Basic
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, true)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, false)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, false)

        val uiSettings = naverMap.uiSettings
        uiSettings.isLocationButtonEnabled = true

        locationChangeListener = NaverMap.OnLocationChangeListener { location ->
            naverMap.removeOnLocationChangeListener(locationChangeListener)

            CoroutineScope(Dispatchers.Main).launch {
                listRefresh()
                naverMap.moveCamera(CameraUpdate.zoomTo(12.0))
            }
        }
        naverMap.addOnLocationChangeListener(locationChangeListener)
    }

    private suspend fun detectionPath(name: String, endLat: Double, endLng: Double, startLat: Double, startLng: Double, isDraw: Boolean) {

        try {
            val resultTraoptimals = withContext(Dispatchers.IO) {
                val baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction-15/"
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val naverMapAPI = retrofit.create(NaverMapAPI::class.java)

                val startLocation = "${startLng},${startLat}"
                val goalLocation = "${endLng},${endLat}"
                val option = "traoptimal"

                val call = naverMapAPI.getPath(apiKeyID, apiKey, startLocation, goalLocation, option)
                val response = call.execute()

                response.body()?.route?.traoptimal ?: emptyList()
            }

            withContext(Dispatchers.Main) {
                path.map = null

                if (isDraw) {
                    // Draw path
                    Log.d("detectionPath", "Draw path")
                    val pathContainer: MutableList<LatLng> = mutableListOf()
                    for (resultTraoptimal in resultTraoptimals) {
                        for (resultRoutePath in resultTraoptimal.path) {
                            pathContainer.add(LatLng(resultRoutePath[1], resultRoutePath[0]))
                        }
                    }
                    path.coords = pathContainer
                    path.color = Color.BLUE
                    path.map = naverMap

                    if (path.coords != null) {
                        val cameraUpdate = CameraUpdate.scrollTo(path.coords[0])
                            .animate(CameraAnimation.Fly, 1000)
                        naverMap.moveCamera(cameraUpdate)
                    }

                } else {
                    // Process data
                    Log.d("detectionPath", "Process data")
                    for (resultTraoptimal in resultTraoptimals) {
                        items[name]?.apply {
                            distance = resultTraoptimal.summary.distance
                            duration = resultTraoptimal.summary.duration
                            lat = endLat
                            lng = endLng
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("detectionPath", "Error in detectionPath", e)
        }
    }

    private suspend fun detectionPaths(markers: MutableList<Marker>) {
        val startLat = locationOverlay.position.latitude
        val startLng = locationOverlay.position.longitude
        val marker = markers[0]
        Log.d("locationChangeListener", "detectionPaths Inside")
        val jobs = markers.map { marker ->
            CoroutineScope(Dispatchers.Main).async {
                detectionPath(marker.captionText, marker.position.latitude, marker.position.longitude, startLat, startLng, false)
            }
        }
        jobs.awaitAll()
    }

    private suspend fun findAll(): List<Cafe> {
        var cafes: List<Cafe> = emptyList()

        val baseUrl = "http://ec2-3-34-131-7.ap-northeast-2.compute.amazonaws.com/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val cafesAPI = retrofit.create(CafesAPI::class.java)

        val response = cafesAPI.findAllApi().execute()
        cafes = response.body() ?: emptyList()

        for (cafe in cafes) {
            items[cafe.name] = Item(
                cafe.name,
                0,
                0,
                (cafe.positions.count { it >= 1L }.toDouble() / cafe.positions.size * 100).roundToInt(),
                cafe.coordinate.latitude,
                cafe.coordinate.longitude
            )
        }

        return cafes
    }

    private fun updateRecyclerview(posFlag: Int) {
        when (posFlag) {
            0 -> {
                val sortedItems = items.toList().sortedBy { (_, value) -> value.distance }.toMap().toMutableMap()
                binding.recyclerview.adapter = RecyclerViewAdapter(sortedItems, posFlag, this)
            }
            1 -> {
                val sortedItems = items.toList().sortedBy { (_, value) -> value.duration }.toMap().toMutableMap()
                binding.recyclerview.adapter = RecyclerViewAdapter(sortedItems, posFlag, this)
            }
            else -> {
                val sortedItems = items.toList().sortedBy { (_, value) -> value.congestion }.toMap().toMutableMap()
                binding.recyclerview.adapter = RecyclerViewAdapter(sortedItems, posFlag, this)
            }
        }
    }

    private suspend fun listRefresh() {
        binding.refreshButton.isEnabled = false

        items = mutableMapOf()

        val job = CoroutineScope(Dispatchers.Main).async {
            cafes = withContext(Dispatchers.IO) {
                findAll()
            }

            markers = withContext(Dispatchers.Main) {
                addMarkers(cafes)
            }

            detectionPaths(markers)

            updateRecyclerview(posFlag)
        }
        job.await()

        binding.refreshButton.isEnabled = true
    }

    private suspend fun addMarkers(cafes: List<Cafe>): MutableList<Marker> {
        var markers: MutableList<Marker> = mutableListOf()

        for (i in cafes.indices) {
            markers.add(Marker())
            markers[i].position = LatLng(cafes[i].coordinate.latitude, cafes[i].coordinate.longitude)
            markers[i].captionText = cafes[i].name
            markers[i].captionTextSize = 16f
            markers[i].map = naverMap
            markers[i].onClickListener = markerClickListener
        }

        return markers
    }

    private val markerClickListener = Overlay.OnClickListener { o ->
        val m: Marker = o as Marker
        val name = m.captionText
        val endLat = m.position.latitude
        val endLng = m.position.longitude
        val startLat = locationOverlay.position.latitude
        val startLng = locationOverlay.position.longitude

        CoroutineScope(Dispatchers.Main).async {
            detectionPath(name, endLat, endLng, startLat, startLng, true)
        }

        true
    }

    override fun onItemClick(position: Int, dataSet: MutableMap<String, Item>) {
        val dataSetList = dataSet.values.toList()

        val name = dataSetList[position].name
        val endLat = dataSetList[position].lat
        val endLng = dataSetList[position].lng
        val startLat = locationOverlay.position.latitude
        val startLng = locationOverlay.position.longitude

        CoroutineScope(Dispatchers.Main).async {
            detectionPath(name, endLat, endLng, startLat, startLng, true)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        if (!spinnerFlag) {
            spinnerFlag = true
            return
        }

        posFlag = pos
        updateRecyclerview(posFlag)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}
