package org.lite.mesh.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.lite.mesh.model.EurekaInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/mesh")
public class HomeController {

    @Autowired
    EurekaClient eurekaClient;

    @GetMapping(value = "/home", name = "home")
    public String home(Model model){

        List<EurekaInstance> instances = new ArrayList<>();
        eurekaClient.getApplications().getRegisteredApplications().forEach(application -> {
            List<InstanceInfo> infoList = application.getInstances();
            infoList.forEach(instanceInfo -> {
                instances.add(new EurekaInstance(instanceInfo.getId(), instanceInfo.getAppName(), instanceInfo.getStatus().name()));
            });
        });

        model.addAttribute("user", "John Doe");
        model.addAttribute("module", "home");
        model.addAttribute("instances", instances);
        return "index";
    }
}
