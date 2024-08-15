package com.samoyer.rpc.server.tcp;

import com.samoyer.rpc.protocol.ProtocolConstant;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

/**
 * 装饰者模式（使用recordParser对原有的RecordParser的处理能力进行增强
 * @author Samoyer
 * @since 2024-08-15
 */
public class TcpBufferHandlerWrapper implements Handler<Buffer> {

    private final RecordParser recordParser;

    public TcpBufferHandlerWrapper(Handler<Buffer> bufferHandler) {
        recordParser=initRecordParser(bufferHandler);
    }

    /**
     * NetSocket收到数据后进行的操作
     * @param buffer
     */
    @Override
    public void handle(Buffer buffer) {
        recordParser.handle(buffer);
    }

    /**
     * 自定义一个parser，增强原有的能力
     * @param bufferHandler
     * @return
     */
    private RecordParser initRecordParser(Handler<Buffer> bufferHandler){
        //构造parser
        RecordParser parser = RecordParser.newFixed(ProtocolConstant.MESSAGE_HEADER_LENGTH);

        //用来设置一个输出处理器，
        //这个输出处理器就是TcpServerHandler中 buffer -> { ... } 这个 lambda 表达式
        parser.setOutput(new Handler<Buffer>() {
            //初始化
            int size=-1;
            //完整的读取（头+体）
            Buffer resultBuffer = Buffer.buffer();

            @Override
            public void handle(Buffer buffer) {
                //消息头
                if (size==-1){
                    //读取消息头中的消息体长度
                    size=buffer.getInt(13);
                    parser.fixedSizeMode(size);
                    //写入头信息到结果
                    resultBuffer.appendBuffer(buffer);
                }else {
                    //写入体消息到结果
                    resultBuffer.appendBuffer(buffer);
                    //已拼接为完整Buffer，执行处理
                    bufferHandler.handle(resultBuffer);
                    //重置一轮
                    parser.fixedSizeMode(ProtocolConstant.MESSAGE_HEADER_LENGTH);
                    size=-1;
                    resultBuffer=Buffer.buffer();
                }
            }
        });

        return parser;
    }
}
