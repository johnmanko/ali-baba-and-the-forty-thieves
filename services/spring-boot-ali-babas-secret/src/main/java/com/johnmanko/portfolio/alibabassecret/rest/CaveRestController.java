package com.johnmanko.portfolio.alibabassecret.rest;

import com.johnmanko.portfolio.alibabassecret.models.TreasureModel;
import com.johnmanko.portfolio.alibabassecret.services.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 *
 * For method security, we can use the @PreAuthorize annotation to secure individual methods.
 * Reference:
 * https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html#authorization-expressions
 * https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#use-preauthorize
 * https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html#_the_authorizationmanager
 */
@RestController
@RequestMapping("/api/cave")
public class CaveRestController {

    @Autowired
    private RedisService redis;

    @GetMapping(value="/authorities", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> getPrincipalInfo(JwtAuthenticationToken principal) {

        Collection<String> authorities = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String,Object> info = new HashMap<>();
        info.put("name", principal.getName());
        info.put("authorities", authorities);

        return info;
    }

    @GetMapping(value="/thieves-treasure", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('treasure-hunter')")
    public CompletableFuture<TreasureModel> getTreasureCount() {
        return getTreasure("thieves-treasure", 1000);
    }

    @GetMapping(value="/alibaba-treasure", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_see:alibaba-treasure')")
    public CompletableFuture<TreasureModel> getAliBabasTreasureCount() {
        return getTreasure("alibaba-treasure", 0);
    }

    @PostMapping(value="/take-treasure",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SCOPE_take:thieves-treasure')")
    public CompletableFuture<Map<String, Integer>> takeTreasure(@RequestBody TreasureModel takeTreasure) {
        return CompletableFuture.supplyAsync(() -> {

            CompletableFuture<TreasureModel> thievesCountFuture = getTreasureCount();
            CompletableFuture<TreasureModel> alibabaCountFuture = getAliBabasTreasureCount();

            // Wait for both to complete and retrieve results
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(thievesCountFuture, alibabaCountFuture);

            // Block until all are done
            combinedFuture.join();

            TreasureModel thievesTreasure = null;
            TreasureModel alibabaTreasure = null;
            try {
                thievesTreasure = thievesCountFuture.get();
                alibabaTreasure = alibabaCountFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (thievesTreasure.amount() < takeTreasure.amount()) {
                throw new IllegalArgumentException("Not enough treasure to take");
            }

            alibabaTreasure = new TreasureModel(alibabaTreasure.owner(), alibabaTreasure.amount() + takeTreasure.amount());
            thievesTreasure = new TreasureModel(thievesTreasure.owner(), thievesTreasure.amount() - takeTreasure.amount());

            redis.saveToRedis(alibabaTreasure.owner(), alibabaTreasure.amount(), 60);
            redis.saveToRedis(thievesTreasure.owner(), thievesTreasure.amount(), 60);
            Map<String, Integer> results = new HashMap<>();
            results.put(alibabaTreasure.owner(), alibabaTreasure.amount());
            results.put(thievesTreasure.owner(), thievesTreasure.amount());
            return results;
        });
    }

    private CompletableFuture<TreasureModel> getTreasure(String key, Integer initialValue) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return redis.getFromRedis(key)
                        .thenApply((value) -> {
                            if (value.isEmpty()) {
                                redis.saveToRedis(key, initialValue, 60);
                            }
                            return value.orElse(initialValue);
                        }).thenApply(amount -> new TreasureModel(key, amount)).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
