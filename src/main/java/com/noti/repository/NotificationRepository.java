package com.noti.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noti.dto.request.NotificationSearchRequest;
import com.noti.dto.response.NotificationResponse;
import com.noti.kafka.DeviceNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final JdbcTemplate jdbc;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final RowMapper<NotificationResponse> ROW_MAPPER = (rs, rowNum) -> {
        NotificationResponse r = new NotificationResponse();
        r.setId(rs.getLong("notification_id"));
        r.setAction(rs.getString("action"));
        r.setIcon(rs.getString("icon"));
        r.setTitle(rs.getString("title"));
        r.setContent(rs.getString("content"));
        r.setCreateDate(rs.getTimestamp("created_at"));
        r.setSeen(rs.getInt("is_seen") == 1);
        r.setMapExt(fromJson(rs.getString("ext_json")));
        return r;
    };

    public Long insert(DeviceNotificationEvent event) {
        String extData = toJson(event.getMapExt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String finalExtData = extData;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    """
                    INSERT INTO notification (receiver_id, app, action, icon, title, content, ext_data, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                    RETURNING notification_id
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, String.valueOf(event.getReceiverID()));
            ps.setString(2, event.getApp());
            ps.setString(3, event.getAction());
            ps.setString(4, event.getIcon());
            ps.setString(5, event.getTitle());
            ps.setString(6, event.getContent());
            ps.setString(7, finalExtData);
            ps.setTimestamp(8, new Timestamp(event.getCreateDate() != null ? event.getCreateDate() : System.currentTimeMillis()));
            return ps;
        }, keyHolder);

        Number id = keyHolder.getKey();
        return id != null ? id.longValue() : null;
    }

    public void markAsSeen(Long notificationId, String userId, Long createDate) {
        jdbc.update("""
                INSERT INTO user_notification (notification_id, user_id, created_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, notification_id, created_at) DO NOTHING
                """,
                notificationId,
                userId,
                new Timestamp(createDate));
    }

    public List<NotificationResponse> search(String userId, String jobTitleCode,
                                              LocalDateTime from, LocalDateTime to,
                                              NotificationSearchRequest request) {
        int fetchLimit = request.getLimit() + request.getOffset();
        String seenCondition = "";
        if (request.getSeen() != null) {
            if (request.getSeen() == 1) seenCondition = " AND u.user_id IS NOT NULL ";
            else if (request.getSeen() == 0) seenCondition = " AND u.user_id IS NULL ";
        }

        String sql = """
                (SELECT n.notification_id, n.receiver_id, n.app, n.action, n.icon, n.title, n.content, n.created_at,
                        CASE WHEN u.user_id IS NOT NULL THEN 1 ELSE 0 END as is_seen,
                        n.ext_data::text as ext_json
                 FROM notification n
                 LEFT JOIN user_notification u ON n.notification_id = u.notification_id
                                               AND n.created_at = u.created_at
                                               AND u.user_id = ?
                                               AND u.created_at >= ? AND u.created_at <= ?
                 WHERE n.receiver_id = ?
                   AND n.created_at >= ? AND n.created_at <= ?
                   %s
                 ORDER BY n.created_at DESC LIMIT ?)
                UNION ALL
                (SELECT n.notification_id, n.receiver_id, n.app, n.action, n.icon, n.title, n.content, n.created_at,
                        CASE WHEN u.user_id IS NOT NULL THEN 1 ELSE 0 END as is_seen,
                        n.ext_data::text as ext_json
                 FROM notification n
                 LEFT JOIN user_notification u ON n.notification_id = u.notification_id
                                               AND n.created_at = u.created_at
                                               AND u.user_id = ?
                                               AND u.created_at >= ? AND u.created_at <= ?
                 WHERE n.receiver_id = 'evtman'
                   AND n.created_at >= ? AND n.created_at <= ?
                   %s
                 ORDER BY n.created_at DESC LIMIT ?)
                UNION ALL
                (SELECT n.notification_id, n.receiver_id, n.app, n.action, n.icon, n.title, n.content, n.created_at,
                        CASE WHEN u.user_id IS NOT NULL THEN 1 ELSE 0 END as is_seen,
                        n.ext_data::text as ext_json
                 FROM notification n
                 LEFT JOIN user_notification u ON n.notification_id = u.notification_id
                                               AND n.created_at = u.created_at
                                               AND u.user_id = ?
                                               AND u.created_at >= ? AND u.created_at <= ?
                 WHERE n.receiver_id = ?
                   AND n.created_at >= ? AND n.created_at <= ?
                   %s
                 ORDER BY n.created_at DESC LIMIT ?)
                ORDER BY created_at DESC LIMIT ? OFFSET ?
                """.formatted(seenCondition, seenCondition, seenCondition);

        Timestamp fromTs = Timestamp.valueOf(from);
        Timestamp toTs = Timestamp.valueOf(to);
        String receiverIdStr = String.valueOf(userId);
        String jobCode = jobTitleCode != null ? jobTitleCode : "";

        return jdbc.query(sql, ROW_MAPPER,
                userId, fromTs, toTs, receiverIdStr, fromTs, toTs, fetchLimit,
                userId, fromTs, toTs, fromTs, toTs, fetchLimit,
                userId, fromTs, toTs, jobCode, fromTs, toTs, fetchLimit,
                request.getLimit(), request.getOffset());
    }

    public int countTotal(String userId, String jobTitleCode,
                           LocalDateTime from, LocalDateTime to,
                           NotificationSearchRequest request) {
        Timestamp fromTs = Timestamp.valueOf(from);
        Timestamp toTs = Timestamp.valueOf(to);
        String receiverIdStr = String.valueOf(userId);
        String jobCode = jobTitleCode != null ? jobTitleCode : "";

        boolean isSeen = Integer.valueOf(1).equals(request.getSeen());
        boolean isUnseen = Integer.valueOf(0).equals(request.getSeen());
        boolean isAll = request.getSeen() == null || request.getSeen() == -1;

        if (isSeen) {
            Integer count = jdbc.queryForObject(
                    """
                    SELECT COUNT(1) FROM user_notification
                    WHERE user_id = ? AND created_at BETWEEN ? AND ?
                    """,
                    Integer.class, userId, fromTs, toTs);
            return count != null ? count : 0;
        }

        if (isUnseen) {
            Integer totalNotif = jdbc.queryForObject(
                    """
                    SELECT COUNT(1) FROM notification
                    WHERE receiver_id IN (?, 'evtman', ?)
                      AND created_at BETWEEN ? AND ?
                    """,
                    Integer.class, receiverIdStr, jobCode, fromTs, toTs);
            Integer totalSeen = jdbc.queryForObject(
                    """
                    SELECT COUNT(1) FROM user_notification
                    WHERE user_id = ? AND created_at BETWEEN ? AND ?
                    """,
                    Integer.class, userId, fromTs, toTs);
            return Math.max((totalNotif != null ? totalNotif : 0) - (totalSeen != null ? totalSeen : 0), 0);
        }

        if (!isAll) {
            throw new IllegalArgumentException("Invalid seen value: " + request.getSeen());
        }

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(1) FROM notification
                WHERE receiver_id IN (?, 'evtman', ?)
                  AND created_at BETWEEN ? AND ?
                """,
                Integer.class, receiverIdStr, jobCode, fromTs, toTs);
        return count != null ? count : 0;
    }

    public Optional<NotificationResponse> getById(Long notificationId, String userId,
                                                   String jobTitleCode, Long createDate) {
        String jobCode = jobTitleCode != null ? jobTitleCode : "";
        List<NotificationResponse> results = jdbc.query(
                """
                SELECT n.notification_id, n.receiver_id, n.app, n.action, n.icon,
                       n.title, n.content, n.created_at,
                       CASE WHEN u.user_id IS NOT NULL THEN 1 ELSE 0 END AS is_seen,
                       n.ext_data::text as ext_json
                FROM notification n
                LEFT JOIN user_notification u ON n.notification_id = u.notification_id
                                               AND n.created_at = u.created_at
                                               AND u.user_id = ?
                WHERE n.notification_id = ?
                  AND n.created_at = ?
                  AND n.receiver_id IN (?, 'evtman', ?)
                """,
                ROW_MAPPER, userId, notificationId, new Timestamp(createDate),
                String.valueOf(userId), jobCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private String toJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, String> fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
