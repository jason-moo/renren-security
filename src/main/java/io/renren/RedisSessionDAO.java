package io.renren;

/**
 * Created by jason_moo on 2018/6/19.
 */

import io.renren.utils.RedisUtils;
import io.renren.utils.SerializeUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 调整shiro session操作父类
 */
public class RedisSessionDAO extends AbstractSessionDAO {

    private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * The Redis key prefix for the sessions
     */
    private String keyPrefix = "shiro_redis_session:";

    /**
     * redis 缓存过期时间/秒
     */
    private int expire = 60 * 60;

    /**
     * save session
     *
     * @param session
     * @throws UnknownSessionException
     */
    private void saveSession(final Session session) throws UnknownSessionException {
        logger.debug("saveSession");
        if (session == null || session.getId() == null) {
            logger.error("session or session id is null");
            return;
        }
        final byte[] key = getByteKey(session.getId());
        final byte[] value = SerializeUtils.serialize(session);
        session.setTimeout(expire * 1000);
        stringRedisTemplate.execute(new RedisCallback<Void>() {
            @Override
            public Void doInRedis(RedisConnection connection) throws DataAccessException {
                connection.setEx(key,expire,value);
                return null;
            }
        });
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        logger.debug("update");
        this.saveSession(session);
    }

    @Override
    public void delete(Session session) {
        logger.debug("delete");
        if (session == null || session.getId() == null) {
            logger.error("session or session id is null");
            return;
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        logger.debug("getActiveSessions");
        Set<Session> sessions = new HashSet<>();
        Set<byte[]> keys = RedisUtils.keys(this.keyPrefix + "*");
        if (keys != null && keys.size() > 0) {
            for (byte[] key : keys) {
                Session s = (Session) SerializeUtils.deserialize(RedisUtils.get(key));
                sessions.add(s);
            }
        }
        return sessions;
    }

    @Override
    protected Serializable doCreate(Session session) {
        logger.debug("doCreate");
        Serializable sessionId = this.generateSessionId(session);
        this.assignSessionId(session, sessionId);
        this.saveSession(session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        logger.debug("doReadSession,sessionId:{}", sessionId);
        if (sessionId == null) {
            logger.error("session id is null");
            return null;
        }
        try {
            return (Session) SerializeUtils.deserialize(RedisUtils.get(this.getByteKey(sessionId)));
        } catch (Exception e) {
            logger.error("Failed to deserialize", e);
            return null;
        }
    }

    /**
     * 获得byte[]型的key
     *
     * @param sessionId
     * @return
     */
    private byte[] getByteKey(Serializable sessionId) {
        String preKey = this.keyPrefix + sessionId;
        return preKey.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the Redis session keys
     * prefix.
     *
     * @return The prefix
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * Sets the Redis sessions key
     * prefix.
     *
     * @param keyPrefix The prefix
     */
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }
}