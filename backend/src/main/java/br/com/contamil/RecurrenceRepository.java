package br.com.contamil;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {
    List<Recurrence> findAllByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurrence r where r.active = true order by r.id")
    List<Recurrence> findActiveForUpdate();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurrence r where r.id = :id")
    Optional<Recurrence> findForUpdate(@Param("id") Long id);
}
