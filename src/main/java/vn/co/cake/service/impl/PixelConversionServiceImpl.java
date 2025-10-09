package vn.co.cake.service.impl;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.serverside.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PixelConversionServiceImpl {
    public static final String ACCESS_TOKEN = "<ACCESS_TOKEN>";
    public static final String PIXEL_ID = "<ADS_PIXEL_ID>";

    public void main(List<String> emails, List<String> phones) {
        APIContext context = new APIContext(ACCESS_TOKEN).enableDebug(true);
        context.setLogger(System.out);

        UserData userData = new UserData()
                .emails(emails)
                .phones(phones)
                // It is recommended to send Client IP and User Agent for Conversions API Events.
//                .clientIpAddress(clientIpAddress)
//                .clientUserAgent(clientUserAgent)
                .fbc("fb.1.1554763741205.AbCdEfGhIjKlMnOpQrStUvWxYz1234567890")
                .fbp("fb.1.1558571054389.1098115397");

        Content content = new Content()
                .productId("product123")
                .quantity(1L)
                .deliveryCategory(DeliveryCategory.home_delivery);

        CustomData customData = new CustomData()
                .addContent(content)
                .currency("VND")
                .value(500000F);

        Event purchaseEvent = new Event();
        purchaseEvent.eventName("Purchase")
                .eventTime(System.currentTimeMillis() / 1000L)
                .userData(userData)
                .customData(customData)
                .eventSourceUrl("http://jaspers-market.com/product/123")
                .actionSource(ActionSource.website);

        EventRequest eventRequest = new EventRequest(PIXEL_ID, context);
        eventRequest.addDataItem(purchaseEvent);

        try {
            EventResponse response = eventRequest.execute();
            log.info(String.format("Standard API response : %s ", response));
        } catch (APIException e) {
            e.printStackTrace();
        }
    }
}
