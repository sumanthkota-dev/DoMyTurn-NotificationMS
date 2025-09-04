package app.domyturn.notification.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ActivityDTO {
	private String action;
	private LocalDateTime timestamp;
}
