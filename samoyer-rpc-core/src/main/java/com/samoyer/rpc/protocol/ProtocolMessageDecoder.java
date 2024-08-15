package com.samoyer.rpc.protocol;

import com.samoyer.rpc.model.RpcRequest;
import com.samoyer.rpc.model.RpcResponse;
import com.samoyer.rpc.serializer.Serializer;
import com.samoyer.rpc.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

import java.io.IOException;

/**
 * 解码器
 * @author Samoyer
 * @since 2024-08-14
 */
public class ProtocolMessageDecoder {

    /**
     * 解码
     * 传进Buffer，解码到ProtocolMessage对象
     * @param buffer
     * @return
     */
    public static ProtocolMessage<?> decode(Buffer buffer) throws IOException {
        //分别从指定位置读出Buffer
        //解析消息头
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        byte magic = buffer.getByte(0);
        //校验魔数
        if (magic!=ProtocolConstant.PROTOCOL_MAGIC){
            throw new RuntimeException("消息 magic 非法");
        }
        //从buffer中读取对应的信息到新new的Header对象里
        header.setMagic(magic);
        header.setVersion(buffer.getByte(1));
        header.setSerializer(buffer.getByte(2));
        header.setType(buffer.getByte(3));
        header.setStatus(buffer.getByte(4));
        header.setRequestId(buffer.getLong(5));
        //long占8个字节，bodyLength从13开始读
        header.setBodyLength(buffer.getInt(13));
        //读取数据。解决粘包半包问题，只读指定长度的数据(bodyLength)
        byte[] bodyBytes = buffer.getBytes(17, 17 + header.getBodyLength());

        //解析消息体
        //header.getSerializer()为1，2，3，4，根据key获取到"json"...
        ProtocolMessageSerializerEnum serializerEnum = ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum==null){
            throw new RuntimeException("序列化消息的协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        ProtocolMessageTypeEnum messageTypeEnum = ProtocolMessageTypeEnum.getEnumByKey(header.getType());
        if (messageTypeEnum==null){
            throw new RuntimeException("序列化消息的类型不存在");
        }

        switch (messageTypeEnum){
            case REQUEST:
                RpcRequest request = serializer.deserialize(bodyBytes, RpcRequest.class);
                return new ProtocolMessage<>(header,request);
            case RESPONSE:
                RpcResponse response = serializer.deserialize(bodyBytes, RpcResponse.class);
                return new ProtocolMessage<>(header,response);
            case HEART_BEAT:
            case OTHERS:
            default:
                throw new RuntimeException("暂不支持该消息类型");
        }
    }
}
