package app.domyturn.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.domyturn.common.dto.NotificationDTO;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import app.domyturn.notification.dto.ActivityDTO;
import app.domyturn.notification.dto.PushNotification;
import app.domyturn.notification.dto.UserDTO;
import app.domyturn.notification.entity.Home;
import app.domyturn.notification.entity.HomeNotification;
import app.domyturn.notification.entity.User;
import app.domyturn.notification.exception.NotificationException;
import app.domyturn.notification.repository.HomeRepository;
import app.domyturn.notification.repository.NotificationRepository;
import app.domyturn.notification.repository.UserRepository;

@Service
public class NotificationService {

	private final HomeRepository homeRepository;

	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	private final FcmPushService fcmPushService;

	public NotificationService(FcmPushService fcmPushService, HomeRepository homeRepository) {
		this.fcmPushService = fcmPushService;
		this.homeRepository = homeRepository;
	}

	public String sendNotification(PushNotification request) throws Exception {
		Message message = Message.builder().setToken(request.getToken())
				.setNotification(
						Notification.builder().setTitle(request.getTitle()).setBody(request.getMessage()).build())
				.putData("click_action", "FLUTTER_NOTIFICATION_CLICK").build();

		return FirebaseMessaging.getInstance().send(message);
	}

	public List<String> fetchAllUsers(Long homeId) {
		return userRepository.findByHomeId(homeId).stream().map(User::getFcmToken).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public void saveUser(UserDTO userDTO) throws NotificationException {
		Optional<User> existingUser = userRepository.findByUserId(userDTO.getId());

		if (!existingUser.isPresent()) {
			User newUser = UserDTO.getEntity(userDTO); // id will be null; will auto-increment
			userRepository.save(newUser);
		} else {
			logger.info("User.present" + userDTO.getEmail());
			throw new NotificationException("user.present");
		}
	}

	public void updateFcmToken(Long userId, String fcmToken) throws NotificationException {
		User user = userRepository.findByUserId(userId).orElseThrow(() -> new NotificationException(""));
		user.setFcmToken(fcmToken);
		userRepository.save(user);
	}

	public String getFcmToken(Long userId) throws NotificationException {
		User user = userRepository.findByUserId(userId).orElseThrow(() -> new NotificationException(""));
		if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
			logger.warn("Skipping user {} due to missing FCM token", user.getUserId());
			return null;
		}
		return user.getFcmToken();
	}

	public List<ActivityDTO> getNotificationsForHome(Long homeId) {
		List<HomeNotification> activities = notificationRepository.findTop50ByHomeIdOrderByTimestampDesc(homeId);

		List<ActivityDTO> activityDtos = new ArrayList<>();
		for (HomeNotification homeNotification : activities) {
			ActivityDTO activityDTO = new ActivityDTO();
			activityDTO.setAction(homeNotification.getAction());
			activityDTO.setTimestamp(homeNotification.getTimestamp());
			activityDtos.add(activityDTO);
		}
		return activityDtos;
	}

	public void saveNotification(NotificationDTO dto) {
		HomeNotification notification = new HomeNotification();
		notification.setHomeId(dto.getHomeId());
		notification.setAction(dto.getMsg());
		notification.setUserIds(dto.getUserIds());
		notification.setTimestamp(dto.getTimestamp());
		notificationRepository.save(notification);

	}

	public void sendReminder(Long userId, String message) {
		try {
			String token = getFcmToken(userId);
			logger.info("🔔 Sending to userId={}, token={}", userId, token);

			if (token != null && !token.isBlank()) {
				fcmPushService.sendPush(token, "Chore Reminder", message);
			} else {
				logger.warn("⚠️ Skipping userId={} due to missing token", userId);
			}
		} catch (Exception e) {
			logger.error("❌ Error sending to userId={}", userId, e);
		}
	}

	public void deleteUser(Long id) {
		if (userRepository.existsByUserId(id)) {
			userRepository.deleteById(id);
			logger.info("User Deleted");
		}
	}

	public void deleteHome(Long homeId) throws NotificationException {
		Home home = homeRepository.findByHomeId(homeId).orElseThrow(() -> new NotificationException("no home found"));
		logger.info("Deleting home {}", homeId);
		homeRepository.delete(home);
		
		List<HomeNotification> notifications = notificationRepository.findAllByHomeId(homeId);
		notificationRepository.deleteAll(notifications);
	}
}