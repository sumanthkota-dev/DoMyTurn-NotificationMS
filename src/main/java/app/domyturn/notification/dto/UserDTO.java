package app.domyturn.notification.dto;

import app.domyturn.notification.entity.User;
import lombok.Data;

@Data
public class UserDTO 
{
	private Long id;
	private String email;
	private String userName;
	private String mobile;
	private String fcmToken;
	
	public static User getEntity(UserDTO userDTO) {
		User user = new User();
		user.setUserId(userDTO.getId());
		user.setEmail(userDTO.getEmail());
		user.setMobile(userDTO.getMobile());
		user.setUserName(userDTO.getUserName());
		return user;
	}

}
