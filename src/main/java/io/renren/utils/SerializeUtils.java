package io.renren.utils;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by jason_moo on 2018/6/19.
 */
public class SerializeUtils {
    private static Logger logger = LoggerFactory.getLogger(SerializeUtils.class);

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     */
    public static Object deserialize(byte[] bytes) {

        Object result = null;

        if (isEmpty(bytes)) {
            return null;
        }
        ByteArrayInputStream byteStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            byteStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteStream);
            result = objectInputStream.readObject();
        } catch (ClassNotFoundException ex) {
            logger.error("Failed to deserialize object type", ex);
        } catch (Exception e) {
            logger.error("Failed to deserialize", e);
        } finally {
            try {
                if(objectInputStream!=null) {
                    objectInputStream.close();
                }
                if(byteStream!=null) {
                    byteStream.close();
                }
            } catch (IOException e) {// do nothing
            }
        }
        return result;
    }

    public static boolean isEmpty(byte[] data) {
        return (data == null || data.length == 0);
    }

    /**
     * 序列化
     *
     * @param object
     * @return
     */
    public static byte[] serialize(Object object) {
        return serialize(object, null);
    }

    /**
     * 序列化
     *
     * @param object
     * @param str    前缀，shiro cache时传入避免重复
     * @return
     */
    public static byte[] serialize(Object object, String str) {

        byte[] result = null;

        if (object == null) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream(128);
            try {
                if (!(object instanceof Serializable)) {
                    throw new IllegalArgumentException(SerializeUtils.class.getSimpleName() + " requires a Serializable payload " +
                            "but received an object of type [" + object.getClass().getName() + "]");
                }
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
                objectOutputStream.writeObject(object);
                if (StringUtils.isNotBlank(str)) {
                    objectOutputStream.writeChars(str);
                }
                objectOutputStream.flush();
                result = byteStream.toByteArray();
            } catch (Throwable ex) {
                throw new Exception("Failed to serialize", ex);
            }
        } catch (Exception ex) {
            logger.error("Failed to serialize", ex);
        }
        return result;
    }
}
