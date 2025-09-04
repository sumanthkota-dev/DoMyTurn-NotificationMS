package app.domyturn.notification.exception;

import lombok.Data;

@Data
public class ErrorInfo {
	private String errorMessage;
	private Integer errorCode;
}
