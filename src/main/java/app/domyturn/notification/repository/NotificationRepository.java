package app.domyturn.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domyturn.notification.entity.HomeNotification;

public interface NotificationRepository extends JpaRepository<HomeNotification, Long>
{

	List<HomeNotification> findAllByHomeId(Long homeId);

	List<HomeNotification> findTop50ByHomeIdOrderByTimestampDesc(Long homeId);

}
