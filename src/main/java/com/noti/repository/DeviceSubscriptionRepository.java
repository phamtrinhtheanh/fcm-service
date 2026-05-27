package com.noti.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DeviceSubscriptionRepository {

    private final JdbcTemplate jdbc;

    public int subscribe(Long userId, String app, String token) {
        return jdbc.update("""
                INSERT INTO user_device (userid, app, device_token, status, create_date, last_update)
                VALUES (?, ?, ?, 1, now(), now())
                ON CONFLICT (userid, app)
                DO UPDATE SET device_token = EXCLUDED.device_token,
                              status = 1,
                              last_update = now()
                """, userId, app, token);
    }

    public int unsubscribe(Long userId, String app) {
        return jdbc.update("""
                UPDATE user_device
                SET device_token = NULL, status = 0, last_update = now()
                WHERE userid = ? AND app = ?
                """, userId, app);
    }

    public String getActiveToken(Long userId, String app) {
        var results = jdbc.query(
                """
                SELECT device_token
                FROM user_device
                WHERE userid = ? AND app = ? AND status = 1 AND device_token IS NOT NULL
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("device_token"),
                userId, app);
        return results.isEmpty() ? null : results.get(0);
    }
}
