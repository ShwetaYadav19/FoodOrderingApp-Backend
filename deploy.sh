echo "******* Building *******"
mvn clean install

cd FoodOrderingApp-db
echo "******* Setting up database *******"


cd ../FoodOrderingApp-api
echo " *******Starting server "
mvn spring-boot:run