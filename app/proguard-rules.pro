# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep JDBC drivers
-keep class com.mysql.cj.jdbc.** { *; }
-keep class com.mysql.** { *; }
-keep class com.jcraft.jsch.** { *; }

# Keep Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn javax.xml.stream.**

# Keep data classes
-keep class com.miladb.data.model.** { *; }
