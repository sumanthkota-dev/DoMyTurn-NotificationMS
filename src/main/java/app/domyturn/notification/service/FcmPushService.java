package app.domyturn.notification.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

@Service
public class FcmPushService {

	@Autowired
    private FirebaseMessaging firebaseMessaging;

    public FcmPushService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendPush(String token, String title, String message) {
        Message msg = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .build();

        firebaseMessaging.sendAsync(msg); // Optionally handle CompletableFuture here
    }
}
