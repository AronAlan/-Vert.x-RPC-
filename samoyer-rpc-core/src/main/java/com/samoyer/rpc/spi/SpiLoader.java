package com.samoyer.rpc.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import com.samoyer.rpc.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI加载器
 *
 * @author Samoyer
 * @since 2024-08-09
 */
@Slf4j
public class SpiLoader {
    /**
     * 存储已加载的类：接口名 => (key => 实现类）
     */
    private static final Map<String, Map<String, Class<?>>> LOADER_MAP = new ConcurrentHashMap<>();

    /**
     * 对象实例缓存（避免重复new），类路径 => 对象实例，单例模式
     */
    private static final Map<String, Object> INSTANCE_CACHE = new ConcurrentHashMap<>();

    /**
     * 系统(system)SPI目录
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /**
     * 用户自定义(custom)SPI目录
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /**
     * 扫描路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_CUSTOM_SPI_DIR,RPC_SYSTEM_SPI_DIR};

    /**
     * 动态加载的类列表
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = List.of(Serializer.class);

    /**
     * 加载所有类型
     */
    public static void loadAll() {
        log.info("加载所有SPI");
        for (Class<?> aClass : LOAD_CLASS_LIST) {
            Map<String, Class<?>> loadSpi = load(aClass);
        }
    }

    /**
     * 获取某个接口的实例
     * 2.再获取其实例
     * @param tClass
     * @param key
     * @param <T>
     * @return
     */
    public static <T> T getInstance(Class<?> tClass, String key) {
        // 获取当前类的全限定名
        String tClassName = tClass.getName();
        // 从配置加载器中获取与当前类名相关联的键类映射
        Map<String, Class<?>> keyClassMap = LOADER_MAP.get(tClassName);
        // 如果未找到对应的键类映射，抛出异常提示该类型未被加载
        if (keyClassMap == null) {
            throw new RuntimeException(String.format("SpiLoader 未加载 %s 类型", tClassName));
        }
        // 如果键类映射中不存在指定的键，抛出异常提示该键对应类型不存在
        if (!keyClassMap.containsKey(key)) {
            throw new RuntimeException(String.format("SpiLoader 的 %s 不存在 key=%s 的类型", tClassName, key));
        }
        //获取到要加载的实现类型
        Class<?> implClass = keyClassMap.get(key);
        //从实例缓存中加载指定类型的实例
        String implClassName = implClass.getName();
        // 检查实例缓存中是否已存在指定类名的实例
        if (!INSTANCE_CACHE.containsKey(implClassName)) {
            // 如果不存在，则尝试创建一个新的实例并将其缓存
            try {
                INSTANCE_CACHE.put(implClassName, implClass.getDeclaredConstructor().newInstance());
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                // 如果实例化失败，抛出运行时异常并附带错误信息
                String errorMessage = String.format("%s 类实例化失败", implClassName);
                throw new RuntimeException(errorMessage, e);
            }
        }
        // 从缓存中获取实例，并将其转换类型后返回
        return (T) INSTANCE_CACHE.get(implClassName);

    }

    /**
     * 加载某个类型
     * 1.先加载类型
     * @param loadClass
     * @return
     */
    public static Map<String, Class<?>> load(Class<?> loadClass) {
        log.info("加载类型为 {} 的 SPI",loadClass.getName());
        //扫描路径，用户自定义的SPI优先级高于系统的SPI
        Map<String,Class<?>> keyClassMap=new HashMap<>();
        // 遍历所有指定的扫描目录
        for (String scanDir : SCAN_DIRS) {
            // 获取指定包名下的所有资源
            List<URL> resources = ResourceUtil.getResources(scanDir + loadClass.getName());
            // 读取每个资源文件
            for (URL resource : resources) {
                try {
                    // 创建输入流读取资源文件内容
                    InputStreamReader inputStreamReader = new InputStreamReader(resource.openStream());
                    // 包装输入流以逐行读取
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    // 逐行读取文件内容
                    while((line= bufferedReader.readLine())!=null){
                        // 分割行内容为键值对
                        String[] strArray=line.split("=");
                        // 如果行内容包含键值对
                        if (strArray.length>1){
                            // 获取键
                            String key=strArray[0];
                            // 获取值（类名）
                            String className=strArray[1];
                            // 将键值对存入映射表
                            keyClassMap.put(key,Class.forName(className));
                        }
                    }
                }catch (Exception e){
                    // 记录资源加载错误
                    log.error("spi resource load error",e);
                }
            }
        }
        // 将加载的类映射存入全局映射表
        LOADER_MAP.put(loadClass.getName(),keyClassMap);
        return keyClassMap;
    }

}
