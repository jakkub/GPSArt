package com.jakkub.gpsart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.example.gpsart.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity  implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, 
LocationListener, com.google.android.gms.location.LocationListener {
	
    private GoogleMap map;
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private PolylineOptions polyline = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
    boolean pause = false;
    Menu mainMenu;
    
    private final static int
    CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (savedInstanceState != null)
		{
			pause = savedInstanceState.getBoolean("pause");
		}

		// Initialize Google Map
		try {
            initilizeMap();
            
            // Restore saved polyline
    		if (savedInstanceState != null) {
    			ArrayList<LatLng> list = savedInstanceState.getParcelableArrayList("polyline");
    	        for (int i = 0; i < list.size(); i++) {
    	        	polyline.add(list.get(i));
    	        }
    	    }
 
        } catch (Exception e) {
            e.printStackTrace();
        }
		
		// Check if Google Play Services are available
		servicesConnected();
		
		// Enable My Location
		map.setMyLocationEnabled(true); 
		
		// Enable My Location Button
		map.getUiSettings().setMyLocationButtonEnabled(true);
		
		// Create Location Client
		mLocationClient = new LocationClient(this, this, this);
		
		// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        
        // Set the update interval to 12 seconds
        mLocationRequest.setInterval(12000);
        
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(1000);
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        
        // Connect the Location Client
        mLocationClient.connect();
    }
	
	@Override
    protected void onStop() {
        // Disconnecting the client invalidates it
        mLocationClient.disconnect();
        
        super.onStop();
    }
	
	
	// Load Google Maps
    private void initilizeMap() {
        if (map == null) 
        {
        	map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
 
            // check if map is created successfully or not
            if (map == null) 
            {
                Toast.makeText(getApplicationContext(), "Sorry! unable to create maps", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private LatLng getMyLocation() {

		Location mCurrentLocation;
	    mCurrentLocation = mLocationClient.getLastLocation();
	    
	    LatLng mLatLng;
	    mLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
	    
	    return mLatLng;

    }
    
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mainMenu = menu;
		setPauseMenuTitle();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    	case R.id.action_export:
				try {
					export();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    		return true;
	        case R.id.action_pause:
	            pause();
	            return true;
	        case R.id.action_clear:
	            clear();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void clear() {
		polyline = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
		map.clear();
	}
	
	private void pause() {
		if (pause)
		{
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
			pause = false;
			
		} else {
			mLocationClient.removeLocationUpdates(this);
			pause = true;
		}
		setPauseMenuTitle();
	}
	
	private void setPauseMenuTitle() {
		if (pause)
		{
			mainMenu.findItem(R.id.action_pause).setTitle(R.string.action_resume);
			
		} else {
			mainMenu.findItem(R.id.action_pause).setTitle(R.string.action_pause);
		}
	}
	
	private void export() throws IOException {
		ArrayList<LatLng> listLL = new ArrayList<LatLng>(polyline.getPoints());
		ArrayList<PointXY> listXY = new ArrayList<PointXY>();
		
		if (listLL.size() > 1)
		{
			
			double maxX = 0;
			double minX = 500;
			double maxY = 0;
			double minY = 500;
			
			// convert LL values to XY
			for (LatLng item : listLL) {
				listXY.add(latLngToXY(item));
	        }
			
			// find mins and maxs
			for (PointXY item : listXY) {
				if (item.x > maxX)
					maxX = item.x;
				if (item.y > maxY)
					maxY = item.y;
				if (item.x < minX)
					minX = item.x;
				if (item.y < minY)
					minY = item.y;
	        }
			
			double rangeX = maxX - minX;
			double rangeY = maxY - minY;
			double rangeMax;
			if (rangeX > rangeY)
				rangeMax = rangeX;
			else 
				rangeMax = rangeY;
			
			if (rangeMax > 0)
			{
			
				double multiplier = 500 / rangeMax;
				
				int width = (int) (rangeX*multiplier+20);
				int height = (int) (rangeY*multiplier+20);
				
				Bitmap.Config conf = Bitmap.Config.ARGB_8888; 
				Bitmap bmp = Bitmap.createBitmap(width, height, conf);
				Canvas canvas = new Canvas(bmp);
				canvas.drawColor(Color.WHITE);
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				paint.setStrokeWidth(3);
				paint.setColor(Color.BLUE);
				
				for (int i = 0; i < listXY.size() - 1; i++) {
					Point p1 = new Point();
					Point p2 = new Point();
					p1.x = (int) ((listXY.get(i).x - minX)*multiplier);
					p1.y = (int) ((listXY.get(i).y - minY)*multiplier);
					p2.x = (int) ((listXY.get(i + 1).x - minX)*multiplier);
					p2.y = (int) ((listXY.get(i + 1).y - minY)*multiplier);
		        	canvas.drawLine(p1.x+10, p1.y+10, p2.x+10, p2.y+10, paint);
		        }
				
				
				
				Calendar calendar = Calendar.getInstance();
				SimpleDateFormat date_format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
			    String filename = "GPSArt_" + date_format.format(calendar.getTime()) + ".png";
		
				String path = Environment.getExternalStorageDirectory().toString();
		
				File file = new File(path, filename);
				
				FileOutputStream fOut = new FileOutputStream(file);
		
				bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
				
				fOut.flush();
				fOut.close();
				
				Toast.makeText(getApplicationContext(), "Image saved to " + path + "/" + filename, Toast.LENGTH_SHORT).show();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Unable to create image", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private PointXY latLngToXY(LatLng latlng) {
		PointXY point = new PointXY();
		
		int width    = 500;
		int height   = 500;
		double lng = latlng.longitude;
		double lat = latlng.latitude;

		point.x = ((lng+180)*(width/360));
        point.y = ((height/2)-(width*Math.log(Math.tan((Math.PI/4)+((lat*Math.PI/180)/2)))/(2*Math.PI)));
	
		return point;
	}
	
	
	 /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        
        // Start location updates
        if (!pause)
        {
        	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        } else {
        	map.addPolyline(polyline);
        }

        // Zoom to my location
        CameraPosition cameraPosition = new CameraPosition.Builder()
        .target(getMyLocation())      // Sets the center of the map to Mountain View
        .zoom(15)                   // Sets the zoom
        .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        
        
    }
    
    
    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        
        // Get location
        LatLng mLatLng = new LatLng (location.getLatitude(), location.getLongitude());

        // Add new point to the polyline list
        polyline.add(mLatLng);
        
        // Render polyline
        map.addPolyline(polyline);
        
        
        // Get visible region
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        
        // Move camera to the new position
        if(!bounds.contains(mLatLng))
        	map.animateCamera(CameraUpdateFactory.newLatLng(mLatLng));
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	
	    ArrayList<LatLng> points = new ArrayList<LatLng>(polyline.getPoints());
	    outState.putParcelableArrayList("polyline", points);
	    
	    outState.putBoolean("pause", pause);
    }


    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {

    }


    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Toast.makeText(this, connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
        }
        
    }
    
    
    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    
    // Check that Google Play services is available
    private boolean servicesConnected() {
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(),
                        "Location Updates");
            }
        }
        return false;
    }

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
       

}
