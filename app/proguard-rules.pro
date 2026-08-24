# libphonenumber はメタデータをリソースとして持ち、リフレクションで読み込む。
# 削除・難読化されるとパースが実行時に失敗するため保持する。
-keep class com.google.i18n.phonenumbers.** { *; }
-keepclassmembers class com.google.i18n.phonenumbers.** { *; }

# CallRedirectionService はシステムから名前で束縛される
-keep class io.github.tmlksu.prefixdialer.PrefixRedirectionService { *; }
-keep class io.github.tmlksu.prefixdialer.CallLogRewriteService { *; }
