package app.domyturn.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.domyturn.notification.entity.Home;

public interface HomeRepository extends JpaRepository<Home, Long>
{

	Optional<Home> findByHomeId(Long homeId);

	boolean existsByHomeId(Long homeId);
	void deleteByHomeId(Long homeId);
}
