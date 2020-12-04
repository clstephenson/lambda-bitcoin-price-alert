package lambdabitcoinpricealert;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Handler value: lambdabitcoinpricealert.Handler
public class Handler implements RequestHandler<ScheduledEvent, Void> {

    static LambdaLogger logger;
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static boolean isDebug = false;
    static String url = "https://api.coinbase.com/v2/prices/spot?currency=USD";
    static String metricName = "Bitcoin Price";
    static boolean isLambdaEnvironment;

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        isDebug = System.getenv("DEBUG") == "1";
        isLambdaEnvironment = true;
        logger = context.getLogger();
        BasicConfigurator.configure();

        double bitcoinPrice;
        try {
            bitcoinPrice = getLastPriceForBitcoin();
            printLogMessage("[INFO]: Writing metric to CloudWatch (Bitcoin Price: )" + bitcoinPrice);
            writeMetric(metricName, bitcoinPrice);
        } catch (Exception e) {
            printLogMessage("[ERROR]: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        isLambdaEnvironment = false;
        double bitcoinPrice;
        try {
            bitcoinPrice = getLastPriceForBitcoin();
            printLogMessage(String.valueOf(bitcoinPrice));
        } catch (Exception e) {
            printLogMessage("[ERROR]: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    static double getLastPriceForBitcoin() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(
                URI.create(url))
                .build();

        printLogMessage("[INFO]: Sending request" + gson.toJson(request));
        HttpResponse<String> response = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            printLogMessage("[INFO]: Attempt #" + attempt);
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                printLogMessage("[INFO]: Request Sent");
                break;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    printLogMessage("[ERROR]: " + e.getMessage());
                    throw new RuntimeException("ERROR: There was an error sending the request!");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("[ERROR]: Could not get data from API. StatusCode: " + response.statusCode() + "\n");
        } else {
            printLogMessage("[INFO]: API response: " + response.statusCode());
            var data = response.body();
            BitcoinPrice btc = gson.fromJson(data, BitcoinPrice.class);
            return btc.getData().getAmount();
        }
    }

    static void writeMetric(String metricName, double metricValue) {
        var metricDatum = MetricDatum.builder()
                .metricName(metricName)
                .value(metricValue)
                .dimensions(Dimension.builder()
                        .name("Price")
                        .value("BitcoinLastPrice")
                        .build()
                ).build();

        var cloudWatchClient = CloudWatchClient.builder().build();
        cloudWatchClient.putMetricData(
                PutMetricDataRequest.builder()
                        .namespace("BitcoinPriceAlert")
                        .metricData(metricDatum)
                        .build()
        );

    }

    static void printLogMessage(String message) {
        if (isLambdaEnvironment) {
            logger.log(message + "\n");
        } else {
            System.out.println(message);
        }
    }
}