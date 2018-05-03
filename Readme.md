# How to use


 ###start redis and mongodb by using docker
    
    docker-compose up -d
 
 ###run app by using sbt
    
    sbt run .
    
###prepare init data

```
RUN demo-application/test/endpoint/OAuthEndPointDataInit.scala
```
 
Try to create access tokens using curl

### Client credentials

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=oauth2-client-id" -d "client_secret=oauth2-client-secret" -d "grant_type=client_credentials"
```
   
```
{"token_type":"Bearer","access_token":"h8IKJfKhx47ZDf25akbTRKpHgV7QJFOFEpQcJqsv","expires_in":3600,"refresh_token":"g8OBI0IMXKleE99R9JxWg3JGanq7uPGzuDQ9Chnt"}
```

### Refresh token

```
$ curl http://localhost:9000/oauth/access_token -X POST -d "client_id=oauth2-client-id" -d "client_secret=oauth2-client-secret" -d "refresh_token=EuMLdSVUJqhMOhVlb6xeLlNmf4gfuDMMgDqr7P1l" -d "grant_type=refresh_token"
```

### Access resource using access_token

You can access application resource using access token

```
$ curl --dump-header - -H "Authorization: Bearer ${access_token}" http://localhost:9000/resources
```