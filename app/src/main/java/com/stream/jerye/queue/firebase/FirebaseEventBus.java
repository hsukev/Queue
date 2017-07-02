package com.stream.jerye.queue.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stream.jerye.queue.MessagePage.Message;
import com.stream.jerye.queue.MusicPage.SimpleTrack;
import com.stream.jerye.queue.lobby.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jerye on 6/20/2017.
 */

public class FirebaseEventBus {
    private static FirebaseDatabase mFirebaseDatabase;
    private static DatabaseReference mMusicDatabaseReference, mMessageDatabaseReference, mRoomDatabaseReference;
    private static String TAG = "FirebaseEventBus.java";

    public interface FirebasePeekHandler{
        void peekedResult(List<SimpleTrack> list);
    }

    public interface FirebaseQueueAdapterHandler {
        void enqueue(SimpleTrack simpleTrack);
    }

    public interface FirebaseMessageHandler {
        void addMessage(Message message);
    }


    public static class MusicDatabaseAccess{
        private FirebasePeekHandler mFirebasePeekHandler;
        private FirebaseQueueAdapterHandler mFirebaseQueueAdapterHandler;
        private Context mContext;
        private SharedPreferences prefs;

        public MusicDatabaseAccess(Context context, FirebasePeekHandler firebasePeekHandler) {
            mContext = context;
            mFirebasePeekHandler = firebasePeekHandler;
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            prefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
            String roomKey = prefs.getString("room key", "");
            if(!roomKey.equals("")){
                mMusicDatabaseReference = mFirebaseDatabase.getReference().child(roomKey).child("tracks");
            }else{
                Log.e(TAG, "invalid room key");
            }

        }

        public MusicDatabaseAccess(Context context, FirebaseQueueAdapterHandler firebaseQueueAdapterHandler) {
            mContext = context;
            mFirebaseQueueAdapterHandler = firebaseQueueAdapterHandler;
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            prefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
            String roomKey = prefs.getString("room key", "");
            if(!roomKey.equals("")){
                mMusicDatabaseReference = mFirebaseDatabase.getReference().child(roomKey).child("tracks");
                Log.e(TAG, "room key: "+ roomKey);
                Log.e(TAG, "ref: "+ mMusicDatabaseReference.toString());

            }else{
                Log.e(TAG, "invalid room key");
            }        }

        public void addChildListener() {
            mMusicDatabaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    SimpleTrack simpleTrack = dataSnapshot.getValue(SimpleTrack.class);
                    simpleTrack.setKey(dataSnapshot.getKey());
                    mFirebaseQueueAdapterHandler.enqueue(simpleTrack);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void push(SimpleTrack simpleTrack) {

            mMusicDatabaseReference.push().setValue(simpleTrack);
            Log.d(TAG,mMusicDatabaseReference.getRef().toString());

        }

        public void peek() {
            mMusicDatabaseReference.orderByKey().limitToFirst(2).addChildEventListener(new ChildEventListener() {
                List<SimpleTrack> list = new ArrayList<>(2);

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Log.d(TAG, "peeked");
                    SimpleTrack track = dataSnapshot.getValue(SimpleTrack.class);
                    track.setKey(dataSnapshot.getKey());
                    list.add(track);
                    if (list.size() == 2) {
                        mFirebasePeekHandler.peekedResult(list);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void remove(SimpleTrack track) {
            mMusicDatabaseReference.child(track.getKey()).removeValue();
        }
    }

    public static class MessageDatabaseAccess {
        private FirebaseMessageHandler mFirebaseMessageHandler;

        public MessageDatabaseAccess(FirebaseMessageHandler firebaseMessageHandler) {
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mMessageDatabaseReference = mFirebaseDatabase.getReference().child("messages");
            mFirebaseMessageHandler = firebaseMessageHandler;
        }

        public void addChildListener() {
            mMessageDatabaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Message message = dataSnapshot.getValue(Message.class);
                    mFirebaseMessageHandler.addMessage(message);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        public void push(String chatMessage, String userName) {
            Message message = new Message(chatMessage, userName, null);
            mMessageDatabaseReference.push().setValue(message);
        }
    }

    public static class RoomDatabaseAccess {
        private Context mContext;
        private SharedPreferences prefs;

        public RoomDatabaseAccess(Context context) {
            mContext = context;
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mRoomDatabaseReference = mFirebaseDatabase.getReference();
            prefs = mContext.getSharedPreferences(mContext.getPackageName(), Context.MODE_PRIVATE);
        }

        public void push(String roomTitle, String roomPassword) {
            Room newRoom = new Room(roomTitle, roomPassword);
            String roomKey = mRoomDatabaseReference.push().getKey();
            prefs.edit().putString("room key",roomKey).apply();
            mRoomDatabaseReference.child(roomKey).setValue(newRoom);
        }
    }

}
