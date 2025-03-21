# Spring Boot Ali Baba's Secret

This project uses OpenID Connect (OIDC) to grant access to rest endpoints. The project is a simple Spring Boot application that uses Auth0 as the OIDC provider.

A few notes on all the moving parts.

## Moving Parts

### OAuth Resource Server

First, this project is an **OAuth2 Resource Server** (maven's `spring-boot-starter-oauth2-resource-server` dependency), and therefore ACCESS tokens are validated against Auth0's servers.  The `spring.security.oauth2.resourceserver.*` properties (see configuration below) configures the applications with the information needed to make that validation possible.

Read more about the Resource Server at [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

The Resource Server should not be confused with the Authorization Server

| Feature | OAuth2 Resource Server (API) | OAuth2 Authorization Server |
| --- | --- | --- |
| Primary Role | Protects APIs & validates tokens | Issues OAuth2 access tokens |
| Dependency | spring-boot-starter-oauth2-resource-server | spring-boot-starter-oauth2-authorization-server |
| Who Uses It? | API servers / microservices | Authentication providers |
| Requires Another Auth Server? | Yes, needs an external OAuth2 provider | No, it is the OAuth2 provider |
| Common Use Case | Protecting REST APIs with JWT tokens | Custom OAuth2 identity provider |
| Example Providers | Keycloak, Okta, Auth0, Custom | Spring Authorization Server, Keycloak |

---

The project includes `spring-boot-docker-compose`, which will detect the provided `compose.yaml` and launch a local docker stack with a Redis server exposed on the host's port `6379`.  If you have a service running on that port, be sure to stop it before running this server.  Otherwise, delete the `componse.yaml` file and update the `spring.redis.*` properties accordingly.

---

Spring Boot 3+ and Spring Security 6+ automatically configure `SecurityFilterChain` if one exists. `@EnableWebSecurity` was required in older versions of Spring Security, but in Spring Boot 3+, simply defining a `SecurityFilterChain` is enough.
Method security (`@EnableMethodSecurity`) only controls method-level access but does not disable HTTP security.

`@EnableMethodSecurity` vs. `@EnableWebSecurity` in Spring Security

Both annotations are used to configure security in a Spring Boot application, but they serve different purposes:

| Annotation | Purpose | Scope | Common Use Cases |
| --- | --- | --- | --- | 
| @EnableWebSecurity | Configures HTTP security | Web requests (filter-based security) | Securing URLs, CSRF, CORS, session management |
| @EnableMethodSecurity | Enables method-level security | Method calls (@PreAuthorize, @Secured, @RolesAllowed) | Role-based access control at service/controller level |


* Annotation Interface [EnableWebSecurity](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/config/annotation/web/configuration/EnableWebSecurity.html)
* Annotation Interface [EnableMethodSecurity](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/config/annotation/method/configuration/EnableMethodSecurity.html)


---

Reading resources:

* Spring Security [CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
* Spring Data [Redis](https://spring.io/projects/spring-data-redis)
* Spring Security [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
* [How JWT Authentication Works](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-architecture)
* Spring Security [HttpSecurity](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#jc-httpsecurity)
* Spring Security [SecurityFilterChain](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-securityfilterchain)
* Spring Security [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
* Spring Security [Authorize HttpServletRequests](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
* [Java Configuration](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#jc-hello-wsca)
* [@EnableWebSecurity](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-provide-securityfilterchain-bean)
* [Testing Method Security](https://docs.spring.io/spring-security/reference/servlet/test/method.html)
* [Working with Objects through RedisTemplate](https://docs.spring.io/spring-data/redis/reference/redis/template.html)

## Configuration

Copy `application.properties` to `application-dev.properties` and update the auth configurations

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://<client-subdomain>.us.auth0.com/
app.config.client.auth.auth0.domain=https://<client-subdomain>.us.auth0.com/
app.config.client.auth.auth0.client-id=<client-id>
app.config.server.auth.auth0.custom-jwt-namespace=<trigger-action-namespace/roles>
```

## Running the application

This project includes the `org.springframework.boot:spring-boot-docker-compose` dependency that will recognize the provided compose.yaml file that starts a Redis database on localhost:6379. The Redis database is used to store treasure counts.

```shell
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Test endpoing

### Curl scripting

From the root of this repo, run the `auth0-fetch-token.sh` test"

(if you haven't already)
```shell
chmod +x ./scripts/auth0-fetch-token.sh
```

```shell
cd scripts
./auth0-fetch-token.sh
```

Output should look like:

`/public/config.json`:
```json
{
  "authAuth0Domain": "<client-subdomain>.us.auth0.com",
  "authAuth0ClientId": "<client-id>"
}
```


`/cave/authorities`:
```json
{
  "name": "auth0|xxxxxxxxxxxxxxxx",
  "authorities": [
    "ROLE_treasure-hunter",
    "SCOPE_see:alibaba-treasure",
    "SCOPE_see:thieves-treasure",
    "SCOPE_take:thieves-treasure"
  ]
}
```

`/cave/thieves-treasure`:
```json
{
  "owner": "thieves-treasure",
  "amount": 1000
}
```

`cave/ali-babas-treasure`:
```json
{
  "owner": "ali-babas-treasure",
  "amount": 0
}
```

`cave/take-treasure`:
```json
[
  {
    "owner": "ali-babas-treasure",
    "amount": 20
  },
  {
    "owner": "thieves-treasure",
    "amount": 980
  }
]
```

### Maven tests

> [!NOTE]
> There is a problem with using WebTestClient and mocking JWT
> See the following issues:
> [spring-projects/spring-security/issues/9304](https://github.com/spring-projects/spring-security/issues/9304#issuecomment-841495717) (`archi-hub-85`'s comment solution works and was implemented as a workaround in the included tests)
> [spring-projects/spring-security/issues/9257](https://github.com/spring-projects/spring-security/issues/9257)
 
```shell
./mvnw clean compile test
```

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] +--Testing '/api/cave/*' API - 4.500 s
[INFO] |  +-- [OK] GET /api/cave/authorities (Authorized) - 0.138 s
[INFO] |  +-- [OK] GET /api/cave/thieves-treasure (Unauthorized) - 0.012 s
[INFO] |  +-- [OK] GET /api/cave/thieves-treasure (Authorized) - 0.081 s
[INFO] |  +-- [OK] GET /api/cave/alibaba-treasure (Unauthorized) - 0.007 s
[INFO] |  +-- [OK] GET /api/cave/alibaba-treasure (Authorized) - 0.012 s
[INFO] |  +-- [OK] POST /api/cave/take-treasure (Unauthorized) - 0.012 s
[INFO] |  '-- [OK] POST /api/cave/take-treasure (Authorized) - 0.033 s
[INFO] +--Testing '/public/*' API - 1.146 s
[INFO] |  '-- [OK] GET /public/config.json - 0.008 s
[INFO] +--E2E Testing '/public/*' API - 2.137 s
[INFO] |  '-- [OK] GET /public/config.json - 0.243 s
[INFO] +--E2E Testing '/api/cave/*' API - 1.354 s
[INFO] |  +-- [OK] GET /api/cave/authorities (Authorized) - 0.018 s
[INFO] |  +-- [OK] GET /api/cave/thieves-treasure (Unauthorized) - 0.021 s
[INFO] |  +-- [OK] GET /api/cave/thieves-treasure (Authorized) - 0.286 s
[INFO] |  +-- [OK] GET /api/cave/alibaba-treasure (Unauthorized) - 0.007 s
[INFO] |  +-- [OK] GET /api/cave/alibaba-treasure (Authorized) - 0.009 s
[INFO] |  +-- [OK] POST /api/cave/take-treasure (Unauthorized) - 0.006 s
[INFO] |  '-- [OK] POST /api/cave/take-treasure (Authorized) - 0.022 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0

```

