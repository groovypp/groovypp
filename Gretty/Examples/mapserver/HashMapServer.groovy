@Typed package mapserver

import org.mbte.gretty.AbstractServer
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.channel.Channel
import org.mbte.gretty.AbstractClient
import org.jboss.netty.buffer.DynamicChannelBuffer
import groovypp.channels.ExecutingChannel
import java.util.concurrent.Executors
import org.jboss.netty.channel.ExceptionEvent
import java.util.concurrent.ConcurrentHashMap
import org.jboss.netty.buffer.HeapChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers

abstract class Command {
    Channel channel

    abstract void execute(Map map)
}

class SET extends Command {
    String key, value

    void execute(Map map) {
        map.put(key, value)
        def buffer = new DynamicChannelBuffer(4)
        buffer.writeInt(0)
        channel.write(buffer)
    }

    static SET read(ChannelBuffer buffer) {
        def start = buffer.readerIndex()

        if (buffer.readableBytes() < 4) {
            buffer.readerIndex(start - 4)
            return null;
        }

        def keyLen = buffer.readInt();

        if (buffer.readableBytes() < keyLen + 4) {
            buffer.readerIndex(start - 4)
            return null;
        }

        def ri = buffer.readerIndex();
        buffer.skipBytes(keyLen);

        def valueLen = buffer.readInt();

        if (buffer.readableBytes() < valueLen) {
            buffer.readerIndex(start - 4)
            return null;
        }

        buffer.skipBytes(valueLen)

        def keyBytes = new byte[keyLen]
        buffer.getBytes(ri, keyBytes)

        def valueBytes = new byte[valueLen]
        buffer.getBytes(ri + 4 + keyLen, valueBytes)

        return new SET(key: new String(keyBytes, "UTF-8"), value: new String(valueBytes, "UTF-8"))
    }

    static ChannelBuffer write(String key, String value) {
        def keyLen = key.utf8Length()
        def valueLen = value.utf8Length()
        def buffer = ChannelBuffers.buffer(4+4+keyLen+4+valueLen)
        buffer.writeInt(0)
        buffer.writeUtf8(key, keyLen)
        buffer.writeUtf8(value, valueLen)
        buffer
    }
}

class GET extends Command {
    String key


    static ChannelBuffer write(String key) {
        def keyLen = key.utf8Length()
        def buffer = ChannelBuffers.buffer(4+4+keyLen)
        buffer.writeInt(1)
        buffer.writeUtf8(key, keyLen)
        buffer
    }

    static GET read(ChannelBuffer buffer) {
        def start = buffer.readerIndex ()

        if(buffer.readableBytes() < 4) {
            buffer.readerIndex(start-4)
            return null;
        }

        def keyLen = buffer.readInt();

        if(buffer.readableBytes() < keyLen) {
            buffer.readerIndex(start-4)
            return null;
        }

        def keyBytes = new byte[keyLen]
        buffer.readBytes(keyBytes)

        return new GET(key:new String(keyBytes, "UTF-8"))
    }

    void execute(Map map) {
        def value = map.get(key)

        def buffer = new DynamicChannelBuffer(4)
        if(value != null) {
            def wi = buffer.writerIndex()
            buffer.writeInt(0)
            buffer.writeBytes(key.getBytes("UTF-8"))
            buffer.setInt(wi, buffer.writerIndex()-wi-4)

            channel.write(buffer)
        }
        else {
            def wi = buffer.writerIndex()
            buffer.writeInt(-1)
            channel.write(buffer)
        }
    }
}

class MapServer extends AbstractServer {
    ExecutingChannel commander

    final ConcurrentHashMap map = [:]

    MapServer() {
        localAddress = new InetSocketAddress(8080)
        commander = [
                executor: Executors.newSingleThreadExecutor(),
                onMessage: { msg ->
                    ((Command)msg).execute(map)
                }
        ]
    }

