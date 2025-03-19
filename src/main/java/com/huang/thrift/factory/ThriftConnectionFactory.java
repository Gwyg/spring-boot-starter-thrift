package com.huang.thrift.factory;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;

public class ThriftConnectionFactory extends BasePooledObjectFactory<TTransport> {
    private final String host;
    private final int port;
    private final int timeout;

    public ThriftConnectionFactory(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public TTransport create() throws TTransportException {
        TTransport socket = new TSocket(host, port, timeout);
        socket = new TFramedTransport(socket);
        socket.open();
        return socket;
    }

    @Override
    public PooledObject<TTransport> wrap(TTransport socket) {
        return new DefaultPooledObject<>(socket);
    }

    @Override
    public boolean validateObject(PooledObject<TTransport> p) {
        if (p == null || p.getObject() == null){
            return false;
        }
        TTransport socket = p.getObject();
        return socket.isOpen();
    }

    @Override
    public void destroyObject(PooledObject<TTransport> p) {
        TTransport socket = p.getObject();
        if (socket.isOpen()){
            socket.close();
        }
    }

}
