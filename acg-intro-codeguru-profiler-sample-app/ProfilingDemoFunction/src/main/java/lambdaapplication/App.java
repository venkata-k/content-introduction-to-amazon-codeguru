package com.amazon.codeguru.lambdaprofiler.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.codeguruprofilerjavaagent.LambdaProfiler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<Object, Object> {

    private static final Logger LOG = Logger.getLogger(App.class.getName());
    private final static String TABLE_NAME = System.getenv("TABLE_NAME");
    
    @Override
    public Object handleRequest(final Object input, final Context context) {
        return LambdaProfiler.profile(input, context, this::myHandlerFunction);
    }

    public Object myHandlerFunction(final Object input, final Context context) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        try {
            final String pageContents = this.getPageContents("https://checkip.amazonaws.com");
            this.storeData(pageContents);
            final String output = String.format("{ \"message\": \"hello world\", \"location\": \"%s\" }", pageContents);
            return new GatewayResponse(output, headers, 200);
        } catch (final Exception e) {
            return new GatewayResponse("{}", headers, 500);
        }
    }

    private String getPageContents(final String address) throws IOException {
        final URL url = new URL(address);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private void static storeData(final String ipv4) throws AmazonServiceException {
        final AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
        final String now = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        final String randomNumbersGenerated = String.valueOf(this.generateRandomNumbers());

        final HashMap<String, AttributeValue> keyValuePair = new HashMap<String,AttributeValue>();
        keyValuePair.put("location", new AttributeValue(ipv4));
        keyValuePair.put("date", new AttributeValue(now));
        keyValuePair.put("randomNumberCount", new AttributeValue(randomNumbersGenerated));
        ddb.putItem(TABLE_NAME, keyValuePair);
    }

    private int generateRandomNumbers() {
        int count = 0;
        // Generate random numbers for 5 seconds
        Instant endTime = Instant.now().plusMillis(5000);

        while (Instant.now().isBefore(endTime)) {
            final Random random = new Random();
            random.nextInt();
            count++;
        }
        return count;
    }
}
