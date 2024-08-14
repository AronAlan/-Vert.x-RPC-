package com.samoyer.rpc.protocol;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 协议消息的序列化器枚举
 * @author Samoyer
 * @since 2024-08-14
 */
@Getter
public enum ProtocolMessageSerializerEnum {
    //请求
    JDK(0,"jdk"),
    //响应
    JSON(1,"json"),
    //心跳
    KRYO(2,"kryo"),
    //其他
    HESSIAN(3,"hessian");

    private final int key;
    private final String value;

    ProtocolMessageSerializerEnum(int key,String value) {
        this.key=key;
        this.value=value;
    }

    /**
     * 获取value列表
     * @return
     */
    public static List<String> getValues(){
        return Arrays.stream(values()).map(item->item.value).collect(Collectors.toList());
    }

    /**
     * 根据key获取枚举
     * @param key
     * @return
     */
    public static ProtocolMessageSerializerEnum getEnumByKey(int key){
        for (ProtocolMessageSerializerEnum anEnum : ProtocolMessageSerializerEnum.values()) {
            if (anEnum.key==key){
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static ProtocolMessageSerializerEnum getEnumByValue(String value){
        for (ProtocolMessageSerializerEnum anEnum : ProtocolMessageSerializerEnum.values()) {
            if (StrUtil.equals(anEnum.value,value)){
                return anEnum;
            }
        }
        return null;
    }
}
