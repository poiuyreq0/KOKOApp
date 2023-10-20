package com.poiuyreq0.koko

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatButton
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PathActivity : AppCompatActivity(), OnMapReadyCallback, OnItemClickListener {

    private lateinit var binding: ActivityPathBinding

    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap

    private lateinit var locationOverlay: LocationOverlay

    private var path = PathOverlay()

    private lateinit var cafes: List<Cafe>
    private lateinit var markers: MutableList<Marker>
    private var distances: MutableList<Item> = mutableListOf()
    private var durations: MutableList<Item> = mutableListOf()

    private val apiKeyID = BuildConfig.API_KEY_ID
    private val apiKey = BuildConfig.API_KEY

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

        val searchButton: AppCompatButton = binding.searchButton
        val distanceButton: AppCompatButton = binding.distanceButton
        val durationButton: AppCompatButton = binding.durationButton

        searchButton.setOnClickListener {
            searchButton.visibility = View.GONE
            distanceButton.visibility = View.VISIBLE
            durationButton.visibility = View.VISIBLE

            nonDrawDetectionPaths()
        }
        distanceButton.setOnClickListener {
            distances.sortBy { it.value }
            binding.recyclerview.adapter = RecyclerViewAdapter(distances, 0, this)
        }
        durationButton.setOnClickListener {
            durations.sortBy { it.value }
            binding.recyclerview.adapter = RecyclerViewAdapter(durations, 1, this)
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

        cafes = findAll()

//        naverMap.setOnMapClickListener { point, coord ->
//            Log.d("point", "${coord.latitude}, ${coord.longitude}")
//        }
    }

    private fun nonDrawDetectionPath(name: String, lat: Double, lng: Double) {
        path.map = null

        val baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction-15/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val naverMapAPI = retrofit.create(NaverMapAPI::class.java)

        val startLocation ="${locationOverlay.position.longitude},${locationOverlay.position.latitude}"
        val goalLocation = "${lng},${lat}"
        val option = "traoptimal"

        val call = naverMapAPI.getPath(apiKeyID, apiKey, startLocation, goalLocation, option)

        call.enqueue(object: Callback<NaverMapAPIResult.ResultResponse> {
            override fun onResponse(
                call: Call<NaverMapAPIResult.ResultResponse>,
                response: Response<NaverMapAPIResult.ResultResponse>
            ) {
                val resultTraoptimals = response.body()?.route?.traoptimal ?: emptyList()
//                val pathContainer: MutableList<LatLng> = mutableListOf()
                for (resultTraoptimal in resultTraoptimals) {
                    distances.add(Item(name, resultTraoptimal.summary.distance, lat, lng))
                    durations.add(Item(name, resultTraoptimal.summary.duration, lat, lng))

//                    for (resultRoutePath in resultTraoptimal.path) {
//                        pathContainer.add(LatLng(resultRoutePath[1], resultRoutePath[0]))
//                    }
                }
//                path.coords = pathContainer
//                path.color = Color.BLUE
//                path.map = naverMap
//
//                if (path.coords != null) {
//                    val cameraUpdate = CameraUpdate.scrollTo(path.coords[0])
//                        .animate(CameraAnimation.Fly, 1000)
//                    naverMap.moveCamera(cameraUpdate)
//                }
            }

            override fun onFailure(call: Call<NaverMapAPIResult.ResultResponse>, t: Throwable) {
                Log.e("detectionPath", "onFailure", t)
            }
        })
    }

    private fun detectionPath(name: String, lat: Double, lng: Double) {
        path.map = null

        val baseUrl = "https://naveropenapi.apigw.ntruss.com/map-direction-15/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val naverMapAPI = retrofit.create(NaverMapAPI::class.java)

        val startLocation ="${locationOverlay.position.longitude},${locationOverlay.position.latitude}"
        val goalLocation = "${lng},${lat}"
        val option = "traoptimal"

        val call = naverMapAPI.getPath(apiKeyID, apiKey, startLocation, goalLocation, option)

        call.enqueue(object: Callback<NaverMapAPIResult.ResultResponse> {
            override fun onResponse(
                call: Call<NaverMapAPIResult.ResultResponse>,
                response: Response<NaverMapAPIResult.ResultResponse>
            ) {
                val resultTraoptimals = response.body()?.route?.traoptimal ?: emptyList()
                val pathContainer: MutableList<LatLng> = mutableListOf()
                for (resultTraoptimal in resultTraoptimals) {
//                    distances.add(Item(name, resultTraoptimal.summary.distance, lat, lng))
//                    durations.add(Item(name, resultTraoptimal.summary.duration, lat, lng))

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
            }

            override fun onFailure(call: Call<NaverMapAPIResult.ResultResponse>, t: Throwable) {
                Log.e("detectionPath", "onFailure", t)
            }
        })
    }

    private fun nonDrawDetectionPaths() {
        for (marker in markers) {
            nonDrawDetectionPath(marker.captionText, marker.position.latitude, marker.position.longitude)
        }
    }

    private fun findAll(): List<Cafe> {
        var cafes: List<Cafe> = emptyList()

        val baseUrl = "http://ec2-43-202-59-190.ap-northeast-2.compute.amazonaws.com:8080/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val cafesAPI = retrofit.create(CafesAPI::class.java)

        val call = cafesAPI.findAllApi()

        call.enqueue(object: Callback<List<Cafe>> {
            override fun onResponse(
                call: Call<List<Cafe>>,
                response: Response<List<Cafe>>
            ) {
                cafes = response.body() ?: emptyList()
                addMarkers(cafes)
            }

            override fun onFailure(call: Call<List<Cafe>>, t: Throwable) {
                Log.e("findAll", "onFailure", t)
            }
        })

        return cafes
    }

    private fun findByRadius(): List<Cafe> {
        var cafes: List<Cafe> = emptyList()

        val baseUrl = "http://ec2-43-202-59-190.ap-northeast-2.compute.amazonaws.com:8080/"

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val cafesAPI = retrofit.create(CafesAPI::class.java)

        val userLatitude = locationOverlay.position.latitude
        val userLongitude = locationOverlay.position.longitude
        val radius = 10.0

        val call = cafesAPI.findByRadiusApi(userLatitude, userLongitude, radius)

        call.enqueue(object: Callback<List<Cafe>> {
            override fun onResponse(
                call: Call<List<Cafe>>,
                response: Response<List<Cafe>>
            ) {
                cafes = response.body() ?: emptyList()
                addMarkers(cafes)
            }

            override fun onFailure(call: Call<List<Cafe>>, t: Throwable) {
                Log.e("findByRadius", "onFailure", t)
            }
        })

        return cafes
    }

    private fun addMarkers(cafes: List<Cafe>) {
        var markers: MutableList<Marker> = mutableListOf()

        for (i in cafes.indices) {
            markers.add(Marker())
            markers[i].position = LatLng(cafes[i].coordinate.latitude, cafes[i].coordinate.longitude)
            markers[i].captionText = cafes[i].name
            markers[i].map = naverMap
            markers[i].onClickListener = markerClickListener
        }

        this.markers = markers
    }

    private val markerClickListener = Overlay.OnClickListener { o ->
        val m: Marker = o as Marker

        detectionPath(m.captionText, m.position.latitude, m.position.longitude)

        true
    }

    override fun onItemClick(position: Int, dataSet: MutableList<Item>) {
        val name = dataSet[position].name
        val lat = dataSet[position].lat
        val lng = dataSet[position].lng

        detectionPath(name, lat, lng)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}
