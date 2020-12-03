package lambdabitcoinpricealert;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.blockchain.exchange.rest.api.UnauthenticatedApi;
import com.blockchain.exchange.rest.model.PriceEvent;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

// Handler value: lambdabitcoinpricealert.Handler
public class Handler implements RequestHandler<ScheduledEvent, Void> {

    @Override
    public Void handleRequest(ScheduledEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        UnauthenticatedApi apiInstance = new UnauthenticatedApi();
        String symbol = "BTC-USD";
        double bitcoinPrice;
        try {
            bitcoinPrice = getLastPriceForSymbol("BTC-USD");
            writeMetric("Bitcoin Price", bitcoinPrice);
        } catch (Exception e) {
            logger.log("[ERROR]: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        UnauthenticatedApi apiInstance = new UnauthenticatedApi();
        String symbol = "BTC-USD";
        double bitcoinPrice;
        try {
            bitcoinPrice = getLastPriceForSymbol("BTC-USD");
            System.out.println(bitcoinPrice);
        } catch (Exception e) {
            System.err.println("[ERROR]: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    static double getLastPriceForSymbol(String symbol) throws Exception {
        try {
            //System.out.println("[DEBUG]: in getLastPriceForSymbol");
            UnauthenticatedApi apiInstance = new UnauthenticatedApi();
            //System.out.println("[DEBUG]: apiInstance created");
            PriceEvent result = apiInstance.getTickerBySymbol(symbol);
            //System.out.println("[DEBUG]: ticker retrieved");
            return result.getLastTradePrice();
        } catch (Exception e) {
            throw new Exception("Exception when calling UnauthenticatedApi#getTickerBySymbol", e);
        }
    }

    static void writeMetric(String metricName, double metricValue) {
        var metricDatum = MetricDatum.builder()
                .metricName(metricName)
                .value(metricValue)
                .unit("$")
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
}