package com.samoyer.rpc.protocol;

import lombok.Getter;

/**
 * 协议消息的状态枚举
 * 暂时只有：成功、请求失败、响应失败
 * @author Samoyer
 * @since 2024-08-14
 */
@Getter
public enum ProtocolMessageStatusEnum {
    //成功
    OK("ok",20),
    //请求失败
    BAD_REQUEST("badRequest",40),
    //响应失败
    BAD_RESPONSE("badResponse",50);

    private final String text;
    private final int value;

    ProtocolMessageStatusEnum(String text, int value) {
        this.text=text;
        this.value=value;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static ProtocolMessageStatusEnum getEnumByValue(int value){
        for (ProtocolMessageStatusEnum anEnum : ProtocolMessageStatusEnum.values()) {
            if (anEnum.value==value){
                return anEnum;
            }
        }
        return null;
    }
}
