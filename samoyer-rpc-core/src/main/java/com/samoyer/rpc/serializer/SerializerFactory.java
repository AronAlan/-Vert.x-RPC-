package com.samoyer.rpc.serializer;

import com.samoyer.rpc.spi.SpiLoader;

/**
 * 序列化器工厂（用于获取序列化器对象）
 * @author Samoyer
 * @since 2024-08-09
 */
public class SerializerFactory {
    /**
     * 序列化映射（用于实现单例）
     */
    //硬编码存入序列化器和实现类
    /*private static final Map<String,Serializer> KEY_SERIALIZER_MAP=new HashMap<>(){{
        put(SerializerKeys.JDK,new JdkSerializer());
        put(SerializerKeys.JSON,new JsonSerializer());
        put(SerializerKeys.KRYO,new KryoSerializer());
        put(SerializerKeys.HESSIAN,new HessianSerializer());
    }};*/
    //改为从SPI加载指定的序列化器对象
    static {
        SpiLoader.load(Serializer.class);
    }

    /**
     * 默认序列化器
     */
    private static final Serializer DEFAULT_SERIALIZER=new JdkSerializer();

    /**
     * 获取序列化器的实例
     * @param key
     * @return
     */
    public static Serializer getInstance(String key){
        return SpiLoader.getInstance(Serializer.class,key);
    }
}
