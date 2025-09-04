package app.domyturn.notification.dto;

import lombok.Data;

@Data
public class FcmTokenRequest 
{
	private Long userId;
	private String fcmToken;
}
