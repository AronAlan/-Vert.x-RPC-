package com.samoyer.rpc.protocol;

import cn.hutool.core.util.IdUtil;
import com.samoyer.rpc.constant.RpcConstant;
import com.samoyer.rpc.model.RpcRequest;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;

import java.io.IOException;

/**
 * 自定义协议，编码解码测试
 * @author Samoyer
 * @since 2024-08-14
 */
public class ProtocolMessageTest {
    @Test
    public void testEncodeAndDecode() throws IOException {
        //构造消息
        ProtocolMessage<RpcRequest> requestMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer((byte) ProtocolMessageSerializerEnum.JDK.getKey());//传的是1
        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
        header.setStatus((byte) ProtocolMessageStatusEnum.OK.getValue());
        header.setRequestId(IdUtil.getSnowflakeNextId());//基于雪花算法生成全局唯一的id
        header.setBodyLength(0);//这里不需要人工确定，后面编码的时候会对消息体进行序列化然后获取长度，并set到bodyLength

        //构造请求
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName("myService");
        rpcRequest.setMethodName("myMethod");
        rpcRequest.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        rpcRequest.setParameterTypes(new Class[]{String.class});
        rpcRequest.setArgs(new Object[]{"aaa","bbb"});

        requestMessage.setHeader(header);
        requestMessage.setBody(rpcRequest);
        //编码
        Buffer encodeBuffer = ProtocolMessageEncoder.encode(requestMessage);

        //解码
        ProtocolMessage<?> decodeMessage = ProtocolMessageDecoder.decode(encodeBuffer);
        System.out.println(decodeMessage);
    }
}
