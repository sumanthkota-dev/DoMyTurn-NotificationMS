package app.domyturn.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domyturn.notification.entity.User;

public interface UserRepository extends JpaRepository<User, Long>
{
	List<User> findByHomeId(Long homeId);

	Optional<User> findByUserId(Long userId);

	boolean existsByUserId(Long userId);

}
