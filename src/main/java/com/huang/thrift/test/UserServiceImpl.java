package com.huang.thrift.test;

import com.huang.thrift.annotation.ThriftService;

@ThriftService("userService")
public class UserServiceImpl implements UserService.Iface{
    @Override
    public User getUser(int id) {
        System.out.println("getUser");
        return new User(id, "huang", "huang@huang.com");
    }
}
