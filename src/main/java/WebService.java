import geofunctions.ElasticGeofunctions;
import geofunctions.ElasticGeofunctions$;
import scala.Tuple2;
import scala.collection.JavaConverters.*;
import java.util.List;
import java.util.ArrayList;
import static spark.Spark.*;

/**
 * Created by fabiana on 08/09/17.
 */
public class WebService {
    public static void main(String[] args) {

        int elPort = 9900;
        String esIndex = "test_index";
        String esType = "test_type";
        ElasticGeofunctions es = new ElasticGeofunctions("localhost", elPort, esIndex, esType);

        //Spark.port(8080);

        // curl -d "lat=41.1262519&lng=16.8629509&distance=1000&checkSedeLegale=true" -X POST http://localhost:4567/getNeighborsGroupByAteco
        post("/getEntropy", (req,res)-> {
            double lat = Double.parseDouble(req.queryParams("lat"));
            double lng = Double.parseDouble(req.queryParams("lng"));
            int distance = Integer.parseInt(req.queryParams("distance"));
            boolean analyzeSediLegali = Boolean.parseBoolean(req.queryParams("checkSedeLegale"));

            double entropy = es.getEntropy(lat,lng, distance, analyzeSediLegali);
            res.type("application/json");

            return "{ \"entropy\": "+ entropy +"}";
        });

        post("/getDensity", (req,res)-> {
            double lat = Double.parseDouble(req.queryParams("lat"));
            double lng = Double.parseDouble(req.queryParams("lng"));
            int distance = Integer.parseInt(req.queryParams("distance"));
            boolean analyzeSediLegali = Boolean.parseBoolean(req.queryParams("checkSedeLegale"));

            double density = es.getDensity(lat,lng, distance, analyzeSediLegali);
            res.type("application/json");

            return "{ \"density\": "+ density +"}";
        });

        post("/getCompetiveness", (req,res)-> {
            double lat = Double.parseDouble(req.queryParams("lat"));
            double lng = Double.parseDouble(req.queryParams("lng"));
            int distance = Integer.parseInt(req.queryParams("distance"));
            boolean analyzeSediLegali = Boolean.parseBoolean(req.queryParams("checkSedeLegale"));
            String codiceAteco = req.queryParams("codiceAteco");

            double competiveness = es.getCompetiveness(lat,lng, distance, codiceAteco, analyzeSediLegali);
            res.type("application/json");

            return "{ \"competiveness\": "+ competiveness +"}";
        });

        /**
         * Returns all the organizations close to a given location
         */
        post("/getAllNeighbors", (req,res)-> {
            double lat = Double.parseDouble(req.queryParams("lat"));
            double lng = Double.parseDouble(req.queryParams("lng"));
            int distance = Integer.parseInt(req.queryParams("distance"));
            boolean analyzeSediLegali = Boolean.parseBoolean(req.queryParams("checkSedeLegale"));

            String result= es.getNeighborsWithPoints(lat,lng, distance, analyzeSediLegali);

            res.type("application/json");
            return result;
        });


        post("/getNeighborsByAteco", (req,res)-> {
            double lat = Double.parseDouble(req.queryParams("lat"));
            double lng = Double.parseDouble(req.queryParams("lng"));
            int distance = Integer.parseInt(req.queryParams("distance"));
            boolean analyzeSediLegali = Boolean.parseBoolean(req.queryParams("checkSedeLegale"));
            String codiceAteco = req.queryParams("codiceAteco");
            double density = es.getDensity(lat,lng, distance, analyzeSediLegali);
            res.type("application/json");

            return "{ \"density\": "+ density +"}";
        });




    }
}
