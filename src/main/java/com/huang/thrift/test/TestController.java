package com.huang.thrift.test;

import com.huang.thrift.annotation.ThriftClient;
import org.apache.thrift.TException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @ThriftClient("userService")
    private UserService.Iface userService;

    @GetMapping("/test")
    public void test() throws TException {
        User user = userService.getUser(1);
        System.out.println(user);
    }

}
