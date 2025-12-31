package com.ravi.busmanagementt.utils

object Constants{

    /************ Firestore ***********/

    const val BUS_COLLECTION_REF = "buses"
    // Collection fields
    const val BUS_ID_FIELD_REF = "busId"
    const val BUS_NAME_FIELD_REF = "busName"
    const val EMAIL_FIELD_REF = "email"
    const val DRIVER_NAME_FIELD_REF = "driverName"
    const val ROUTES_FIELD_REF = "routes"
    // Routes fields
    const val GEO_POINT_FIELD_REF = "geoPoint"
    const val LOCATION_FIELD_REF = "location"
    const val STOP_NAME_FIELD_REF = "stopName"


    const val PARENTS_COLLECTION_REF = "parents"
    const val ASSIGNED_BUS_ID_FIELD_REF = "assignedBusId"
    const val BUS_STOP_LOCATION_FIELD_REF = "busStopLocation"
    const val FCM_TOKEN_FIELD_REF = "fcmToken"
    const val PARENT_NAME_FIELD_REF = "name"
    const val BUS_STOP_NAME_FIELD_REF = "stopName"





    /************ Firebase Realtime ***********/



    /** Others **/
    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/privacypolicy-school-bus/home"
}