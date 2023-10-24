package com.kipu_fav.read_module.Controller;

import com.kipu_fav.read_module.Entity.RedisResponse;
import com.kipu_fav.read_module.Entity.Schedule;
import com.kipu_fav.read_module.Entity.UserQuery;
import com.kipu_fav.read_module.Service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/redis")
public class Controller {
    @Autowired
    Service service;
    @GetMapping("/starter")
    public String starter() throws ParseException {
        this.service.starter();
        return "Starter executed successfully!!!";
    }

    @GetMapping("/all")
    public List<Schedule> getAll(){
        return this.service.getAllEntries();
    }

    @GetMapping("/query")
    public List<RedisResponse> query(@RequestBody UserQuery userQuery) throws IOException, ParseException {
      return this.service.querySlots(userQuery);
    }

}
