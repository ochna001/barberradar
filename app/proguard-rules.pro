# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep model classes
-keep class com.example.barberradar.models.BarberShop { *; }
-keepclassmembers class com.example.barberradar.models.BarberShop { 
   <fields>; 
   <methods>; 
}

# Keep Firestore model classes
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.firestore.**$* { *; }

# Keep @Keep annotations
-keep @androidx.annotation.Keep class * {*;}
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * {*;}

# Keep all public and protected methods that could be used via reflection
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keep @androidx.annotation.Keep class * {*;}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile