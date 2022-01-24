package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.se.omapi.SEService
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.internal.ConnectionCallbacks
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.dsl.koinApplication
import com.google.android.gms.maps.model.PointOfInterest
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    //private var locationRequest: LocationRequest? = null
    private var homeLatLng = LatLng(-34.0, 151.0)
    private var localpoi: PointOfInterest? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        binding.saveGeoLocation.setOnClickListener{
            onLocationSelected()
    }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)

//        TODO: add the map setup implementation


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as
                SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding.root

    }
//        TODO: zoom to the user location after taking his permission
    fun isPermissionGranted() : Boolean {
            return ContextCompat.checkSelfPermission(
        context!!,
        Manifest.permission.ACCESS_FINE_LOCATION).equals(PackageManager.PERMISSION_GRANTED)
}

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            Log.i("TAG", "It ENTERS onRequestPermissionsResult")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_LOCATION_PERMISSION) {
                if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.i("TAG", "ZZZ in onRequestPermissionsResult()")
                    enableMyLocation()
                }
            }
        }

        @SuppressLint("MissingPermission")
        fun enableMyLocation() {
            if (isPermissionGranted()) {
                map.setMyLocationEnabled(true)
                // place actions that need to be taken once permission is granted here:
                getDeviceLocation()
            }
            else {
                requestPermissions(
                    arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )

            }
        }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        Log.i("TAG", "ZZZ entered into getDeviceLocation()")
       try {
           if (isPermissionGranted()) {
               val locationResult = fusedLocationProviderClient?.lastLocation
               locationResult?.addOnCompleteListener(activity!!) { task ->
                   if (task.isSuccessful) {
                       Log.i("TAG", "ZZZ task was successful")
                       val lastKnownLocation = task.result
                       Log.i("TAG what is task?", "ZZZ " + task.toString())
                       Log.i("TAG", "ZZZ " + lastKnownLocation.toString() )
                       if (lastKnownLocation != null) {
                           map.moveCamera(
                               CameraUpdateFactory.newLatLngZoom(
                                   LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude),
                                   15.0f
                               )
                           )
                           Log.i("TAG", "ZZZ" + lastKnownLocation.latitude.toString() + " " + lastKnownLocation.longitude.toString())
                       } }
                   else {
                          Log.d("TAG", "ZZZ Current location is null. Using defaults.")
                       Log.e("TAG", "ZZZ Exception: %s", task.exception)
                       map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, 15.0f))
                       }
                   }
               }
           } catch (e: SecurityException) {
               Log.e("ZZZ Exception: %s", e.message, e)
       }
        }


//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
     //  must call onLocationSelected()




    private fun onLocationSelected() {
        _viewModel.latitude.value = localpoi?.latLng?.latitude
        _viewModel.longitude.value = localpoi?.latLng?.longitude
        _viewModel.reminderSelectedLocationStr.value = localpoi?.name
        _viewModel.selectedPOI.value = localpoi
        Log.i("YYY", "reminderSelectedLocationStr is: " + localpoi?.name)

        // https://stackoverflow.com/questions/10863572/programmatically-go-back-to-the-previous-fragment-in-the-backstack
        getParentFragmentManager().popBackStackImmediate()
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection, done.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        var locationRequest = LocationRequest()
        locationRequest.setInterval(1000)
      //  mFusedLocationProviderClient.requestLocationUpdates(locationRequest, )

        map = googleMap

        // Add a marker in Sydney and move the camera
        /*    val sydney = LatLng(-34.0, 151.0) - put marker at my home, instead!
            map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            map.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        var latitude = 36.1387812280053
        var longitude = -115.14705185099919
        latitude = -34.0
        longitude = 151.0
        homeLatLng = LatLng(latitude, longitude)
        val zoomLevel = 15f
        val overlaySize = 100f

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        map.addMarker(MarkerOptions().position(homeLatLng))
       // val androidOverlay = GroundOverlayOptions().image(BitmapDescriptorFactory.fromResource(R.drawable.android))
            //  .position(homeLatLng, overlaySize)
       // map.addGroundOverlay(androidOverlay)

       //setMapLongClick(map)
        setPoiClick(map)
       // setMapStyle(map)
       */
        setPoiClick(map)
        setMapLongClick(map)
        Log.i("TAG", "ZZZ in onMapReady")
        enableMyLocation()

    }

    private fun setMapLongClick(map:GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            map.clear() // erases previous markers until user is ready to press "SAVE"
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

            )}
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener {
                poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker.showInfoWindow()
            // pass value to global variable:
            localpoi = poi
        }
    }


}
