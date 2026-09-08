# Proguard configuration for Nivya Android Foundation

# Retain Retrofit and Gson models
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retain Room database entities and Daos
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Retain WorkManager workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
