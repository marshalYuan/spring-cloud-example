package com.codedocker.springcloud;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

/**
 * Created by marshal on 16/9/27.
 */

@FeignClient(name = "node-sidecar")
public interface BookFeignClient {
    @RequestMapping("/books")
    public List<Book> findByUid(@RequestParam("uid") Long id);
}
