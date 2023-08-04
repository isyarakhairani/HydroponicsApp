package com.example.hydroponics;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

public class Firestore {
    private static final String TAG = Firestore.class.getSimpleName();
    private FirebaseFirestore db;
    private onRealtimeUpdateListener callback;

    public Firestore(onRealtimeUpdateListener callback) {
        this.callback = callback;
    }

    public interface onRealtimeUpdateListener {
        void onRealtimeUpdate(DocumentSnapshot snapshot);
    }

    public void setup() {
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build();
        db.setFirestoreSettings(settings);
        Log.i(TAG, "Firestore initialized");
    }

    public void addRealtimeUpdateListener() {
        final DocumentReference docRef = db.collection("hydroponics").document("realtime_update");
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    callback.onRealtimeUpdate(snapshot);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }
}


