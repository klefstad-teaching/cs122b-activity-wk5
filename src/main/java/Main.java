import com.braintreepayments.http.serializer.Json;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.braintreepayments.http.HttpResponse;
import com.braintreepayments.http.exceptions.HttpException;
import com.paypal.orders.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Main {

    public static void main(String args[])
    {
        String orderid;
        PayPalOrderClient ppOrderclient = new PayPalOrderClient();
        System.out.println("---------- Creating order -----------");
        orderid = createPayPalOrder(ppOrderclient, true);

        // uncomment capturing part after you approve the payment
        // orderid = "8M024637NR407413J";
        try {
            // System.out.println("---------- Capturing order -----------");
            // captureOrder(orderid, ppOrderclient);
            System.out.println("---------- Getting order details-----------");
            getOrder(orderid, ppOrderclient);
        } catch (Exception  e) {
            e.printStackTrace();
        }

    }
    public static String createPayPalOrder(PayPalOrderClient client, boolean debug)
    {
        //Construct a request object and set desired parameters
        //Here orderscreaterequest creates a post request to v2/checkout/orders

        OrderRequest orderRequest = new OrderRequest();

        //MUST use this method instead of intent to create capture.
        orderRequest.checkoutPaymentIntent("CAPTURE");

        //Create application context with return url upon payer completion.
        ApplicationContext applicationContext = new ApplicationContext().returnUrl("https://www.paypal.com/us/home");


        orderRequest.applicationContext(applicationContext);

        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        purchaseUnits
                .add(new PurchaseUnitRequest().amountWithBreakdown(new AmountWithBreakdown().currencyCode("USD").value("1000.00")));
        orderRequest.purchaseUnits(purchaseUnits);
        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

        try {
            // Call API with your client and get a response for your call
            HttpResponse<Order> response = client.client.execute(request);

            // If call returns body in response, you can get the de-serialized version by
            // calling result() on the response
            if (debug) {
                if (response.statusCode() == 201) {
                    System.out.println("Status Code: " + response.statusCode());
                    System.out.println("Status: " + response.result().status());
                    System.out.println("Order ID: " + response.result().id());
                    response.result().links().forEach(link -> System.out.println(link.rel() + " => " + link.method() + ":" + link.href()));
                }
            }

            return response.result().id();
        } catch (IOException ioe) {
            System.err.println("*******COULD NOT CREATE ORDER*******");
            if (ioe instanceof HttpException) {
                // Something went wrong server-side
                HttpException he = (HttpException) ioe;
                System.out.println(he.getMessage());
                he.headers().forEach(x -> System.out.println(x + " :" + he.headers().header(x)));
            } else {


                // Something went wrong client-side
            }
            return null;
        }

    }

    public static void captureOrder(String orderID, PayPalOrderClient orderClient)
    {
        Order order = null;
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderID);

        try {

            // Call API with your client and get a response for your call
            HttpResponse<Order> response = orderClient.client.execute(request);

            // If call returns body in response, you can get the de-serialized version by
            // calling result() on the response
            order = response.result();


            System.out.println("Capture ID: " + order.purchaseUnits().get(0).payments().captures().get(0).id());

            order.purchaseUnits().get(0).payments().captures().get(0).links()
                    .forEach(link -> System.out.println(link.rel() + " => " + link.method() + ":" + link.href()));
        } catch (IOException ioe) {
            if (ioe instanceof HttpException) {
                // Something went wrong server-side

                HttpException he = (HttpException) ioe;
                System.out.println(he.getMessage());
                he.headers().forEach(x -> System.out.println(x + " :" + he.headers().header(x)));
            } else {
                // Something went wrong client-side
            }
        }

    }

    public static void getOrder(String orderId, PayPalOrderClient orderClient) throws IOException {

        OrdersGetRequest request = new OrdersGetRequest(orderId);
        HttpResponse<Order> response = orderClient.client.execute(request);
        System.out.println("Full response body:" + (new Json().serialize(response.result())));
    }
}

class PayPalOrderClient
{
    private final String clientId = "yourClientId";
    private final String clientSecret = "yourClientSecret";

    {
        System.out.println(new String(Base64.getEncoder().encode((clientId + ":" + clientSecret).getBytes())));
    }

    //setup paypal envrionment
    public PayPalEnvironment environment = new PayPalEnvironment.Sandbox(clientId, clientSecret);
    //Create client for environment
    public PayPalHttpClient client = new PayPalHttpClient(environment);
}