package com.samoyer.rpc.protocol;

import com.samoyer.rpc.serializer.Serializer;
import com.samoyer.rpc.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

/**
 * 编码器
 * @author Samoyer
 * @since 2024-08-14
 */
public class ProtocolMessageEncoder {

    /**
     * 编码
     * 包括对消息的序列化
     * 传进ProtocolMessage，编码成Buffer
     * @param protocolMessage
     * @return
     * @throws IOException
     */
    public static Buffer encode(ProtocolMessage<?> protocolMessage) throws IOException {
        //如果消息为空或者消息头为空
        if (protocolMessage==null||protocolMessage.getHeader()==null){
            return Buffer.buffer();
        }
        ProtocolMessage.Header header=protocolMessage.getHeader();
        //依次向缓冲区中写入字节
        Buffer buffer = Buffer.buffer();
        buffer.appendByte(header.getMagic());
        buffer.appendByte(header.getVersion());
        buffer.appendByte(header.getSerializer());
        buffer.appendByte(header.getType());
        buffer.appendByte(header.getStatus());
        buffer.appendLong(header.getRequestId());
        //获取序列化器
        //根据key获取枚举
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum==null){
            throw new RuntimeException("序列化消息的协议不存在");
        }
        //根据枚举的value("jdk")，创建序列化器实例
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        //序列化
        byte[] bodyBytes = serializer.serialize(protocolMessage.getBody());
        //写入body长度和数据
        buffer.appendInt(bodyBytes.length);
        buffer.appendBytes(bodyBytes);

        return buffer;
    }
}
