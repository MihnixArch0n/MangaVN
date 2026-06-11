# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Giữ line number để crash stack trace đọc được sau khi R8 obfuscate;
# ẩn tên file gốc (chỉ còn "SourceFile:line") — đủ debug với mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile