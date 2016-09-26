package com.codedocker.springcloud;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by marshal on 16/9/26.
 */
@RestController
public class UserController {

    @Autowired
    private DiscoveryClient discoveryClient;
    private HashMap<Long, User> users;

    public UserController() {
        users = new HashMap<Long, User>();
        int count = 100;
        while (count > 0) {
            User user = new User();
            user.setId(Long.valueOf(count));
            user.setAge(10 + (int)(Math.random()*10)); //10 - 20
            user.setUsername("user"+ count);
            users.put(Long.valueOf(count), user);
            count--;
        }
    }

    /**
     * 查询user
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        User user = users.get(id);
        if (user == null) {
            return new User();
        }
        return user;
    }

    /**
     * 本地服务实例的信息
     * @return
     */
    @GetMapping("/info")
    public ServiceInstance showInfo() {
        ServiceInstance localServiceInstance = this.discoveryClient.getLocalServiceInstance();
        return localServiceInstance;
    }
}

class User {
    private Long id;
    private String username;
    private Integer age;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
