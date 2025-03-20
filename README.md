![Ali Baba's Secret Cave](./assets/header.webp)

# Ali Baba and The Forty Thieves

[Ali Baba and the Forty Thieves](https://en.wikipedia.org/wiki/Ali_Baba_and_the_Forty_Thieves) is a famous tale from *One Thousand and One Nights* (*Arabian Nights*). Ali Baba, a poor woodcutter, accidentally discovers a hidden cave belonging to a group of thieves. He overhears their leader say the magic words, “Open, Simsim!” (or “Open Sesame!”), to reveal a treasure-filled cavern. After the thieves leave, Ali Baba enters, takes some treasure, and returns home.

The story originates from the medieval *Arabic One Thousand and One Nights*, though it was likely added by the French translator Antoine Galland in the 18th century. It reflects Middle Eastern oral storytelling traditions, emphasizing themes of fate, cleverness, and moral justice. The tale has endured as a classic of folklore, influencing literature, films, and popular culture worldwide.

## This Project

This project demonstrates authenticating against Okta (auth0.com) using their free developer accounts.  

The Angular SPA (`ui/ng-ui`) has both protected and unprotected routes, and will redirect to auth0.com login page.  Once a JWT is obtained, it will use that for calling protected services (`services/*`).

The services (Spring or Quarkus) serve an unprotected endpoint (`/public`), mainly to deliver application configuring to the client ui app.  Other resources (`/api`) are protected, and use the configured Auth0 Client Domain as the issuer URI for validating the JWT token.

## Okta/Auth0

Head over to [okta.com](https://okta.com)/[auth0.com](https://auth0.com) and create a developer account.  

<details>
  <summary>Expand to view Auth0/Okta application setup</summary>

### Create Auth0 Application

Once in your dashboard, create a new appliatiom for "single-page applications."

`Applications`->`Applications`->`Create Application`

* Name: `Ali Baba's Secret Treasure`

![Create Application](./assets/okta/01-create-application.png)

### Select Angular

![Choose Angular](./assets/okta/02-create-application-choose-angular.png)

### Auth0 App Settings

Read more from [Auth0's Documentation](https://auth0.com/docs/quickstart/spa/angular/interactive)

> [!WARNING]  
> When using the Default App with a Native or Single Page Application, ensure to update the Token Endpoint Authentication Method to None and set the Application Type to either SPA or Native.

### Configure Callback URLs

* **Allowed Callback URLs**: `http://localhost:4200`
* **Allowed Logout URLs**: `http://localhost:4200`
* **Allowed Allowed Web Origins**: `http://localhost:4200`

Make note of the following information, which you'll need to configure the services and Angular applications.

* **Domain**
* **Client ID**

![Note Client-Id and Domain](./assets/okta/02-note-client-id-and-domain.png)

### Create and Configure API

`Applications`->`API`->`Create API`

* Name: `ali-baba`
* Identifier: `http://localhost:4200/api`

![Create API](./assets/okta/03-create-api-ali-baba.png)

Add the following permissions:

* `see:thieves-treasure`
* `see:alibaba-treasure`
* `take:thieves-treasure`

![API Permissions](./assets/okta/04-add-api-permissions.png)

Make note of the `Identifier`/`audience`.

### Create Role

`User Management`->`Roles`->`Create Role`

* Name: `treasure-hunter`

![Create Role](./assets/okta/05-create-treasure-hunter-role.png)

Add API permissions to roles:


![Add Role Permissions](./assets/okta/06-add-role-permissions.png)

### Create User

`User Management`->`Users`->`Create User`

![Create User](./assets/okta/07-1-create-user-new-user.png)

Assign User Roles:

![Assign User Roles](./assets/okta/07-2-edit-user-assign-role.png)

View inherited permissions:

![View User permissions](./assets/okta/07-3-view-user-permissions.png)

### Create Login Trigger Action

A login trigger is needed to modify the tokens to include the user roles, otherwise the tokens will only contain permissions.  Read more about adding roles in Auth0's documentation [Add user roles to tokens](https://auth0.com/docs/manage-users/access-control/sample-use-cases-actions-with-authorization#add-user-roles-to-tokens).

`Actions`->`Trigger`->`post-login`


![post-login trigger](./assets/okta/08-create-post-login-trigger.png)

Add Action, choose `Build from scratch`:

![add action](./assets/okta/09-choose-add-action.png)

Create Trigger Action:

* Name: `Add Roles To Tokens`
* Trigger: `Login / Post Login`
* Runtime: Recommended Node version

![create action](./assets/okta/10-create-trigger-action.png)

Past the following code, but that the `namespace` with whatever you want:

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'your-namespace.example.com'; // Can be anything
  if (event.authorization) {
    api.idToken.setCustomClaim(`${namespace}/roles`, event.authorization.roles);
    api.accessToken.setCustomClaim(`${namespace}/roles`, event.authorization.roles);  
  }
}
```

![create action](./assets/okta/11-paste-trigger-code.png)

After you save, the action wil appear to right of the Post Login trigger pipeline.  Drag-and-drop it to the pipeline:

![add action to trigger pipeline](./assets/okta/12-add-action-to-trigger.png)

Save changes.

</details>

## Running Services

Rename `./scripts/auth0-config-sample.sh` to `./scripts/auth0-config.sh` and set the configurations used/generated with the Auth0 configuration:

```shell
AUTH0_DOMAIN="client-domain"
AUTH0_EMAIL="email@example.com"
AUTH0_PASSWORD="yourpassword"
AUTH0_AUDIENCE="identifier (audience)"
AUTH0_CLIENT_ID="client-id"
```

### Spring Boot

Following instructions at [./services/spring-boot-ali-babas-secret/README.md](./services/spring-boot-ali-babas-secret/README.md)

