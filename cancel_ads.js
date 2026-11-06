/*
 * cancel_ads.js - Block sponsored ad pop-ups in cz.bakalari.mobile (Flutter app)
 *
 * Usage:
 *   frida -U -f cz.bakalari.mobile -l cancel_ads.js --no-pause
 *
 * This script hooks the HTTP client at multiple levels to block requests
 * to sponsored ad CDN domains. Since cz.bakalari.mobile is a Flutter app,
 * its ad logic lives in Dart code (libapp.so) which cannot be patched
 * with Morphe patches or binary patching without corrupting the snapshot.
 *
 * Frida runtime instrumentation is the correct approach for Flutter apps.
 */

const BLOCKED_DOMAINS = [
    'campaign.bakalari.cz',
    'campaign-dev.bakalari.cz',
    'netglade.cz'
];

function isBlocked(url) {
    try {
        return BLOCKED_DOMAINS.some(d => url.includes(d));
    } catch(e) {
        return false;
    }
}

function log(msg) {
    console.log('[Morphe-AdBlock] ' + msg);
}

Java.perform(function () {
    let hooksApplied = 0;

    // ================================================================
    // Hook 1: OkHttpClient.newCall - Main OkHttp client
    // Flutter's Dart HttpClient typically uses OkHttp on Android
    // ================================================================
    try {
        var OkHttpClient = Java.use('okhttp3.OkHttpClient');
        OkHttpClient.newCall.overload('okhttp3.Request').implementation = function(request) {
            var url = request.url().toString();
            if (isBlocked(url)) {
                log('BLOCKED (OkHttpClient.newCall): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.newCall(request);
        };
        log('Hooked OkHttpClient.newCall');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook OkHttpClient.newCall: ' + e.message);
    }

    // Hook 2: FirebasePerfOkHttpClient.execute
    try {
        var FirebasePerfOkHttpClient = Java.use('com.google.firebase.perf.network.FirebasePerfOkHttpClient');
        FirebasePerfOkHttpClient.execute.overload('okhttp3.Call').implementation = function(call) {
            var url = call.request().url().toString();
            if (isBlocked(url)) {
                log('BLOCKED (FirebasePerfOkHttpClient): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.execute(call);
        };
        log('Hooked FirebasePerfOkHttpClient.execute');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook FirebasePerfOkHttpClient.execute: ' + e.message);
    }

    // Hook 3: c8/f.j - Firebase Performance URL tracking method
    // This is called for every outgoing request in the app
    try {
        var c8_f = Java.use('c8.f');
        c8_f.j.overload('java.lang.String').implementation = function(url) {
            if (isBlocked(url)) {
                log('BLOCKED (c8/f.j): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.j(url);
        };
        log('Hooked c8/f.j (Firebase Performance URL tracking)');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook c8/f.j: ' + e.message);
    }

    // Hook 4: HttpURLConnection.connect - Direct Java HTTP connections
    try {
        var HttpURLConnection = Java.use('java.net.HttpURLConnection');
        HttpURLConnection.connect.implementation = function() {
            var url = this.getURL().toString();
            if (isBlocked(url)) {
                log('BLOCKED (HttpURLConnection): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.connect();
        };
        log('Hooked HttpURLConnection.connect');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook HttpURLConnection.connect: ' + e.message);
    }

    // Hook 5: FirebasePerfHttpClient.execute - Apache HttpClient wrapper
    try {
        var FirebasePerfHttpClient = Java.use('com.google.firebase.perf.network.FirebasePerfHttpClient');
        FirebasePerfHttpClient.execute.overload(
            'org.apache.http.client.HttpClient',
            'org.apache.http.HttpHost',
            'org.apache.http.HttpRequest',
            'org.apache.http.client.ResponseHandler'
        ).implementation = function(client, host, request, handler) {
            var url = request.getRequestLine().toString();
            if (isBlocked(url)) {
                log('BLOCKED (FirebasePerfHttpClient): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.execute(client, host, request, handler);
        };
        log('Hooked FirebasePerfHttpClient.execute');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook FirebasePerfHttpClient.execute: ' + e.message);
    }

    // Hook 6: okhttp3.HttpUrl.Companion.d - URL parsing
    try {
        var HttpUrlCompanion = Java.use('okhttp3.HttpUrl$Companion');
        HttpUrlCompanion.d.overload('java.lang.String').implementation = function(url) {
            if (isBlocked(url)) {
                log('BLOCKED (HttpUrl.Companion.d): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.d(url);
        };
        log('Hooked okhttp3.HttpUrl.Companion.d');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook okhttp3.HttpUrl.Companion.d: ' + e.message);
    }

    // Hook 7: okhttp3.Call.execute - Direct method call execution
    try {
        var OkHttpCall = Java.use('okhttp3.Call');
        OkHttpCall.execute.overload().implementation = function() {
            var url = this.request().url().toString();
            if (isBlocked(url)) {
                log('BLOCKED (OkHttpCall.execute): ' + url);
                throw new Error('Ad blocked: ' + url);
            }
            return this.execute();
        };
        log('Hooked okhttp3.Call.execute');
        hooksApplied++;
    } catch(e) {
        log('Failed to hook okhttp3.Call.execute: ' + e.message);
    }

    log('');
    log('Morphe AdBlock loaded successfully!');
    log('Blocked domains: ' + BLOCKED_DOMAINS.join(', '));
    log('Active hooks: ' + hooksApplied);
    log('');
});