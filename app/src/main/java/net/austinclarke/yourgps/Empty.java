package net.austinclarke.yourgps;

/**
 * Created by aclarke822 on 8/13/2016.
 */
public class Empty {

/**May be useful in future
 @Override public void onResult(LocationSettingsResult locationSettingsResult) {
 final Status status = locationSettingsResult.getStatus();
 switch (status.getStatusCode()) {
 case LocationSettingsStatusCodes.SUCCESS:

 // NO need to show the dialog;

 break;

 case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
 //  Location settings are not satisfied. Show the user a dialog

 try {
 // Show the dialog by calling startResolutionForResult(), and check the result
 // in onActivityResult().

 status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);

 } catch (IntentSender.SendIntentException e) {

 //unable to execute request
 }
 break;

 case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
 // Location settings are inadequate, and cannot be fixed here. Dialog not created
 break;
 }
 }





 public class MockLocationProvider {
 String providerName;
 Context ctx;
 public static LocationManager getMockLocationManager(String name, MainActivity context) {
 String providerName = name;
 LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
 locationManager.addTestProvider(providerName, false, false, false, false, false, true, true, 0, 5);
 locationManager.setTestProviderEnabled(providerName, true);
 return locationManager;
 }

 public void pushLocation(Location mockLocation) {
 LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
 double lat = mockLocation.getLatitude();
 double lon = mockLocation.getLongitude();

 mockLocation.setLatitude(lat);
 mockLocation.setLongitude(lon);
 mockLocation.setAltitude(0);
 mockLocation.setTime(System.currentTimeMillis());
 mockLocation.setAccuracy(1);
 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
 }
 lm.setTestProviderLocation(providerName, mockLocation);
 }

 public void shutdown() {
 LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
 lm.removeTestProvider(providerName);
 }
 }
 **/


}
