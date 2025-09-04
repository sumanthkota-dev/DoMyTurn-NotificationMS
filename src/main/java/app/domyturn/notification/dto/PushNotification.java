package app.domyturn.notification.dto;

import lombok.Data;

@Data
public class PushNotification {
	private String title;
	private String message;
	private String token;
}
