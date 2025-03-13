package com.huang.thrift.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Test {

    public static class Client {



    }

    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<Client> constructor = Client.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        System.out.println(constructor.newInstance().getClass());
    }
}
