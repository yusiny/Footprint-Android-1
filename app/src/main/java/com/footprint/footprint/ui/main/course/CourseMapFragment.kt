package com.footprint.footprint.ui.main.course

import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import com.footprint.footprint.R
import com.footprint.footprint.data.dto.CourseDTO
import com.footprint.footprint.databinding.FragmentCourseMapBinding
import com.footprint.footprint.domain.model.BoundsModel
import com.footprint.footprint.ui.BaseFragment
import com.footprint.footprint.ui.main.course.Filtering.filterState
import com.footprint.footprint.utils.LogUtils
import com.footprint.footprint.utils.SEARCH_IN
import com.footprint.footprint.utils.SEARCH_IN_MAP
import com.footprint.footprint.utils.SEARCH_IN_MY_LOCATION
import com.footprint.footprint.viewmodel.CourseViewModel
import com.google.android.gms.location.*
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CourseMapFragment() :
    BaseFragment<FragmentCourseMapBinding>(FragmentCourseMapBinding::inflate), OnMapReadyCallback{

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var locationOverlay: LocationOverlay

    private lateinit var map: NaverMap
    private val markerList = arrayListOf<Marker>()
    private var isCameraInitialized = false // 카메라 현위치로 이동했는지 확인하는 변수

    private val courseVm: CourseViewModel by sharedViewModel()

    override fun initAfterBinding() {
        initMap()
        initClickListener()
        observe()
    }

    private fun initClickListener() {
        binding.courseMapCurrentLocationIv.setOnClickListener {
            setCameraPositionToCurrent() // 현위치로 카메라 이동
        }
    }

    /* 지도 */
    private fun initMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.course_map_fragment) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction().add(R.id.walkmap_map_fragment, it)
                        .commit()
                }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(naverMap: NaverMap) {
        map = naverMap

        setMap()
        initMapEvent()
        locationActivate()
    }

    private fun setMap() {
        map.uiSettings.isZoomControlEnabled = false
        map.uiSettings.isCompassEnabled = false

        locationOverlay = map.locationOverlay
        locationOverlay.apply {
            icon = OverlayImage.fromResource(R.drawable.ic_location_overlay)
            anchor = PointF(0.5f, 0.5f)
            subIcon = null
        }

        // 이동해야 할 카메가 위치가 있는 경우,
        if(lastCameraPosition != null){
            setCameraPosition(lastCameraPosition!!)
            isCameraInitialized = true
        }
    }

    private fun initMapEvent() {
        map.addOnCameraChangeListener { reason, _ ->
            when (reason) {
                CameraUpdate.REASON_DEVELOPER -> {
                    binding.courseMapCurrentLocationIv.alpha = 1F
                }
                CameraUpdate.REASON_GESTURE -> {
                    binding.courseMapCurrentLocationIv.alpha = 0.5F
                }
            }

            val bounds = BoundsModel(
                map.contentBounds.southWest,
                map.contentBounds.southEast,
                map.contentBounds.northWest,
                map.contentBounds.northEast
            )
            courseVm.setMapBounds(bounds)
        }
    }

    private fun observe() {
        courseVm.filteredCourseList.observe(requireActivity(), Observer {
            // 기존 마커 다 지우기
            // clearMarkers()

            // 정렬된 리스트가 바뀌면 새로운 마커들을 띄워준다
            // addMarkers()
        })
    }

    /* 현위치 */
    private fun locationActivate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 2000L // 위치 업데이트 주기
            fastestInterval = 1000L // 가장 빠른 위치 업데이트 주기
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // 배터리 소모를 고려하지 않으며 정확도를 최우선으로 고려
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)

            result.lastLocation.let { location ->

                binding.courseLoadingBgV.visibility = View.GONE
                binding.courseLoadingPb.visibility = View.GONE

                currentLocation = location
                val position = LatLng(location)

                // 처음에만 카메라 이동
                if (!isCameraInitialized) {
                    map.moveCamera(CameraUpdate.scrollTo(position))

                    isCameraInitialized = true
                }

                // 현위치 오버레이
                if(!locationOverlay.isVisible) locationOverlay.isVisible = true
                locationOverlay.apply {
                    this.position = position
                    bearing = location.bearing
                }

                // 현위치 bounds
                val bounds = BoundsModel(
                    map.contentBounds.southWest,
                    map.contentBounds.southEast,
                    map.contentBounds.northWest,
                    map.contentBounds.northEast
                )
                courseVm.setCurrentBounds(bounds)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    /* 마커 관련 */
    private fun addMarker(courseList: ArrayList<CourseDTO>){
        if(courseList.isEmpty())
            return

        for(course in courseList){
            val marker = Marker()
            marker.position = LatLng(course.startLat, course.startLong)
            marker.map = map
            marker.icon = OverlayImage.fromResource(R.drawable.ic_location_pin_start)

            markerList.add(marker)
        }
    }

    private fun updateMarkers(courseList: ArrayList<CourseDTO>){
        if(markerList.isEmpty() || courseList.isEmpty())
            return

        for(i in markerList.indices){
            markerList[i].position = LatLng(courseList[i].startLat, courseList[i].startLong)
            markerList[i].icon = OverlayImage.fromResource(R.drawable.ic_location_pin_start)
        }
    }

    private fun clearMarkers(){
        if(markerList.isEmpty())
            return

        for(marker in markerList){
            marker.map = null
        }
    }

    /* public */
     var lastCameraPosition: CameraPosition? = null // 코스 검색 액티비티에서 돌아왔을 때, 동기화할 카메라 위치

    fun getCameraPosition(): CameraPosition? {
        if(!::map.isInitialized)
            return null

        return map.cameraPosition
    }

    fun setCameraPositionToCurrent(){
        if(::map.isInitialized && ::currentLocation.isInitialized)
            map.moveCamera(CameraUpdate.scrollTo(LatLng(currentLocation)).animate(CameraAnimation.Easing))
    }

    fun setCameraPosition(cameraPosition: CameraPosition){
        map.moveCamera(CameraUpdate.toCameraPosition(cameraPosition))
        lastCameraPosition = null
    }

}