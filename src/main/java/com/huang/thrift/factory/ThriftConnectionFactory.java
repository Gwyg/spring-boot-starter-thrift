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
        TSocket socket = new TSocket(host, port, timeout);
        TTransport transport = new TFramedTransport(socket);
        transport.open();
        return transport;
    }

    @Override
    public PooledObject<TTransport> wrap(TTransport transport) {
        return new DefaultPooledObject<>(transport);
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
        if (p == null) {
            return;
        }
        TTransport transport = p.getObject();
        if (transport != null) {
            try {
                if (transport.isOpen()) {
                    transport.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

}
