package com.erp.backend.repository;

import com.erp.backend.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Für Admins: eigene + globale (targetUsername = null)
    @Query("SELECT n FROM Notification n WHERE n.targetUsername = :username OR n.targetUsername IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findForAdmin(@Param("username") String username, Pageable pageable);

    // Nur für diesen User (nicht-Admin)
    @Query("SELECT n FROM Notification n WHERE n.targetUsername = :username ORDER BY n.createdAt DESC")
    Page<Notification> findForUser(@Param("username") String username, Pageable pageable);

    // Ungelesene Anzahl für Admins
    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.targetUsername = :username OR n.targetUsername IS NULL) AND n.read = false")
    long countUnreadForAdmin(@Param("username") String username);

    // Ungelesene Anzahl für normale User
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetUsername = :username AND n.read = false")
    long countUnreadForUser(@Param("username") String username);

    // Alle als gelesen markieren (Admin: eigene + globale)
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.targetUsername = :username OR n.targetUsername IS NULL")
    int markAllReadForAdmin(@Param("username") String username);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.targetUsername = :username")
    int markAllReadForUser(@Param("username") String username);

    // Prüft ob heute bereits eine Notification dieses Typs für diese Entität existiert (Deduplizierung)
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.type = :type AND n.entityId = :entityId AND n.createdAt >= :since")
    boolean existsRecentForEntity(@Param("type") Notification.NotificationType type,
                                  @Param("entityId") String entityId,
                                  @Param("since") LocalDateTime since);

    // Alte Benachrichtigungen löschen (z.B. älter als 90 Tage)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff AND n.read = true")
    int deleteOldReadNotifications(@Param("cutoff") LocalDateTime cutoff);
}
