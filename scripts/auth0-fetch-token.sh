#!/bin/sh

APP_URL=localhost:8080
#CURL_OPTIONS="-v"
#VERBOSE_LOGGING=true

. auth0-config.sh

[ "$VERBOSE_LOGGING" = true ] && set -x
export AUTH0_RESPONSE=$(curl $CURL_OPTIONS --request POST \
  --url "https://$AUTH0_DOMAIN/oauth/token" \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type='http://auth0.com/oauth/grant-type/password-realm' \
  --data username="$AUTH0_EMAIL" \
  --data password="$AUTH0_PASSWORD" \
  --data audience="$AUTH0_AUDIENCE" \
  --data scope='openid profile email' \
  --data realm='Username-Password-Authentication' \
  --data client_id="$AUTH0_CLIENT_ID" \
  --data response_type='id_token token' \
  --data nonce='random_nonce')
set +x

AUTH0_ACCESS_TOKEN=$(echo $AUTH0_RESPONSE | jq -r .access_token)
AUTH0_ID_TOKEN=$(echo $AUTH0_RESPONSE | jq -r .id_token)

if [ "$VERBOSE_LOGGING" = true ]; then
	echo "\n----------------AUTH0_RESPONSE---------------------"
	echo $AUTH0_RESPONSE
	echo "\n----------------AUTH0_ACCESS_TOKEN---------------------"
	echo $AUTH0_ACCESS_TOKEN | cut -d "." -f2 | awk '{ l=length($0) % 4; if (l == 2) {print $0 "=="} else if (l == 3) {print $0 "="} else {print $0} }' | base64 -D | jq .
	echo "\n------------------AUTH0_ID_TOKEN-----------------------"
	echo $AUTH0_ID_TOKEN | cut -d "." -f2 | awk '{ l=length($0) % 4; if (l == 2) {print $0 "=="} else if (l == 3) {print $0 "="} else {print $0} }' | base64 -D | jq . 
	echo "\n-------------------------------------------------------\n"
fi

echo "\n------------------------/public/config.json-------------------------------\n"
[ "$VERBOSE_LOGGING" = true ] && set -x
curl $CURL_OPTIONS $APP_URL/public/config.json | jq .
set +x

GET_ENDPOINTS=("authorities" "thieves-treasure" "alibaba-treasure")

for endpoint in "${GET_ENDPOINTS[@]}";
do
	echo "\n------------------------/cave/$endpoint-------------------------------\n"
	[ "$VERBOSE_LOGGING" = true ] && set -x
	curl $CURL_OPTIONS -H "Authorization: Bearer $AUTH0_ACCESS_TOKEN" $APP_URL/api/cave/$endpoint | jq .
	set +x
done

echo "\n-------------------------/cave/take-treasure------------------------------\n"
[ "$VERBOSE_LOGGING" = true ] && set -x
curl $CURL_OPTIONS \
	-X POST \
	-H "Content-Type: application/json" \
	-H "Authorization: Bearer $AUTH0_ACCESS_TOKEN" \
	-d '{"owner":"ali-babas-treasure","amount":20}' \
	$APP_URL/api/cave/take-treasure | jq .
set +x


