################################################################################
################# ~ Updates local container on machine ~ #######################
################################################################################

echo '[API] Updating local container'

CONTAINER_ID=$(echo $(docker ps -aqf name="api") | sed 's/\=//')

echo '[API] Destroying and updating... (ID: $CONTAINER_ID)'

docker stop $CONTAINER_ID
docker rm $CONTAINER_ID
docker build . -t registry.floofy.dev/auguwu/api:latest
docker run --name api -d -p 3621:3621 registry.floofy.dev/auguwu/api:latest
