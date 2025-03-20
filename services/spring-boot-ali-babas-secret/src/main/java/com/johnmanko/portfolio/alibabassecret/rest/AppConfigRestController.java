package com.johnmanko.portfolio.alibabassecret.rest;

import com.johnmanko.portfolio.alibabassecret.models.AppConfigModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class AppConfigRestController {

    @Value("${app.config.client.auth.auth0.domain}")
    private String authAuth0Domain;

    @Value("${app.config.client.auth.auth0.client-id}")
    private String authAuth0ClientId;

    @GetMapping("/config.json")
    public AppConfigModel getAppConfig() {
        return new AppConfigModel(authAuth0Domain, authAuth0ClientId);
    }

}
