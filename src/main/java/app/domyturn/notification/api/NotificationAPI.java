package app.domyturn.notification.api;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;

import app.domyturn.commonsecurity.model.CustomUserDetails;
import app.domyturn.notification.dto.ActivityDTO;
import app.domyturn.notification.dto.FcmTokenRequest;
import app.domyturn.notification.dto.PushNotification;
import app.domyturn.notification.dto.UserDTO;
import app.domyturn.notification.exception.NotificationException;
import app.domyturn.notification.service.NotificationService;

import org.springframework.core.io.Resource;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notification")
@Validated
public class NotificationAPI {

	private static final Log logger = LogFactory.getLog(NotificationAPI.class);

	@Autowired
	private  NotificationService notificationService;
	
	@Value("${firebase.credentials}")
	private Resource firebaseCredentials;

	private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/domyturn-54b16/messages:send";

	@PostMapping("/hello")
	public ResponseEntity<String> getmsg()
	{
		return new ResponseEntity<String>("Hello User",HttpStatus.OK);
	}
	
	@PostMapping("/public/user/create")
	public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO user) throws Exception {
		notificationService.saveUser(user);
		logger.info("User : " + user.toString());
		return new ResponseEntity<>("success", HttpStatus.CREATED);
	}
	
	@DeleteMapping("/public/user/delete/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable Long id) throws Exception {
		notificationService.deleteUser(id);
		return new ResponseEntity<>("success", HttpStatus.CREATED);
	}
	
	@DeleteMapping("/public/delete/home/{homeId}")
	public ResponseEntity<?> deleteHome(@PathVariable Long homeId) throws Exception {
		notificationService.deleteHome(homeId);
		return new ResponseEntity<>("success", HttpStatus.CREATED);
	}
	@PostMapping("/send")
	public ResponseEntity<String> send(@RequestBody PushNotification request) {
		try {
			String response = notificationService.sendNotification(request);
			return ResponseEntity.ok("Notification sent: " + response);
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to send: " + e.getMessage());
		}
	}

	@PostMapping("/send-to-all")
	public ResponseEntity<String> sendToAll(@RequestBody Map<String, String> payload,
			@AuthenticationPrincipal CustomUserDetails user) throws Exception {
		logger.info("Send all entered");
		String accessToken = getAccessToken();
		String title = payload.get("title");
		String body = payload.get("body");
		Long homeId = Long.parseLong(payload.get("homeId"));

		List<String> fcmTokens = notificationService.fetchAllUsers(homeId);

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		RestTemplate restTemplate = new RestTemplate();
		List<String> responses = new ArrayList<>();

		for (String token : fcmTokens) {
			Map<String, Object> notification = new HashMap<>();
			notification.put("title", title);
			notification.put("body", body);

			Map<String, Object> message = new HashMap<>();
			message.put("token", token);
			message.put("notification", notification);

			Map<String, Object> finalPayload = new HashMap<>();
			finalPayload.put("message", message);

			HttpEntity<String> entity = new HttpEntity<>(new Gson().toJson(finalPayload), headers);

			try {
				ResponseEntity<String> response = restTemplate.postForEntity(FCM_ENDPOINT, entity, String.class);
				responses.add("✅ Token: " + token + " → " + response.getStatusCode());
			} catch (Exception e) {
				responses.add("❌ Token: " + token + " → Error: " + e.getMessage());
			}
		}

		return ResponseEntity.ok("Notifications sent:\n" + String.join("\n", responses));
	}

	private String getAccessToken() throws Exception {
//		FileInputStream serviceAccount = new FileInputStream("domyturn-54b16-firebase-adminsdk-fbsvc-116ef59b02.json");

		InputStream stream = firebaseCredentials.getInputStream();
		GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream)
				.createScoped(Collections.singletonList("https://www.googleapis.com/auth/firebase.messaging"));

		googleCredentials.refreshIfExpired();
		return googleCredentials.getAccessToken().getTokenValue();
	}

	@PostMapping("/public/fcm-token")
	public ResponseEntity<String> updateFcmToken(@RequestBody FcmTokenRequest request) throws NotificationException {
		notificationService.updateFcmToken(request.getUserId(), request.getFcmToken());
		return ResponseEntity.ok("Token updated");
	}
	
	@GetMapping("/{homeId}")
    public ResponseEntity<List<ActivityDTO>> getNotificationsByHomeId(@PathVariable Long homeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ActivityDTO> notifications = notificationService.getNotificationsForHome(homeId);
        logger.info(notifications.toString());
        return ResponseEntity.ok(notifications);
    }
	@PostMapping("/reminder/{userId}")
    public ResponseEntity<String> sendReminder(
            @PathVariable Long userId,
            @RequestBody String message) {

        try {
            notificationService.sendReminder(userId, message);
            return ResponseEntity.ok("Reminder sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to send reminder: " + e.getMessage());
        }
    }
	

}
