package com.huang.thrift.factory;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

public class ThriftConnectionFactory extends BasePooledObjectFactory<TSocket> {
    private final String host;
    private final int port;
    private final int timeout;

    public ThriftConnectionFactory(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    public TSocket create() throws TTransportException {
        TSocket socket = new TSocket(host, port, timeout);
        socket.open();
        return socket;
    }

    @Override
    public PooledObject<TSocket> wrap(TSocket socket) {
        return new DefaultPooledObject<>(socket);
    }

    @Override
    public boolean validateObject(PooledObject<TSocket> p) {
        TSocket socket = p.getObject();
        return socket.isOpen() && socket.getSocket().isConnected();
    }

    @Override
    public void destroyObject(PooledObject<TSocket> p) {
        TSocket socket = p.getObject();
        if (socket.isOpen()){
            socket.close();
        }
    }

}