    protected void buildPipeline(ChannelPipeline pipeline) {
        pipeline.addLast("decoder",   new CommandDecoder())
        pipeline.addLast("processor", this)
        pipeline
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Command msg = e.message
        msg.channel = ctx.channel
        msg.execute(map)
//        commander << msg
    }
}

class CommandDecoder extends FrameDecoder {
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
        if(buffer.readableBytes() < 4)
            return null

        def code = buffer.readInt ()

        if(code == 0)
           return SET.read(buffer)

        if(code == 1)
           return GET.read(buffer)

        throw new RuntimeException("Corrupted stream")
    }
}

class MapClient extends AbstractClient {

    MapClient() {
        super(new InetSocketAddress(8080));
    }

    void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.cause.printStackTrace()
        super.exceptionCaught(ctx, e)
    }

    void write(Object obj) {
        channel.write(obj)
    }
}

String [] args = binding.variables.args

if(args.length == 0) {
    println """\
Usage: mapserver [server|client]
"""
}

if(args[0] == "server") {
    new MapServer().start()
}
else {
    if(args[0] == "client") {
        def client = new MapClient()
        client.connect ().await()

        def start = System.currentTimeMillis()
        def N = 200000
        for(i in 0..<N) {
            def key = "foo$i"
            def value = "bar$i"

            def buf = SET.write(key, value)
            client.write(buf)

            buf = GET.write(key)
            client.write(buf)

            if(i % 10000 == 0)
               println i
        }
        def elapsed = System.currentTimeMillis() - start
        println "done in ${elapsed} ms : ${(N * 1000 * 2) / elapsed} ops"
        System.exit(0)
    }
}

static int utf8Length (String str) {
    int strLen = str.length(), utfLen = 0;
    for(int i = 0; i != strLen; ++i) {
        char c = str.charAt(i);
        if (c < 0x80) {
            utfLen++;
        } else if (c < 0x800) {
            utfLen += 2;
        } else if (isSurrogate(c)) {
            i++;
            utfLen += 4;
        } else {
            utfLen += 3;
        }
    }
    return utfLen;
}

private static boolean isSurrogate(char ch) {
    return ch >= Character.MIN_SURROGATE && ch <= Character.MAX_SURROGATE;
}

static void writeUtf8(ChannelBuffer buffer, String str, int len = -1) {
    int strLen = str.length();

    if(len < 0)
        len = str.utf8Length()

    buffer.ensureWritableBytes(len+4)
    buffer.writeInt(len)

    def wi = buffer.writerIndex()

    int i;
    for (i = 0; i < strLen; i++) {
        char c = str.charAt(i)
        if (!(c < 0x80)) break;
        buffer.setByte(wi++, c);
    }

    for (; i < strLen; i++) {
        char c = str.charAt(i);
        if (c < 0x80) {
            buffer.writeByte(c);
        } else if (c < 0x800) {
            buffer.setByte(wi++, ((byte)(0xc0 | (c >> 6))))
            buffer.setByte(wi++, (byte)(0x80 | (c & 0x3f)))
        } else if (isSurrogate(c)) {
            int uc = Character.toCodePoint(c, str.charAt(i++))
            buffer.setByte(wi++, ((byte)(0xf0 | ((uc >> 18)))))
            buffer.setByte(wi++, ((byte)(0x80 | ((uc >> 12) & 0x3f))))
            buffer.setByte(wi++, ((byte)(0x80 | ((uc >> 6) & 0x3f))))
            buffer.setByte(wi++, ((byte)(0x80 | (uc & 0x3f))))
        } else {
            buffer.setByte(wi++, (byte)(0xe0 | ((c >> 12))));
            buffer.setByte(wi++, (byte)(0x80 | ((c >> 6) & 0x3f)));
            buffer.setByte(wi++, ((byte)(0x80 | (c & 0x3f))))
        }
    }
    buffer.writerIndex(wi)
}
