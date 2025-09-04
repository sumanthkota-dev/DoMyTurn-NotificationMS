package app.domyturn.notification.kafka;

import app.domyturn.common.dto.NotificationDTO;

import app.domyturn.notification.entity.Home;
import app.domyturn.notification.exception.NotificationException;
import app.domyturn.notification.repository.HomeRepository;
import app.domyturn.notification.service.FcmPushService;
import app.domyturn.notification.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationListener {

	private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

	private final FcmPushService fcmPushService;
	private final NotificationService notificationService;

	@Autowired
	private HomeRepository homeRepository;

	public NotificationListener(FcmPushService fcmPushService, NotificationService notificationService) {
		this.fcmPushService = fcmPushService;
		this.notificationService = notificationService;
	}

	@KafkaListener(topics = "${kafka.topic}", groupId = "home-notification-group")
	public void handleNotification(NotificationDTO dto) throws NotificationException {
		if (dto.getHomeId() == null || dto.getMsg() == null || dto.getTimestamp() == null) {
			logger.warn("⚠️ Skipping invalid or legacy message: {}", dto);
			return;
		}

		logger.info("📥 Received NotificationDTO: {}", dto);

		Set<Long> targetUserIds = new HashSet<>();

		if ("Home".equals(dto.getSource()) && dto.getUserIds() != null && !dto.getUserIds().isEmpty()) {
			targetUserIds.addAll(dto.getUserIds());
			if(dto.getMsg().contains("exited"))
			{
				removeUserFromHome(dto.getUserId(),dto.getHomeId());
			}
			saveNewUsersInHome(dto.getHomeId(), dto.getUserIds(), dto.getMsg());
		} else {
			Home home = homeRepository.findByHomeId(dto.getHomeId())
					.orElseThrow(() -> new NotificationException("No Home found with id=" + dto.getHomeId()));
			targetUserIds.addAll(home.getUsers());
			logger.info("Targeted Users: {}", targetUserIds);
		}

		notificationService.saveNotification(dto);
		sendPushNotifications(targetUserIds, dto.getMsg());
	}

	private void removeUserFromHome(Long userId, Long homeId) throws NotificationException {
		Home home = homeRepository.findByHomeId(homeId)
				.orElseThrow(() -> new NotificationException("No Home found with id=" + homeId));
		if(home.getUsers().contains(userId))
		{
			home.getUsers().remove(userId);
		}
		homeRepository.save(home);
	}

	private void sendPushNotifications(Set<Long> userIds, String message) {
		List<Long> failedUserIds = new ArrayList<>();

		for (Long userId : userIds) {
			try {
				String token = notificationService.getFcmToken(userId);
				logger.info("🔔 Sending to userId={}, token={}", userId, token);

				if (token != null && !token.isBlank()) {
					fcmPushService.sendPush(token, "Home Update", message);
				} else {
					logger.warn("⚠️ Skipping userId={} due to missing token", userId);
				}
			} catch (Exception e) {
				logger.error("❌ Error sending to userId={}", userId, e);
				failedUserIds.add(userId);
			}
		}

		if (!failedUserIds.isEmpty()) {
			logger.warn("❗ Notification failed for userIds: {}", failedUserIds);
			// Optionally store in DLQ or retry queue
		}
	}

	private void saveNewUsersInHome(Long homeId, List<Long> userIds, String message) throws NotificationException {
		if (message.contains("created")) {
			Home home = new Home();
			home.setHomeId(homeId);
			for (Long userId : userIds) {
				home.getUsers().add(userId);
			}
			homeRepository.save(home);
		}
		else {
			Home home = homeRepository.findByHomeId(homeId)
					.orElseThrow(() -> new NotificationException("No Home found with id=" + homeId));
			if (home.getUsers() == null) {
				home.setUsers(new HashSet<>()); // ✅ initialize the set if null
			}
			boolean changed = false;
			for (Long userId : userIds) {
				if (!home.getUsers().contains(userId)) {
					home.getUsers().add(userId);
					changed = true;
				}
			}

			if (changed) {
				homeRepository.save(home);
				logger.info("💾 Updated Home {} with new users: {}", homeId, userIds);
			}
		}
	}
}
